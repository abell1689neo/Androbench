package kr.ac.snu.csl.androbench.history;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class HistoryDatabase_Impl extends HistoryDatabase {
  private volatile HistoryDao _historyDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(3) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `bench_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `targetPath` TEXT NOT NULL, `fsType` TEXT NOT NULL, `readOnly` INTEGER NOT NULL, `deviceModel` TEXT NOT NULL, `androidRelease` TEXT NOT NULL, `kernelPageKb` INTEGER NOT NULL, `seqReclenKb` INTEGER NOT NULL, `rndReclenKb` INTEGER NOT NULL, `fileSizeKb` INTEGER NOT NULL, `numThreads` INTEGER NOT NULL, `rndMaxRecs` INTEGER NOT NULL, `sqliteOperations` INTEGER NOT NULL, `numIterations` INTEGER NOT NULL, `seqReadMBps` REAL NOT NULL, `seqReadIops` REAL NOT NULL, `seqWriteMBps` REAL NOT NULL, `seqWriteIops` REAL NOT NULL, `rndReadMBps` REAL NOT NULL, `rndReadIops` REAL NOT NULL, `rndWriteMBps` REAL NOT NULL, `rndWriteIops` REAL NOT NULL, `sqliteInsertTps` REAL NOT NULL, `sqliteUpdateTps` REAL NOT NULL, `sqliteDeleteTps` REAL NOT NULL, `mode` TEXT NOT NULL, `fioReadMBps` REAL NOT NULL, `fioReadIops` REAL NOT NULL, `fioWriteMBps` REAL NOT NULL, `fioWriteIops` REAL NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'b8214f33714d00a6bd1a62c7292f8c23')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `bench_history`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsBenchHistory = new HashMap<String, TableInfo.Column>(31);
        _columnsBenchHistory.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("targetPath", new TableInfo.Column("targetPath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("fsType", new TableInfo.Column("fsType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("readOnly", new TableInfo.Column("readOnly", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("deviceModel", new TableInfo.Column("deviceModel", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("androidRelease", new TableInfo.Column("androidRelease", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("kernelPageKb", new TableInfo.Column("kernelPageKb", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("seqReclenKb", new TableInfo.Column("seqReclenKb", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("rndReclenKb", new TableInfo.Column("rndReclenKb", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("fileSizeKb", new TableInfo.Column("fileSizeKb", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("numThreads", new TableInfo.Column("numThreads", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("rndMaxRecs", new TableInfo.Column("rndMaxRecs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("sqliteOperations", new TableInfo.Column("sqliteOperations", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("numIterations", new TableInfo.Column("numIterations", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("seqReadMBps", new TableInfo.Column("seqReadMBps", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("seqReadIops", new TableInfo.Column("seqReadIops", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("seqWriteMBps", new TableInfo.Column("seqWriteMBps", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("seqWriteIops", new TableInfo.Column("seqWriteIops", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("rndReadMBps", new TableInfo.Column("rndReadMBps", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("rndReadIops", new TableInfo.Column("rndReadIops", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("rndWriteMBps", new TableInfo.Column("rndWriteMBps", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("rndWriteIops", new TableInfo.Column("rndWriteIops", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("sqliteInsertTps", new TableInfo.Column("sqliteInsertTps", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("sqliteUpdateTps", new TableInfo.Column("sqliteUpdateTps", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("sqliteDeleteTps", new TableInfo.Column("sqliteDeleteTps", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("mode", new TableInfo.Column("mode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("fioReadMBps", new TableInfo.Column("fioReadMBps", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("fioReadIops", new TableInfo.Column("fioReadIops", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("fioWriteMBps", new TableInfo.Column("fioWriteMBps", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBenchHistory.put("fioWriteIops", new TableInfo.Column("fioWriteIops", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBenchHistory = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBenchHistory = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoBenchHistory = new TableInfo("bench_history", _columnsBenchHistory, _foreignKeysBenchHistory, _indicesBenchHistory);
        final TableInfo _existingBenchHistory = TableInfo.read(db, "bench_history");
        if (!_infoBenchHistory.equals(_existingBenchHistory)) {
          return new RoomOpenHelper.ValidationResult(false, "bench_history(kr.ac.snu.csl.androbench.history.HistoryRecord).\n"
                  + " Expected:\n" + _infoBenchHistory + "\n"
                  + " Found:\n" + _existingBenchHistory);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "b8214f33714d00a6bd1a62c7292f8c23", "bf01fcfe475a2038fe1bd2f8b6881145");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "bench_history");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `bench_history`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(HistoryDao.class, HistoryDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public HistoryDao historyDao() {
    if (_historyDao != null) {
      return _historyDao;
    } else {
      synchronized(this) {
        if(_historyDao == null) {
          _historyDao = new HistoryDao_Impl(this);
        }
        return _historyDao;
      }
    }
  }
}
