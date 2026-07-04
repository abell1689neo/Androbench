package kr.ac.snu.csl.androbench.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao//sql query interface
interface HistoryDao{
    @Insert
    suspend fun insert(record: HistoryRecord): Long

    @Query("SELECT * FROM bench_history ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<HistoryRecord>>//compose 계속 return

    @Query ("SELECT * FROM bench_history ORDER BY timestamp DESC")
    suspend fun listAll(): List<HistoryRecord> //csv export 등의 일회용 read

    @Query ("DELETE FROM bench_history WHERE id = :id")
    suspend fun deleteById(id:Long)

    @Query ("DELETE FROM bench_history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM bench_history")
    suspend fun count(): Int

}