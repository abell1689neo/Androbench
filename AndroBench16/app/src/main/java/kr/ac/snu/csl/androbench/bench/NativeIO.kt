//C 함수들을 kotlin에서 부를 수 있게 함
package kr.ac.snu.csl.androbench.bench

object NativeIO{

    init{
        System.loadLibrary("androbench_jni")
    }

    //ERR codes
    const val ERR_OPEN=-1L;
    const val ERR_READ=-2L;
    const val ERR_WRITE=-3L;
    const val ERR_ALLOC=-4L;


    //create files
    external fun initFiles(
        path: String,
        fileSizeKb: Int,
        numThread: Int
    ):Int

    //delete files
    external fun finalizeFiles(
        path: String,
        numThread: Int
    ): Int

    //seq read
    external fun seqRead(
        path: String,
        reclenKb: Int,
        filesizeKb: Int,
        numThread: Int
    ): Long

    //seq write 
    external fun seqWrite(
        path:String,
        reclenKb: Int,
        filesizeKb: Int,
        numThread: Int
    ): Long

    //random read
    external fun rndRead(
        path: String,
        reclenKb: Int,
        filesizeKb: Int,
        maxrecs: Int,
        seed: Int,
        numThread: Int
    ): Long

    //random write
    external fun rndWrite(
        path: String,
        reclenKb: Int,
        filesizeKb: Int,
        maxrecs: Int,
        seed: Int,
        numThread: Int
    ): Long

    //count record processed
    external fun getLastRndRecCount(): Long
    external fun getLastLatencies(): LongArray
}