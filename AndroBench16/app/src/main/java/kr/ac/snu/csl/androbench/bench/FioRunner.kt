package kr.ac.snu.csl.androbench.bench

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

enum class FioLogProfile{
    LEGACY,
    SUST_SEQ,
    SUST_RND,
}
class FioRunner(private val context: Context){
    private val fioPath: String
        get()="${context.applicationInfo.nativeLibraryDir}/libfio.so"

    suspend fun run(args: List<String>,
                    profile: FioLogProfile= FioLogProfile.LEGACY, //sustained -> bw log
                    bsKbForDerivedBw: Int=4,
    ): FioResult=withContext(Dispatchers.IO){
        //latency prefix for every fio
        val logDir=context.cacheDir.absolutePath
        val stamp = System.nanoTime()
        val latPrefix="$logDir/fio_lat_$stamp"
        val bwPrefix="$logDir/fio_bw_$stamp"

        val finalArgs=args.toMutableList().apply {
            add("--per_job_logs=0")
            when (profile){
                FioLogProfile.LEGACY, FioLogProfile.SUST_RND->{
                    add("--write_lat_log=$latPrefix")
                    add("--log_avg_msec=0")
                }

                FioLogProfile.SUST_SEQ->{
                    add("--write_bw_log=$bwPrefix")
                    add("--log_avg_msec=1000")
                }
            }
        }
         //lambda block
        val cmd= listOf(fioPath, "--output-format=json")+finalArgs
        Log.i(TAG, "exec: ${cmd.joinToString(" ")}")

        val process= ProcessBuilder(cmd).redirectErrorStream(true).start() //start fio process
        val output=process.inputStream.bufferedReader().readText()
        val exitCode=process.waitFor() //wait for child to terminate

        if(exitCode!=0){
            Log.e(TAG, "fio failed exit=$exitCode\n$output")

            throw RuntimeException("fio failed: exit=$exitCode")

        }
        val base=parseJson(output)
        var latencyStats: LatencyStats? =null
        var bwSamples:List<BwSample> = emptyList()
        when (profile){
            FioLogProfile.LEGACY->{ //legacy: latency만
                findLog(latPrefix, "_clat")?.let{clat->
                    val raw=parseLatLog(clat)
                    if(raw.isNotEmpty()) latencyStats=LatencyCompute.compute(raw)}
                cleanupLatLogs(latPrefix)
            }
            FioLogProfile.SUST_RND -> {
                findLog(latPrefix, "_clat")?.let{clat->
                    val raw=parseLatLog(clat)
                    if(raw.isNotEmpty()) latencyStats= LatencyCompute.compute(raw)
                    bwSamples=deriveBwFromLatLog(clat, bsKbForDerivedBw)
                }
                cleanupLatLogs(latPrefix)
            }
            FioLogProfile.SUST_SEQ -> {
                findLog(bwPrefix, "_bw")?.let { bwSamples = parseBwLog(it) }
                File("${bwPrefix}_bw.log").delete()
                File("${bwPrefix}_bw.1.log").delete()
            }
        }
        base.copy(latency = latencyStats, bwSamples = bwSamples)
        //compute latency stat from lat log
//        val clatLog=File("${logPrefix}_clat.log") //read clat file
//        val latencyStats= if(clatLog.exists()){
//            val rawLatencies=parseLatLog(clatLog)
//            if(rawLatencies.isNotEmpty()) LatencyCompute.compute(rawLatencies) else null
//        }else{
//            Log.w(TAG, "clat log not found: ${clatLog.absolutePath}")
//            null
//        }
//        cleanupLatLogs(logPrefix)
//        base.copy(latency=latencyStats)//latency field update
     }
    private fun findLog(prefix: String, kind: String):File?=
        listOf("$prefix$kind.log", "$prefix$kind.1.log")
            .map{File(it)}.firstOrNull{it.exists()}


    private fun parseLatLog(file: File): LongArray{
        val result=ArrayList<Long>(65536)
        file.bufferedReader().use { reader->
            reader.forEachLine { line->//per line(no entire memory loaded)
                //latency in the second column
                val firstComma=line.indexOf(',')
                if(firstComma<0) return@forEachLine
                val secondComma=line.indexOf(',', firstComma+1)
                val latStr=if(secondComma>0) {
                    line.substring(firstComma + 1, secondComma).trim()
                }else{
                    line.substring(firstComma+1).trim()
                }
                latStr.toLongOrNull()?.let{result.add(it)}
            }
        }
        return result.toLongArray()
    }

    //1초 bucket 합산 (numjobs>1 대비 — nj=1이면 항등)
    private fun parseBwLog(file: File): List<BwSample> {
        val buckets = sortedMapOf<Long, Double>()
        file.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val p = line.split(",")
                if (p.size >= 2) {
                    val t = (p[0].trim().toLongOrNull() ?: return@forEach) / 1000 * 1000
                    val bw = p[1].trim().toDoubleOrNull() ?: return@forEach
                    buckets[t] = (buckets[t] ?: 0.0) + bw
                }
            }
        }
        return buckets.map { BwSample(it.key, it.value) }
    }
    private fun deriveBwFromLatLog(file: File, bsKb: Int): List<BwSample> {
        val counts = sortedMapOf<Long, Long>()
        file.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val c = line.indexOf(',')
                if (c < 0) return@forEach
                val t = line.substring(0, c).trim().toLongOrNull() ?: return@forEach
                val b = t / 1000 * 1000
                counts[b] = (counts[b] ?: 0L) + 1L
            }
        }
        //마지막 부분 버킷(1초 미만)은 BW가 낮게 왜곡되므로 제외
        val list = counts.map { (t, n) -> BwSample(t, n.toDouble() * bsKb) }
        return if (list.size > 2) list.dropLast(1) else list
    }
    private fun cleanupLatLogs(prefix: String) {
        listOf("_clat", "_slat", "_lat").forEach { k ->
            File("$prefix$k.log").delete()
            File("$prefix$k.1.log").delete()
        }
    }
    private fun parseJson(text: String): FioResult{
        val jsonStart=text.indexOf('{')
        require(jsonStart>=0) {"No JSON found"}
        val cleanText=text.substring(jsonStart)
        val json= JSONObject(cleanText) //json을 object tree parsing
        val job=json.getJSONArray("jobs").getJSONObject(0) //job 1개, 첫 원소 추출
        val read=job.getJSONObject("read")
        val write=job.getJSONObject("write")
        return FioResult(//constrcut
            readMBps = read.getDouble("bw")/1024.0,
            readIops = read.getDouble("iops"),
            writeMBps = write.getDouble("bw")/1024.0,
            writeIops = write.getDouble("iops"),
            elapsedSec = job.getInt("elapsed"),
        )
    }

    companion object{
        private const val TAG="FioRunner"
    }
}
data class FioResult(
    val readMBps: Double,
    val readIops:Double,
    val writeMBps: Double,
    val writeIops: Double,
    val elapsedSec: Int,
    val latency: LatencyStats?=null,
    val bwSamples: List<BwSample> = emptyList(),
)
