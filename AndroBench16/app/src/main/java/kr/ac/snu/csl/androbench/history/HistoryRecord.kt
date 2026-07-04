package kr.ac.snu.csl.androbench.history

import androidx.room.Entity
import androidx.room.PrimaryKey

//sqlite table 1개에 대응함
@Entity(tableName = "bench_history")
data class HistoryRecord(
    @PrimaryKey(autoGenerate = true) val id: Long=0, //id 자동 부여 (rowid)
    val timestamp: Long,

    //env info
    val targetPath: String,
    val fsType: String,
    val readOnly: Boolean,

    //device meta
    val deviceModel: String,
    val androidRelease: String,
    val kernelPageKb: Int, //중요: 현재 사용하는 kernel page size 기록

    //config
    val seqReclenKb: Int,
    val rndReclenKb: Int,
    val fileSizeKb: Int,
    val numThreads: Int,
    val rndMaxRecs: Int,
    val sqliteOperations: Int,
    val numIterations: Int,

    //results (default)
    val seqReadMBps: Double,
    val seqReadIops: Double = 0.0,
    val seqWriteMBps: Double,
    val seqWriteIops: Double = 0.0,
    val rndReadMBps: Double,
    val rndReadIops: Double,
    val rndWriteMBps: Double,
    val rndWriteIops: Double,
    val sqliteInsertTps: Double,
    val sqliteUpdateTps: Double,
    val sqliteDeleteTps: Double,

    //results (fio)
    val mode:String="ALL",
    val fioReadMBps: Double = 0.0,
    val fioReadIops: Double = 0.0,
    val fioWriteMBps: Double = 0.0,
    val fioWriteIops: Double = 0.0,
)