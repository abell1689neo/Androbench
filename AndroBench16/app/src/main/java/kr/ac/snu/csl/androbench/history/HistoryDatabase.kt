package kr.ac.snu.csl.androbench.history
import android.content.Context
import android.content.pm.PackageInstaller
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

//table in this db, schema version,
@Database (entities=[HistoryRecord::class], version=4, exportSchema = false)
abstract class HistoryDatabase: RoomDatabase() {//roomdatabase 상속
    abstract fun historyDao(): HistoryDao//interface 구현부-> 호출 시 instance생성

    companion object{//static
        @Volatile private var INSTANCE: HistoryDatabase?=null //one instance

        //v3→v4: mmap 커널 회계 컬럼 추가 — ALTER라 기존 런 기록 보존됨
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bench_history ADD COLUMN mmapReadaheadOn INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE bench_history ADD COLUMN mmapLogicalBytes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE bench_history ADD COLUMN mmapKernelReadBytes INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE bench_history ADD COLUMN mmapKernelWriteBytes INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE bench_history ADD COLUMN mmapMajFaults INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE bench_history ADD COLUMN mmapMinFaults INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun get(context: Context): HistoryDatabase=
            INSTANCE?:synchronized(this){//locking
                INSTANCE?:Room.databaseBuilder(
                    context.applicationContext,
                    HistoryDatabase::class.java,
                    "androbench_history.db"
                ).addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration() //마이그레이션 없는 버전 점프만 파괴적
                    .build().also{INSTANCE=it} //instance에 저장

            }
    }
}