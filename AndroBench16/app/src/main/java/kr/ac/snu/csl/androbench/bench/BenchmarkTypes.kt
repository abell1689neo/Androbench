package kr.ac.snu.csl.androbench.bench

//which bench to run?
enum class BenchmarkMode{
    ALL,
    SEQUENTIAL,
    RANDOM,
    SQLITE,
    FIO_SEQ_READ,
    FIO_SEQ_WRITE,
    FIO_RND_READ,
    FIO_RND_WRITE,

    //mmap engine by fio
    FIO_MMAP_SEQ_READ,
    FIO_MMAP_RND_READ,
    FIO_MMAP_SEQ_WRITE,
    FIO_MMAP_RND_WRITE,
}

//defines the configuration for running the benchmark
data class BenchmarkConfig(
    val targetPath: String, //directory to make test file
    val seqReclenKb: Int=1024, //Sequential R/W record size — 1MB (장치 최대 BW 측정 관례, Seq1M)
    val rndReclenKb: Int=4,  //Random R/W record size — UFS page size
    val fileSizeKb: Int=2097152,  //2 GB — Pixel 9 어디서나 안전 (NativeIO 4 threads × 2 GB = 8 GB)
    val numThreads: Int=8,         //8 — Random 병렬 job (교수님 지정: nj8·QD32). 디스크 8×2GB=16GB 주의
    val rndMaxRecs: Int=16384,     //working set = 4 × 16384 × 4 KB = 256 MB ≥ SRAM 64 MB
    val numIterations: Int=3,
    val sqliteOperations: Int=1024,
    val mode: BenchmarkMode = BenchmarkMode.ALL,
    val mmapReadaheadOn: Boolean=false, //whether to readahead

    //libaio setting
    val fioEngine: String = "libaio",
    val seqIoDepth: Int = 16, //Awake 스윕 실측: QD4 2590 → QD16 3050 MB/s (2026-08-26, 57231 유닛). QD32는 변동 커서 16 채택
    val rndIoDepth: Int = 32,

    //campaign
    val campaignMode: Boolean =false,
    val campaignRuns: Int =10, 
    val campaignTermSec: Int = 60,
    val cpuPinning: Boolean=true,
)
//latency state
data class LatencyStats(
    val count: Int, //total operations measured
    val meanNs: Long,
    val minNs: Long,
    val maxNs: Long,
    val p50Ns: Long,
    val p90Ns: Long,
    val p99Ns: Long,
    val p99_9Ns: Long,
    val histogram: List<HistogramBin> = emptyList(),
    val percentiles: List<PercentilePoint> = emptyList(),
)
data class HistogramBin(
    val LowNs: Long, //bin의 하한
    val highNs: Long, //upper bound, exclusive
    val count: Int,
    val label: String,
)
data class PercentilePoint(//point 1개
    val percentile: Double,
    val latencyNs: Long,
)
//saves the result of 1 execution of benchmark
data class BenchmarkResult(
    val seqReadMBps: Double,
    val seqReadIops: Double=0.0,
    val seqWriteMBps: Double,
    val seqWriteIops: Double=0.0,

    val rndReadMBps: Double,    //throughput
    val rndReadIops: Double,    //operation per sec
    val rndWriteMBps: Double,
    val rndWriteIops: Double,

    val sqliteInsertTps: Double,
    val sqliteUpdateTps: Double,
    val sqliteDeleteTps: Double,
    val mode: BenchmarkMode = BenchmarkMode.ALL,
    //fio results
    val fioReadMBps: Double=0.0,
    val fioReadIops: Double=0.0,
    val fioWriteMBps: Double = 0.0,
    val fioWriteIops: Double = 0.0,

    //mmap stats
    val mmapStats: MmapStats?= null,
    //latency stats
    val rndReadLatency: LatencyStats?=null,
    val rndWriteLatency: LatencyStats?=null,
    val fioLatency: LatencyStats?=null,
    //campaign mode only
    val campaign: CampaignSummary?=null,
    )

//saves the progress of benchmark
sealed class BenchmarkState {
    object Idle: BenchmarkState()

    data class Running(
        val phase: String,           // "Sequential Read" 등
        val phaseIndex: Int,         // 1-indexed: 현재 단계 번호
        val totalPhases: Int,        // 총 단계 수 (mode에 따라 1~5)
        val iteration: Int,          // 1-indexed iter (0이면 표시 안 함)
        val totalIterations: Int,    // 총 iter (0이면 단일 phase, e.g. SQLite)
    ): BenchmarkState() {
        //전체 진행률 0.0 ~ 1.0
        val overallProgress: Float
            get() {
                if (totalPhases <= 0) return 0f
                val perPhase = 1f / totalPhases
                val withinPhase = if (totalIterations > 0)
                    (iteration - 1).coerceAtLeast(0).toFloat() / totalIterations
                else 0f
                return ((phaseIndex - 1).coerceAtLeast(0) * perPhase + withinPhase * perPhase)
                    .coerceIn(0f, 1f)
            }
    }

    data class Done(val result: BenchmarkResult): BenchmarkState()
    data class Error(val message: String): BenchmarkState()
}

data class MmapStats(
    val logicalBytes: Long, //app requested bytes
    val kernelReadBytes: Long, //actual bytes read from the storage
    val kernelWriteBytes: Long=0, //end_fsync -> flush
    val majFaults: Int, //storage IO needed
    val minFaults: Int, //mapping
    val pageSizeBytes: Int,
    val readaheadOn: Boolean=false, //측정 당시 readahead 축 (OFF=순수 페이지 단위)
){
    //Read amplification factor
    val ra: Double get()=if(logicalBytes>0) kernelReadBytes.toDouble()/logicalBytes else 0.0
    //fault 1회당 kernel read bytes
    val bytesPerFault: Double get()=if (majFaults>0) kernelReadBytes.toDouble()/majFaults else 0.0
    val waf: Double get()=if(logicalBytes>0) kernelWriteBytes.toDouble()/logicalBytes else 0.0

}
//campaign 요약 — 대표값은 mean (GC로 꺼진 런도 "사용자가 겪는 성능"의 일부라 버리지 않음)
data class CampaignSummary(
    val runs: Int, //회차 
    val termSec: Int,
    val primaryLabel: String,   //"MB/s"(seq) or "IOPS"(rnd)
    val mean: Double,
    val cvPercent: Double,      //표본표준편차/mean×100 — 저쪽 팀의 "CV 0.5%"와 같은 정의
    val min: Double,
    val max: Double,
    val perRun: List<Double>,   //런별 원값 (그래프/이상치 확인용)
)