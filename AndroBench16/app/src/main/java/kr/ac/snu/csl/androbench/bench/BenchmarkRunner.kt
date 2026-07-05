// Calls NativeIO + SqliteBench sequentially with progress callbacks
package kr.ac.snu.csl.androbench.bench

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Progress callback signature:
 *   onProgress(phaseName, phaseIndex, totalPhases, iteration, totalIterations)
 * iteration / totalIterations = 0 means "single-shot phase" (no iter loop)
 */
typealias ProgressCallback = (
    phaseName: String,
    phaseIndex: Int,
    totalPhases: Int,
    iteration: Int,
    totalIterations: Int,
) -> Unit

class BenchmarkRunner(private val context: Context) {

    suspend fun run(
        config: BenchmarkConfig,
        onProgress: ProgressCallback = { _, _, _, _, _ -> },
    ): BenchmarkResult = withContext(Dispatchers.IO) {

        //1. fio mode appended
        if(config.mode in listOf(
                BenchmarkMode.FIO_SEQ_READ,
                BenchmarkMode.FIO_SEQ_WRITE,
                BenchmarkMode.FIO_RND_READ,
                BenchmarkMode.FIO_RND_WRITE,
        )){
            return@withContext runFio(config, onProgress)
        }

        //2. default mode
        Log.i("BenchmarkRunner",
            "RUN mode=${config.mode} path=${config.targetPath} " +
            "fileSizeKb=${config.fileSizeKb} seqReclenKb=${config.seqReclenKb} " +
            "rndReclenKb=${config.rndReclenKb} numThreads=${config.numThreads} " +
            "rndMaxRecs=${config.rndMaxRecs} sqliteOperations=${config.sqliteOperations} " +
            "numIterations=${config.numIterations}"
        )

        val runSeq    = config.mode == BenchmarkMode.ALL || config.mode == BenchmarkMode.SEQUENTIAL
        val runRnd    = config.mode == BenchmarkMode.ALL || config.mode == BenchmarkMode.RANDOM
        val runSqlite = config.mode == BenchmarkMode.ALL || config.mode == BenchmarkMode.SQLITE

        // Build phase plan for accurate progress reporting
        val phasePlan = buildList<String> {
            if (runSeq || runRnd) add("Initializing files")
            if (runSeq) { add("Sequential Read"); add("Sequential Write") }
            if (runRnd) { add("Random Read"); add("Random Write") }
            if (runSqlite) add("SQLite")
        }
        val totalPhases = phasePlan.size
        var phaseIndex = 0

        fun startPhase(name: String, iter: Int = 0, totalIter: Int = 0) {
            phaseIndex++
            onProgress(name, phaseIndex, totalPhases, iter, totalIter)
        }

        fun updateIter(name: String, iter: Int, totalIter: Int) {
            onProgress(name, phaseIndex, totalPhases, iter, totalIter)
        }

        val needFiles = runSeq || runRnd
        if (needFiles) {
            startPhase("Initializing files")
            val initRc = NativeIO.initFiles(
                path = config.targetPath,
                fileSizeKb = config.fileSizeKb,
                numThread = config.numThreads,
            )
            if (initRc < 0) error("initFiles failed: $initRc")
        }

        try {
            val (seqReadMBps, seqReadIops) = if (runSeq) {
                startPhase("Sequential Read", 1, config.numIterations)
                val avgNs = (1..config.numIterations).map { iter ->
                    updateIter("Sequential Read", iter, config.numIterations)
                    val ns = NativeIO.seqRead(config.targetPath, config.seqReclenKb,
                        config.fileSizeKb, config.numThreads)
                    if (ns < 0) error("seqRead failed: $ns")
                    ns
                }.average()
                computeSeqMBps(avgNs, config) to computeSeqIops(avgNs, config)
            } else 0.0 to 0.0

            val (seqWriteMBps, seqWriteIops) = if (runSeq) {
                startPhase("Sequential Write", 1, config.numIterations)
                val avgNs = (1..config.numIterations).map { iter ->
                    updateIter("Sequential Write", iter, config.numIterations)
                    val ns = NativeIO.seqWrite(config.targetPath, config.seqReclenKb,
                        config.fileSizeKb, config.numThreads)
                    if (ns < 0) error("seqWrite failed: $ns")
                    ns
                }.average()
                computeSeqMBps(avgNs, config) to computeSeqIops(avgNs, config)
            } else 0.0 to 0.0

            val (rndReadMBps, rndReadIops,rndReadLatency) = if (runRnd) {
                startPhase("Random Read", 1, config.numIterations)
                runRandomMany(isWrite = false, config = config) { iter ->
                    updateIter("Random Read", iter, config.numIterations)
                }
            } else Triple(0.0,0.0, null )

            val (rndWriteMBps, rndWriteIops, rndWriteLatency) = if (runRnd) {
                startPhase("Random Write", 1, config.numIterations)
                runRandomMany(isWrite = true, config = config) { iter ->
                    updateIter("Random Write", iter, config.numIterations)
                }
            } else Triple(0.0,0.0, null )

            val sqlite = if (runSqlite) {
                startPhase("SQLite")
                SqliteBench(context).run(config.sqliteOperations)
            } else null

            BenchmarkResult(
                seqReadMBps = seqReadMBps,
                seqReadIops = seqReadIops,
                seqWriteMBps = seqWriteMBps,
                seqWriteIops = seqWriteIops,
                rndReadMBps = rndReadMBps,
                rndReadIops = rndReadIops,
                rndWriteMBps = rndWriteMBps,
                rndWriteIops = rndWriteIops,
                sqliteInsertTps = sqlite?.insertTps() ?: 0.0,
                sqliteUpdateTps = sqlite?.updateTps() ?: 0.0,
                sqliteDeleteTps = sqlite?.deleteTps() ?: 0.0,
                mode = config.mode,
                rndReadLatency=rndReadLatency,
                rndWriteLatency = rndWriteLatency,
            )
        } finally {
            if (needFiles) {
                NativeIO.finalizeFiles(config.targetPath, config.numThreads)
            }
        }
    }

    private fun computeSeqMBps(avgNs: Double, config: BenchmarkConfig): Double {
        //val totalBytes = config.numThreads.toLong() * config.fileSizeKb * 1024L
        val totalBytes = config.fileSizeKb.toLong() * 1024L
        val seconds = avgNs / 1e9
        return (totalBytes / (1024.0 * 1024.0)) / seconds
    }
    private fun computeSeqIops(avgNs: Double, config: BenchmarkConfig,): Double{
        //val totalOps=config.numThreads.toLong()*(config.fileSizeKb/config.seqReclenKb)
        val totalOps=(config.fileSizeKb/config.seqReclenKb).toLong()
        val seconds=avgNs/1e9
        return totalOps/seconds
    }
    private fun runRandomMany(
        isWrite: Boolean,
        config: BenchmarkConfig,
        onIter: (iter: Int) -> Unit = {},
    ): Triple<Double, Double, LatencyStats?> {
        val allLatencies=mutableListOf<Long>()//all latency list
        val stats = (1..config.numIterations).map { iter ->
            onIter(iter)
            val seed = (Math.random() * 10000).toInt()
            val ns = if (isWrite) {
                NativeIO.rndWrite(
                    config.targetPath, config.rndReclenKb, config.fileSizeKb,
                    config.rndMaxRecs, seed, config.numThreads,
                )
            } else {
                NativeIO.rndRead(
                    config.targetPath, config.rndReclenKb, config.fileSizeKb,
                    config.rndMaxRecs, seed, config.numThreads,
                )
            }
            if (ns < 0) error("rnd ${if (isWrite) "Write" else "Read"} failed: $ns")
            val recs = NativeIO.getLastRndRecCount()
            val iterLatencies= NativeIO.getLastLatencies()//pass latency array
            for(lat in iterLatencies) allLatencies.add(lat)
            ns to recs
        }
        val mbpsAvg = stats.map { (ns, recs) ->
            (recs * config.rndReclenKb / 1024.0) / (ns / 1e9)
        }.average()
        val iopsAvg = stats.map { (ns, recs) ->
            recs / (ns / 1e9)
        }.average()

        val latency= if (allLatencies.isNotEmpty()){
            LatencyCompute.compute(allLatencies.toLongArray())
        }else null
        return Triple(mbpsAvg, iopsAvg, latency)
    }
    private suspend fun runFio(
        config: BenchmarkConfig,
        onProgress: ProgressCallback,
    ): BenchmarkResult= coroutineScope{
        onProgress("fio: ${config.mode.name}", 1, 1,0,0)

        //fio 인자 조립
        val isWrite=config.mode in listOf(BenchmarkMode.FIO_SEQ_WRITE, BenchmarkMode.FIO_RND_WRITE)
        val isRandom=config.mode in listOf(BenchmarkMode.FIO_RND_READ, BenchmarkMode.FIO_RND_WRITE)
        val rwArg= when{//fio 변수 인자 결정
            isRandom&&isWrite->"randwrite"
            isRandom&&!isWrite->"randread"
            !isRandom&&isWrite-> "write"
            else->"read"
        }
        val blockSize=if(isRandom) config.rndReclenKb else config.seqReclenKb
        val numjobs=if(isRandom) config.numThreads else 1 //seq일때는 thread 1개로 진행


        //얘네 인자 검증 필요
        val args=mutableListOf(
            "--name=androbench",
            "--rw=$rwArg",
            "--bs=${blockSize}k",
            "--size=${config.fileSizeKb}k",
            "--direct=1",//o_direct
            //"--fdatasync=1",
            //"--end_fsync=1",
            "--ioengine=psync",//why psync?
            "--thread",
            "--numjobs=$numjobs",
            "--loops=${config.numIterations}",
            "--group_reporting",
            "--randrepeat=0", //random seed renewal
            "--unlink=1" //file cleanup
            //"--sync=1",//o_sync
            //"--runtime=${config.fioRuntime}",
            //"--time_based",
            //"--ramp_time=2",//eliminate warmup effect
        )
        //per-loop fdatasync
        val opsPerLoop=if(isRandom){
            config.rndMaxRecs
        }else{
            config.fileSizeKb/config.seqReclenKb
        }
        args.add("--fdatasync=$opsPerLoop")
        
        if(isRandom){
            //per thread file
            args.add("--directory=${config.targetPath}")
            args.add("--filename_format=fio_androbench_\$jobnum")
            args.add("--nrfiles=1") //per job files
            args.add("--openfiles=1")
            args.add("--number_ios=${config.rndMaxRecs}") //maxrec만큼 순회
        }else{
            //sequential: 1 file
            val testFile="${config.targetPath}/fio_androbench"
            args.add("--filename=$testFile")
        }
        // count-based이므로 총 시간 미정 → elapsed sec만 표시 (totalIter=0 → indeterminate)
        val progressJob=launch{
            var sec=0
            while(true){
                onProgress(
                    "fio: ${config.mode.name.removePrefix("FIO_")}",
                    1,1,
                    sec, 0,
                )
                delay(1000)
                sec+=1
            }
        }
        try {
            val fioResult = FioRunner(context).run(args)
            BenchmarkResult(
                seqReadMBps = 0.0,
                seqWriteMBps = 0.0,
                rndReadMBps = 0.0,
                rndReadIops = 0.0,
                rndWriteMBps = 0.0,
                rndWriteIops = 0.0,
                sqliteInsertTps = 0.0,
                sqliteUpdateTps = 0.0,
                sqliteDeleteTps = 0.0,
                mode = config.mode,
                fioReadMBps = fioResult.readMBps,
                fioReadIops = fioResult.readIops,
                fioWriteMBps = fioResult.writeMBps,
                fioWriteIops = fioResult.writeIops,
                fioLatency = fioResult.latency,
            )
        }finally{
            progressJob.cancel()
        }
    }
}
