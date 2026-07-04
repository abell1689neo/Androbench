package kr.ac.snu.csl.androbench.history
import android.content.Context
import android.content.pm.PackageInstaller
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

//table in this db, schema version,
@Database (entities=[HistoryRecord::class], version=3, exportSchema = false)
abstract class HistoryDatabase: RoomDatabase() {//roomdatabase 상속
    abstract fun historyDao(): HistoryDao//interface 구현부-> 호출 시 instance생성

    companion object{//static
        @Volatile private var INSTANCE: HistoryDatabase?=null //one instance

        fun get(context: Context): HistoryDatabase=
            INSTANCE?:synchronized(this){//locking
                INSTANCE?:Room.databaseBuilder(
                    context.applicationContext,
                    HistoryDatabase::class.java,
                    "androbench_history.db"
                ).fallbackToDestructiveMigration() //field 변경됨
                    .build().also{INSTANCE=it} //instance에 저장

            }
    }
}