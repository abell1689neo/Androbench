package kr.ac.snu.csl.androbench.bench

import android.content.Context
import android.util.Log

object FioTest{
    private const val TAG="FioTest"
    fun version (context: Context){
        try{
            val fioPath="${context.applicationInfo.nativeLibraryDir}/libfio.so"
            Log.i(TAG, "version test")
            Log.i(TAG, "fio path: $fioPath")

            val process=ProcessBuilder(fioPath, "--version")
                .redirectErrorStream(true)
                .start()
            val output=process.inputStream.bufferedReader().readText()//read child process output
            val exit=process.waitFor() //reap child

            Log.i(TAG, "exit=$exit")
            Log.i(TAG, "output: $output")
        }catch(e: Exception){
            Log.e(TAG, "version test failed", e)
        }
    }

    fun measure(context: Context){
        try{
            val fioPath="${context.applicationInfo.nativeLibraryDir}/libfio.so"
            val testFile="${context.filesDir}/fiotest"
            Log.i(TAG, "===== measure test =====")
            Log.i(TAG, "test file: $testFile")

            val process= ProcessBuilder(
                fioPath,
                "--name=test",
                "--rw=randread",
                "--bs=4k",
                "--size=64m",
                "--filename=$testFile",
                "--direct=1",                  // O_DIRECT (page cache 우회)
                "--runtime=5",
                "--time_based",
                "--ioengine=psync",            // sync IO engine
                "--output-format=json",
            )
                .redirectErrorStream(true)
                .start()

            val output=process.inputStream.bufferedReader().readText()
            val exit=process.waitFor()
            Log.i(TAG, "exit=$exit")
            // JSON 다 찍으면 logcat 폭주 → 처음 500자만
            Log.i(TAG, "output (first 500 chars):\n${output.take(500)}")

        }catch(e: Exception){
            Log.e(TAG,"measure test failed",e )
        }

    }
}