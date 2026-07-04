package kr.ac.snu.csl.androbench.history;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class HistoryDao_Impl implements HistoryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<HistoryRecord> __insertionAdapterOfHistoryRecord;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public HistoryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfHistoryRecord = new EntityInsertionAdapter<HistoryRecord>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `bench_history` (`id`,`timestamp`,`targetPath`,`fsType`,`readOnly`,`deviceModel`,`androidRelease`,`kernelPageKb`,`seqReclenKb`,`rndReclenKb`,`fileSizeKb`,`numThreads`,`rndMaxRecs`,`sqliteOperations`,`numIterations`,`seqReadMBps`,`seqReadIops`,`seqWriteMBps`,`seqWriteIops`,`rndReadMBps`,`rndReadIops`,`rndWriteMBps`,`rndWriteIops`,`sqliteInsertTps`,`sqliteUpdateTps`,`sqliteDeleteTps`,`mode`,`fioReadMBps`,`fioReadIops`,`fioWriteMBps`,`fioWriteIops`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HistoryRecord entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getTimestamp());
        statement.bindString(3, entity.getTargetPath());
        statement.bindString(4, entity.getFsType());
        final int _tmp = entity.getReadOnly() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindString(6, entity.getDeviceModel());
        statement.bindString(7, entity.getAndroidRelease());
        statement.bindLong(8, entity.getKernelPageKb());
        statement.bindLong(9, entity.getSeqReclenKb());
        statement.bindLong(10, entity.getRndReclenKb());
        statement.bindLong(11, entity.getFileSizeKb());
        statement.bindLong(12, entity.getNumThreads());
        statement.bindLong(13, entity.getRndMaxRecs());
        statement.bindLong(14, entity.getSqliteOperations());
        statement.bindLong(15, entity.getNumIterations());
        statement.bindDouble(16, entity.getSeqReadMBps());
        statement.bindDouble(17, entity.getSeqReadIops());
        statement.bindDouble(18, entity.getSeqWriteMBps());
        statement.bindDouble(19, entity.getSeqWriteIops());
        statement.bindDouble(20, entity.getRndReadMBps());
        statement.bindDouble(21, entity.getRndReadIops());
        statement.bindDouble(22, entity.getRndWriteMBps());
        statement.bindDouble(23, entity.getRndWriteIops());
        statement.bindDouble(24, entity.getSqliteInsertTps());
        statement.bindDouble(25, entity.getSqliteUpdateTps());
        statement.bindDouble(26, entity.getSqliteDeleteTps());
        statement.bindString(27, entity.getMode());
        statement.bindDouble(28, entity.getFioReadMBps());
        statement.bindDouble(29, entity.getFioReadIops());
        statement.bindDouble(30, entity.getFioWriteMBps());
        statement.bindDouble(31, entity.getFioWriteIops());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM bench_history WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM bench_history";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final HistoryRecord record, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfHistoryRecord.insertAndReturnId(record);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<HistoryRecord>> observeAll() {
    final String _sql = "SELECT * FROM bench_history ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"bench_history"}, new Callable<List<HistoryRecord>>() {
      @Override
      @NonNull
      public List<HistoryRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfTargetPath = CursorUtil.getColumnIndexOrThrow(_cursor, "targetPath");
          final int _cursorIndexOfFsType = CursorUtil.getColumnIndexOrThrow(_cursor, "fsType");
          final int _cursorIndexOfReadOnly = CursorUtil.getColumnIndexOrThrow(_cursor, "readOnly");
          final int _cursorIndexOfDeviceModel = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceModel");
          final int _cursorIndexOfAndroidRelease = CursorUtil.getColumnIndexOrThrow(_cursor, "androidRelease");
          final int _cursorIndexOfKernelPageKb = CursorUtil.getColumnIndexOrThrow(_cursor, "kernelPageKb");
          final int _cursorIndexOfSeqReclenKb = CursorUtil.getColumnIndexOrThrow(_cursor, "seqReclenKb");
          final int _cursorIndexOfRndReclenKb = CursorUtil.getColumnIndexOrThrow(_cursor, "rndReclenKb");
          final int _cursorIndexOfFileSizeKb = CursorUtil.getColumnIndexOrThrow(_cursor, "fileSizeKb");
          final int _cursorIndexOfNumThreads = CursorUtil.getColumnIndexOrThrow(_cursor, "numThreads");
          final int _cursorIndexOfRndMaxRecs = CursorUtil.getColumnIndexOrThrow(_cursor, "rndMaxRecs");
          final int _cursorIndexOfSqliteOperations = CursorUtil.getColumnIndexOrThrow(_cursor, "sqliteOperations");
          final int _cursorIndexOfNumIterations = CursorUtil.getColumnIndexOrThrow(_cursor, "numIterations");
          final int _cursorIndexOfSeqReadMBps = CursorUtil.getColumnIndexOrThrow(_cursor, "seqReadMBps");
          final int _cursorIndexOfSeqReadIops = CursorUtil.getColumnIndexOrThrow(_cursor, "seqReadIops");
          final int _cursorIndexOfSeqWriteMBps = CursorUtil.getColumnIndexOrThrow(_cursor, "seqWriteMBps");
          final int _cursorIndexOfSeqWriteIops = CursorUtil.getColumnIndexOrThrow(_cursor, "seqWriteIops");
          final int _cursorIndexOfRndReadMBps = CursorUtil.getColumnIndexOrThrow(_cursor, "rndReadMBps");
          final int _cursorIndexOfRndReadIops = CursorUtil.getColumnIndexOrThrow(_cursor, "rndReadIops");
          final int _cursorIndexOfRndWriteMBps = CursorUtil.getColumnIndexOrThrow(_cursor, "rndWriteMBps");
          final int _cursorIndexOfRndWriteIops = CursorUtil.getColumnIndexOrThrow(_cursor, "rndWriteIops");
          final int _cursorIndexOfSqliteInsertTps = CursorUtil.getColumnIndexOrThrow(_cursor, "sqliteInsertTps");
          final int _cursorIndexOfSqliteUpdateTps = CursorUtil.getColumnIndexOrThrow(_cursor, "sqliteUpdateTps");
          final int _cursorIndexOfSqliteDeleteTps = CursorUtil.getColumnIndexOrThrow(_cursor, "sqliteDeleteTps");
          final int _cursorIndexOfMode = CursorUtil.getColumnIndexOrThrow(_cursor, "mode");
          final int _cursorIndexOfFioReadMBps = CursorUtil.getColumnIndexOrThrow(_cursor, "fioReadMBps");
          final int _cursorIndexOfFioReadIops = CursorUtil.getColumnIndexOrThrow(_cursor, "fioReadIops");
          final int _cursorIndexOfFioWriteMBps = CursorUtil.getColumnIndexOrThrow(_cursor, "fioWriteMBps");
          final int _cursorIndexOfFioWriteIops = CursorUtil.getColumnIndexOrThrow(_cursor, "fioWriteIops");
          final List<HistoryRecord> _result = new ArrayList<HistoryRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HistoryRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpTargetPath;
            _tmpTargetPath = _cursor.getString(_cursorIndexOfTargetPath);
            final String _tmpFsType;
            _tmpFsType = _cursor.getString(_cursorIndexOfFsType);
            final boolean _tmpReadOnly;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfReadOnly);
            _tmpReadOnly = _tmp != 0;
            final String _tmpDeviceModel;
            _tmpDeviceModel = _cursor.getString(_cursorIndexOfDeviceModel);
            final String _tmpAndroidRelease;
            _tmpAndroidRelease = _cursor.getString(_cursorIndexOfAndroidRelease);
            final int _tmpKernelPageKb;
            _tmpKernelPageKb = _cursor.getInt(_cursorIndexOfKernelPageKb);
            final int _tmpSeqReclenKb;
            _tmpSeqReclenKb = _cursor.getInt(_cursorIndexOfSeqReclenKb);
            final int _tmpRndReclenKb;
            _tmpRndReclenKb = _cursor.getInt(_cursorIndexOfRndReclenKb);
            final int _tmpFileSizeKb;
            _tmpFileSizeKb = _cursor.getInt(_cursorIndexOfFileSizeKb);
            final int _tmpNumThreads;
            _tmpNumThreads = _cursor.getInt(_cursorIndexOfNumThreads);
            final int _tmpRndMaxRecs;
            _tmpRndMaxRecs = _cursor.getInt(_cursorIndexOfRndMaxRecs);
            final int _tmpSqliteOperations;
            _tmpSqliteOperations = _cursor.getInt(_cursorIndexOfSqliteOperations);
            final int _tmpNumIterations;
            _tmpNumIterations = _cursor.getInt(_cursorIndexOfNumIterations);
            final double _tmpSeqReadMBps;
            _tmpSeqReadMBps = _cursor.getDouble(_cursorIndexOfSeqReadMBps);
            final double _tmpSeqReadIops;
            _tmpSeqReadIops = _cursor.getDouble(_cursorIndexOfSeqReadIops);
            final double _tmpSeqWriteMBps;
            _tmpSeqWriteMBps = _cursor.getDouble(_cursorIndexOfSeqWriteMBps);
            final double _tmpSeqWriteIops;
            _tmpSeqWriteIops = _cursor.getDouble(_cursorIndexOfSeqWriteIops);
            final double _tmpRndReadMBps;
            _tmpRndReadMBps = _cursor.getDouble(_cursorIndexOfRndReadMBps);
            final double _tmpRndReadIops;
            _tmpRndReadIops = _cursor.getDouble(_cursorIndexOfRndReadIops);
            final double _tmpRndWriteMBps;
            _tmpRndWriteMBps = _cursor.getDouble(_cursorIndexOfRndWriteMBps);
            final double _tmpRndWriteIops;
            _tmpRndWriteIops = _cursor.getDouble(_cursorIndexOfRndWriteIops);
            final double _tmpSqliteInsertTps;
            _tmpSqliteInsertTps = _cursor.getDouble(_cursorIndexOfSqliteInsertTps);
            final double _tmpSqliteUpdateTps;
            _tmpSqliteUpdateTps = _cursor.getDouble(_cursorIndexOfSqliteUpdateTps);
            final double _tmpSqliteDeleteTps;
            _tmpSqliteDeleteTps = _cursor.getDouble(_cursorIndexOfSqliteDeleteTps);
            final String _tmpMode;
            _tmpMode = _cursor.getString(_cursorIndexOfMode);
            final double _tmpFioReadMBps;
            _tmpFioReadMBps = _cursor.getDouble(_cursorIndexOfFioReadMBps);
            final double _tmpFioReadIops;
            _tmpFioReadIops = _cursor.getDouble(_cursorIndexOfFioReadIops);
            final double _tmpFioWriteMBps;
            _tmpFioWriteMBps = _cursor.getDouble(_cursorIndexOfFioWriteMBps);
            final double _tmpFioWriteIops;
            _tmpFioWriteIops = _cursor.getDouble(_cursorIndexOfFioWriteIops);
            _item = new HistoryRecord(_tmpId,_tmpTimestamp,_tmpTargetPath,_tmpFsType,_tmpReadOnly,_tmpDeviceModel,_tmpAndroidRelease,_tmpKernelPageKb,_tmpSeqReclenKb,_tmpRndReclenKb,_tmpFileSizeKb,_tmpNumThreads,_tmpRndMaxRecs,_tmpSqliteOperations,_tmpNumIterations,_tmpSeqReadMBps,_tmpSeqReadIops,_tmpSeqWriteMBps,_tmpSeqWriteIops,_tmpRndReadMBps,_tmpRndReadIops,_tmpRndWriteMBps,_tmpRndWriteIops,_tmpSqliteInsertTps,_tmpSqliteUpdateTps,_tmpSqliteDeleteTps,_tmpMode,_tmpFioReadMBps,_tmpFioReadIops,_tmpFioWriteMBps,_tmpFioWriteIops);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object listAll(final Continuation<? super List<HistoryRecord>> $completion) {
    final String _sql = "SELECT * FROM bench_history ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<HistoryRecord>>() {
      @Override
      @NonNull
      public List<HistoryRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfTargetPath = CursorUtil.getColumnIndexOrThrow(_cursor, "targetPath");
          final int _cursorIndexOfFsType = CursorUtil.getColumnIndexOrThrow(_cursor, "fsType");
          final int _cursorIndexOfReadOnly = CursorUtil.getColumnIndexOrThrow(_cursor, "readOnly");
          final int _cursorIndexOfDeviceModel = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceModel");
          final int _cursorIndexOfAndroidRelease = CursorUtil.getColumnIndexOrThrow(_cursor, "androidRelease");
          final int _cursorIndexOfKernelPageKb = CursorUtil.getColumnIndexOrThrow(_cursor, "kernelPageKb");
          final int _cursorIndexOfSeqReclenKb = CursorUtil.getColumnIndexOrThrow(_cursor, "seqReclenKb");
          final int _cursorIndexOfRndReclenKb = CursorUtil.getColumnIndexOrThrow(_cursor, "rndReclenKb");
          final int _cursorIndexOfFileSizeKb = CursorUtil.getColumnIndexOrThrow(_cursor, "fileSizeKb");
          final int _cursorIndexOfNumThreads = CursorUtil.getColumnIndexOrThrow(_cursor, "numThreads");
          final int _cursorIndexOfRndMaxRecs = CursorUtil.getColumnIndexOrThrow(_cursor, "rndMaxRecs");
          final int _cursorIndexOfSqliteOperations = CursorUtil.getColumnIndexOrThrow(_cursor, "sqliteOperations");
          final int _cursorIndexOfNumIterations = CursorUtil.getColumnIndexOrThrow(_cursor, "numIterations");
          final int _cursorIndexOfSeqReadMBps = CursorUtil.getColumnIndexOrThrow(_cursor, "seqReadMBps");
          final int _cursorIndexOfSeqReadIops = CursorUtil.getColumnIndexOrThrow(_cursor, "seqReadIops");
          final int _cursorIndexOfSeqWriteMBps = CursorUtil.getColumnIndexOrThrow(_cursor, "seqWriteMBps");
          final int _cursorIndexOfSeqWriteIops = CursorUtil.getColumnIndexOrThrow(_cursor, "seqWriteIops");
          final int _cursorIndexOfRndReadMBps = CursorUtil.getColumnIndexOrThrow(_cursor, "rndReadMBps");
          final int _cursorIndexOfRndReadIops = CursorUtil.getColumnIndexOrThrow(_cursor, "rndReadIops");
          final int _cursorIndexOfRndWriteMBps = CursorUtil.getColumnIndexOrThrow(_cursor, "rndWriteMBps");
          final int _cursorIndexOfRndWriteIops = CursorUtil.getColumnIndexOrThrow(_cursor, "rndWriteIops");
          final int _cursorIndexOfSqliteInsertTps = CursorUtil.getColumnIndexOrThrow(_cursor, "sqliteInsertTps");
          final int _cursorIndexOfSqliteUpdateTps = CursorUtil.getColumnIndexOrThrow(_cursor, "sqliteUpdateTps");
          final int _cursorIndexOfSqliteDeleteTps = CursorUtil.getColumnIndexOrThrow(_cursor, "sqliteDeleteTps");
          final int _cursorIndexOfMode = CursorUtil.getColumnIndexOrThrow(_cursor, "mode");
          final int _cursorIndexOfFioReadMBps = CursorUtil.getColumnIndexOrThrow(_cursor, "fioReadMBps");
          final int _cursorIndexOfFioReadIops = CursorUtil.getColumnIndexOrThrow(_cursor, "fioReadIops");
          final int _cursorIndexOfFioWriteMBps = CursorUtil.getColumnIndexOrThrow(_cursor, "fioWriteMBps");
          final int _cursorIndexOfFioWriteIops = CursorUtil.getColumnIndexOrThrow(_cursor, "fioWriteIops");
          final List<HistoryRecord> _result = new ArrayList<HistoryRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HistoryRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpTargetPath;
            _tmpTargetPath = _cursor.getString(_cursorIndexOfTargetPath);
            final String _tmpFsType;
            _tmpFsType = _cursor.getString(_cursorIndexOfFsType);
            final boolean _tmpReadOnly;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfReadOnly);
            _tmpReadOnly = _tmp != 0;
            final String _tmpDeviceModel;
            _tmpDeviceModel = _cursor.getString(_cursorIndexOfDeviceModel);
            final String _tmpAndroidRelease;
            _tmpAndroidRelease = _cursor.getString(_cursorIndexOfAndroidRelease);
            final int _tmpKernelPageKb;
            _tmpKernelPageKb = _cursor.getInt(_cursorIndexOfKernelPageKb);
            final int _tmpSeqReclenKb;
            _tmpSeqReclenKb = _cursor.getInt(_cursorIndexOfSeqReclenKb);
            final int _tmpRndReclenKb;
            _tmpRndReclenKb = _cursor.getInt(_cursorIndexOfRndReclenKb);
            final int _tmpFileSizeKb;
            _tmpFileSizeKb = _cursor.getInt(_cursorIndexOfFileSizeKb);
            final int _tmpNumThreads;
            _tmpNumThreads = _cursor.getInt(_cursorIndexOfNumThreads);
            final int _tmpRndMaxRecs;
            _tmpRndMaxRecs = _cursor.getInt(_cursorIndexOfRndMaxRecs);
            final int _tmpSqliteOperations;
            _tmpSqliteOperations = _cursor.getInt(_cursorIndexOfSqliteOperations);
            final int _tmpNumIterations;
            _tmpNumIterations = _cursor.getInt(_cursorIndexOfNumIterations);
            final double _tmpSeqReadMBps;
            _tmpSeqReadMBps = _cursor.getDouble(_cursorIndexOfSeqReadMBps);
            final double _tmpSeqReadIops;
            _tmpSeqReadIops = _cursor.getDouble(_cursorIndexOfSeqReadIops);
            final double _tmpSeqWriteMBps;
            _tmpSeqWriteMBps = _cursor.getDouble(_cursorIndexOfSeqWriteMBps);
            final double _tmpSeqWriteIops;
            _tmpSeqWriteIops = _cursor.getDouble(_cursorIndexOfSeqWriteIops);
            final double _tmpRndReadMBps;
            _tmpRndReadMBps = _cursor.getDouble(_cursorIndexOfRndReadMBps);
            final double _tmpRndReadIops;
            _tmpRndReadIops = _cursor.getDouble(_cursorIndexOfRndReadIops);
            final double _tmpRndWriteMBps;
            _tmpRndWriteMBps = _cursor.getDouble(_cursorIndexOfRndWriteMBps);
            final double _tmpRndWriteIops;
            _tmpRndWriteIops = _cursor.getDouble(_cursorIndexOfRndWriteIops);
            final double _tmpSqliteInsertTps;
            _tmpSqliteInsertTps = _cursor.getDouble(_cursorIndexOfSqliteInsertTps);
            final double _tmpSqliteUpdateTps;
            _tmpSqliteUpdateTps = _cursor.getDouble(_cursorIndexOfSqliteUpdateTps);
            final double _tmpSqliteDeleteTps;
            _tmpSqliteDeleteTps = _cursor.getDouble(_cursorIndexOfSqliteDeleteTps);
            final String _tmpMode;
            _tmpMode = _cursor.getString(_cursorIndexOfMode);
            final double _tmpFioReadMBps;
            _tmpFioReadMBps = _cursor.getDouble(_cursorIndexOfFioReadMBps);
            final double _tmpFioReadIops;
            _tmpFioReadIops = _cursor.getDouble(_cursorIndexOfFioReadIops);
            final double _tmpFioWriteMBps;
            _tmpFioWriteMBps = _cursor.getDouble(_cursorIndexOfFioWriteMBps);
            final double _tmpFioWriteIops;
            _tmpFioWriteIops = _cursor.getDouble(_cursorIndexOfFioWriteIops);
            _item = new HistoryRecord(_tmpId,_tmpTimestamp,_tmpTargetPath,_tmpFsType,_tmpReadOnly,_tmpDeviceModel,_tmpAndroidRelease,_tmpKernelPageKb,_tmpSeqReclenKb,_tmpRndReclenKb,_tmpFileSizeKb,_tmpNumThreads,_tmpRndMaxRecs,_tmpSqliteOperations,_tmpNumIterations,_tmpSeqReadMBps,_tmpSeqReadIops,_tmpSeqWriteMBps,_tmpSeqWriteIops,_tmpRndReadMBps,_tmpRndReadIops,_tmpRndWriteMBps,_tmpRndWriteIops,_tmpSqliteInsertTps,_tmpSqliteUpdateTps,_tmpSqliteDeleteTps,_tmpMode,_tmpFioReadMBps,_tmpFioReadIops,_tmpFioWriteMBps,_tmpFioWriteIops);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object count(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM bench_history";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
