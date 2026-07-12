package kr.ac.snu.csl.androbench.history
import android.content.Context
import android.os.Build
import kr.ac.snu.csl.androbench.bench.BenchmarkConfig
import kr.ac.snu.csl.androbench.bench.BenchmarkResult
import kotlinx.coroutines.flow.Flow


class HistoryRepository(context: Context) {//constructor
    private val dao= HistoryDatabase.get(context).historyDao() //function 구현체

    suspend fun saveRun( //coroutine 내에서만 호출 강제
        config: BenchmarkConfig,
        result: BenchmarkResult,
        fsType: String,
        readOnly: Boolean,
        kernelPageKb: Int,
    ): Long{
        val record= HistoryRecord(
            timestamp = System.currentTimeMillis(),
            targetPath = config.targetPath,
            fsType=fsType,
            readOnly = readOnly,
            deviceModel = Build.MODEL?: "unknown",
            androidRelease = Build.VERSION.RELEASE?:"unknown",
            kernelPageKb = kernelPageKb,
            seqReclenKb = config.seqReclenKb,
            rndReclenKb = config.rndReclenKb,
            fileSizeKb = config.fileSizeKb,
            numThreads = config.numThreads,
            rndMaxRecs = config.rndMaxRecs,
            sqliteOperations = config.sqliteOperations,
            numIterations = config.numIterations,
            seqReadMBps = result.seqReadMBps,
            seqReadIops = result.seqReadIops,
            seqWriteMBps = result.seqWriteMBps,
            seqWriteIops = result.seqWriteIops,
            rndReadMBps = result.rndReadMBps,
            rndReadIops = result.rndReadIops,
            rndWriteMBps = result.rndWriteMBps,
            rndWriteIops = result.rndWriteIops,
            sqliteInsertTps = result.sqliteInsertTps,
            sqliteUpdateTps = result.sqliteUpdateTps,
            sqliteDeleteTps = result.sqliteDeleteTps,
            mode=result.mode.name,
            fioReadMBps = result.fioReadMBps,
            fioReadIops = result.fioReadIops,
            fioWriteIops = result.fioWriteIops,
            fioWriteMBps = result.fioWriteMBps,
        )
        return dao.insert(record)
    }
    fun observeAll(): Flow<List<HistoryRecord>> =dao.observeAll()
    suspend fun listAll(): List<HistoryRecord> = dao.listAll()
    suspend fun deleteById(id: Long)=dao.deleteById(id)
    suspend fun deleteAll() = dao.deleteAll()
    suspend fun count()=dao.count()
}