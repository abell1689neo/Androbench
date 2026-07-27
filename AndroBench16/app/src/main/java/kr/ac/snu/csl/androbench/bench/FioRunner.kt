package kr.ac.snu.csl.androbench.bench

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import kotlin.math.log

class FioRunner(private val context: Context){
    private val fioPath: String
        get()="${context.applicationInfo.nativeLibraryDir}/libfio.so"

    suspend fun run(args: List<String>,
                    collectMmapCounters: Boolean=false,
                    ): FioResult=withContext(Dispatchers.IO){
        //latency prefix for every fio
        val logDir=context.cacheDir.absolutePath
        val logPrefix="$logDir/fio_lat_${System.nanoTime()}"
        val finalArgs=args+listOf(
            "--write_lat_log=$logPrefix", //log 기록 인자
            "--per_job_logs=0",//모든 job합쳐서 file 1개
            "--log_avg_msec=0", //매 op마다 기록 (평균 x)
        )
         //lambda block
        val cmd= listOf(fioPath, "--output-format=json")+finalArgs
        Log.i(TAG, "exec: ${cmd.joinToString(" ")}")

        //remove prev snpashot
        File(logDir, "mmap.prerun.txt").delete()
        File(logDir, "mmap.postrun.txt").delete()//?

        //log prerun.txt
        val process= ProcessBuilder(cmd)
            .directory(context.cacheDir) //log exec_prerun/postrun stdout to the path
            .redirectErrorStream(true).start() //start fio process
        val output=process.inputStream.bufferedReader().readText()
        val exitCode=process.waitFor() //wait for child to terminate

        if(exitCode!=0){
            Log.e(TAG, "fio failed exit=$exitCode\n$output")

            throw RuntimeException("fio failed: exit=$exitCode")

        }
        //process results
        var base=parseJson(output)

        //1. compute latency stat from lat log
        val clatLog=File("${logPrefix}_clat.log") //read clat file
        val latencyStats= if(clatLog.exists()){
            val rawLatencies=parseLatLog(clatLog)
            if(rawLatencies.isNotEmpty()) LatencyCompute.compute(rawLatencies) else null
        }else{
            Log.w(TAG, "clat log not found: ${clatLog.absolutePath}")
            null
        }
        cleanupLatLogs(logPrefix)

        //2. mmap kernel counter
        if(collectMmapCounters){
            //parse read bytes in the log
            val pre=parseProcIo(File(logDir, "mmap.prerun.txt"))
            val post=parseProcIo(File(logDir, "mmap.postrun.txt"))

            if(pre!=null && post!= null){
                base=base.copy(
                    kernelReadBytes=(post["read_bytes"]?:0) - (pre["read_bytes"]?:0),
                    kernelWriteBytes = (post["write_bytes"]?:0)-(pre["write_bytes"]?:0),

                )
            } else Log.w(TAG, "mmap prerun/postrun snapshot missing")
        }
        base.copy(latency=latencyStats)//latency field update
     }
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
    private fun cleanupLatLogs(prefix: String){
        listOf("${prefix}_clat.log", "${prefix}_slat.log", "${prefix}_lat.log")
            .forEach { File(it).delete() }
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
            //mmap fields in json
            logicalIoBytes = read.getLong("io_bytes")+write.getLong("io_bytes"), // read일때 write 0
            majFaults = job.optInt("majf", 0),
            minFaults = job.optInt("minf", 0)

        )
    }
    private fun parseProcIo(file:File): Map<String, Long>?{
        if (!file.exists()) return null
        return file.readLines()
            .mapNotNull { line->
                val idx=line.indexOf(':')
                if(idx<0) null
                else line.substring(0, idx).trim() to
                        (line.substring(idx+1).trim().toLongOrNull()?: return@mapNotNull null)
            }.toMap()
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
    //mmap stats
    val logicalIoBytes: Long=0,
    val majFaults: Int=0,
    val minFaults: Int=0,
    val kernelReadBytes: Long=-1,
    val kernelWriteBytes: Long=-1,
)
