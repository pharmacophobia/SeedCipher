package com.seedcipher.app.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SmsDao_Impl implements SmsDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CachedSms> __insertionAdapterOfCachedSms;

  private final SharedSQLiteStatement __preparedStmtOfClearCacheForNewSeed;

  private final SharedSQLiteStatement __preparedStmtOfPruneOldMessages;

  private final SharedSQLiteStatement __preparedStmtOfClearAll;

  public SmsDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCachedSms = new EntityInsertionAdapter<CachedSms>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `cached_sms` (`messageId`,`address`,`rawBody`,`decryptedBody`,`seedUsed`,`timestamp`,`isOutgoing`,`isEncrypted`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CachedSms entity) {
        statement.bindLong(1, entity.getMessageId());
        statement.bindString(2, entity.getAddress());
        statement.bindString(3, entity.getRawBody());
        if (entity.getDecryptedBody() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getDecryptedBody());
        }
        if (entity.getSeedUsed() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getSeedUsed());
        }
        statement.bindLong(6, entity.getTimestamp());
        final int _tmp = entity.isOutgoing() ? 1 : 0;
        statement.bindLong(7, _tmp);
        final int _tmp_1 = entity.isEncrypted() ? 1 : 0;
        statement.bindLong(8, _tmp_1);
      }
    };
    this.__preparedStmtOfClearCacheForNewSeed = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM cached_sms WHERE seedUsed != ?";
        return _query;
      }
    };
    this.__preparedStmtOfPruneOldMessages = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM cached_sms WHERE timestamp < ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM cached_sms";
        return _query;
      }
    };
  }

  @Override
  public Object insertMessages(final List<CachedSms> messages,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCachedSms.insert(messages);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clearCacheForNewSeed(final String currentSeed,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearCacheForNewSeed.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, currentSeed);
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
          __preparedStmtOfClearCacheForNewSeed.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object pruneOldMessages(final long threshold,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfPruneOldMessages.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, threshold);
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
          __preparedStmtOfPruneOldMessages.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAll.acquire();
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
          __preparedStmtOfClearAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getMessagesForAddress(final String address,
      final Continuation<? super List<CachedSms>> $completion) {
    final String _sql = "SELECT * FROM cached_sms WHERE address = ? ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, address);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CachedSms>>() {
      @Override
      @NonNull
      public List<CachedSms> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfMessageId = CursorUtil.getColumnIndexOrThrow(_cursor, "messageId");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfRawBody = CursorUtil.getColumnIndexOrThrow(_cursor, "rawBody");
          final int _cursorIndexOfDecryptedBody = CursorUtil.getColumnIndexOrThrow(_cursor, "decryptedBody");
          final int _cursorIndexOfSeedUsed = CursorUtil.getColumnIndexOrThrow(_cursor, "seedUsed");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfIsOutgoing = CursorUtil.getColumnIndexOrThrow(_cursor, "isOutgoing");
          final int _cursorIndexOfIsEncrypted = CursorUtil.getColumnIndexOrThrow(_cursor, "isEncrypted");
          final List<CachedSms> _result = new ArrayList<CachedSms>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CachedSms _item;
            final long _tmpMessageId;
            _tmpMessageId = _cursor.getLong(_cursorIndexOfMessageId);
            final String _tmpAddress;
            _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            final String _tmpRawBody;
            _tmpRawBody = _cursor.getString(_cursorIndexOfRawBody);
            final String _tmpDecryptedBody;
            if (_cursor.isNull(_cursorIndexOfDecryptedBody)) {
              _tmpDecryptedBody = null;
            } else {
              _tmpDecryptedBody = _cursor.getString(_cursorIndexOfDecryptedBody);
            }
            final String _tmpSeedUsed;
            if (_cursor.isNull(_cursorIndexOfSeedUsed)) {
              _tmpSeedUsed = null;
            } else {
              _tmpSeedUsed = _cursor.getString(_cursorIndexOfSeedUsed);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final boolean _tmpIsOutgoing;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsOutgoing);
            _tmpIsOutgoing = _tmp != 0;
            final boolean _tmpIsEncrypted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsEncrypted);
            _tmpIsEncrypted = _tmp_1 != 0;
            _item = new CachedSms(_tmpMessageId,_tmpAddress,_tmpRawBody,_tmpDecryptedBody,_tmpSeedUsed,_tmpTimestamp,_tmpIsOutgoing,_tmpIsEncrypted);
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
  public Object getMessageById(final long messageId,
      final Continuation<? super CachedSms> $completion) {
    final String _sql = "SELECT * FROM cached_sms WHERE messageId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, messageId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CachedSms>() {
      @Override
      @Nullable
      public CachedSms call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfMessageId = CursorUtil.getColumnIndexOrThrow(_cursor, "messageId");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfRawBody = CursorUtil.getColumnIndexOrThrow(_cursor, "rawBody");
          final int _cursorIndexOfDecryptedBody = CursorUtil.getColumnIndexOrThrow(_cursor, "decryptedBody");
          final int _cursorIndexOfSeedUsed = CursorUtil.getColumnIndexOrThrow(_cursor, "seedUsed");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfIsOutgoing = CursorUtil.getColumnIndexOrThrow(_cursor, "isOutgoing");
          final int _cursorIndexOfIsEncrypted = CursorUtil.getColumnIndexOrThrow(_cursor, "isEncrypted");
          final CachedSms _result;
          if (_cursor.moveToFirst()) {
            final long _tmpMessageId;
            _tmpMessageId = _cursor.getLong(_cursorIndexOfMessageId);
            final String _tmpAddress;
            _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            final String _tmpRawBody;
            _tmpRawBody = _cursor.getString(_cursorIndexOfRawBody);
            final String _tmpDecryptedBody;
            if (_cursor.isNull(_cursorIndexOfDecryptedBody)) {
              _tmpDecryptedBody = null;
            } else {
              _tmpDecryptedBody = _cursor.getString(_cursorIndexOfDecryptedBody);
            }
            final String _tmpSeedUsed;
            if (_cursor.isNull(_cursorIndexOfSeedUsed)) {
              _tmpSeedUsed = null;
            } else {
              _tmpSeedUsed = _cursor.getString(_cursorIndexOfSeedUsed);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final boolean _tmpIsOutgoing;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsOutgoing);
            _tmpIsOutgoing = _tmp != 0;
            final boolean _tmpIsEncrypted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsEncrypted);
            _tmpIsEncrypted = _tmp_1 != 0;
            _result = new CachedSms(_tmpMessageId,_tmpAddress,_tmpRawBody,_tmpDecryptedBody,_tmpSeedUsed,_tmpTimestamp,_tmpIsOutgoing,_tmpIsEncrypted);
          } else {
            _result = null;
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
