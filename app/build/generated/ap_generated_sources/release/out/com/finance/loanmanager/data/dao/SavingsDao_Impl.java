package com.finance.loanmanager.data.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.finance.loanmanager.data.entity.Savings;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SavingsDao_Impl implements SavingsDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Savings> __insertionAdapterOfSavings;

  private final EntityDeletionOrUpdateAdapter<Savings> __deletionAdapterOfSavings;

  private final EntityDeletionOrUpdateAdapter<Savings> __updateAdapterOfSavings;

  private final SharedSQLiteStatement __preparedStmtOfDeleteSavingsById;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllSavings;

  public SavingsDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSavings = new EntityInsertionAdapter<Savings>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `savings` (`id`,`amount`,`date`,`note`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Savings entity) {
        statement.bindLong(1, entity.getId());
        statement.bindDouble(2, entity.getAmount());
        if (entity.getDate() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getDate());
        }
        if (entity.getNote() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getNote());
        }
        statement.bindLong(5, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfSavings = new EntityDeletionOrUpdateAdapter<Savings>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `savings` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Savings entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfSavings = new EntityDeletionOrUpdateAdapter<Savings>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `savings` SET `id` = ?,`amount` = ?,`date` = ?,`note` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Savings entity) {
        statement.bindLong(1, entity.getId());
        statement.bindDouble(2, entity.getAmount());
        if (entity.getDate() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getDate());
        }
        if (entity.getNote() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getNote());
        }
        statement.bindLong(5, entity.getCreatedAt());
        statement.bindLong(6, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteSavingsById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM savings WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllSavings = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM savings";
        return _query;
      }
    };
  }

  @Override
  public long insertSavings(final Savings savings) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      final long _result = __insertionAdapterOfSavings.insertAndReturnId(savings);
      __db.setTransactionSuccessful();
      return _result;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insertAllSavings(final List<Savings> savingsList) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfSavings.insert(savingsList);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteSavings(final Savings savings) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfSavings.handle(savings);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void updateSavings(final Savings savings) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfSavings.handle(savings);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteSavingsById(final int id) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteSavingsById.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, id);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfDeleteSavingsById.release(_stmt);
    }
  }

  @Override
  public void deleteAllSavings() {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllSavings.acquire();
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfDeleteAllSavings.release(_stmt);
    }
  }

  @Override
  public LiveData<List<Savings>> getAllSavingsLive() {
    final String _sql = "SELECT * FROM savings ORDER BY date DESC, createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"savings"}, false, new Callable<List<Savings>>() {
      @Override
      @Nullable
      public List<Savings> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<Savings> _result = new ArrayList<Savings>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Savings _item;
            _item = new Savings();
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            _item.setId(_tmpId);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            _item.setAmount(_tmpAmount);
            final String _tmpDate;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmpDate = null;
            } else {
              _tmpDate = _cursor.getString(_cursorIndexOfDate);
            }
            _item.setDate(_tmpDate);
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            _item.setNote(_tmpNote);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item.setCreatedAt(_tmpCreatedAt);
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
  public List<Savings> getAllSavings() {
    final String _sql = "SELECT * FROM savings ORDER BY date DESC, createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
      final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
      final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
      final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
      final List<Savings> _result = new ArrayList<Savings>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final Savings _item;
        _item = new Savings();
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        _item.setId(_tmpId);
        final double _tmpAmount;
        _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
        _item.setAmount(_tmpAmount);
        final String _tmpDate;
        if (_cursor.isNull(_cursorIndexOfDate)) {
          _tmpDate = null;
        } else {
          _tmpDate = _cursor.getString(_cursorIndexOfDate);
        }
        _item.setDate(_tmpDate);
        final String _tmpNote;
        if (_cursor.isNull(_cursorIndexOfNote)) {
          _tmpNote = null;
        } else {
          _tmpNote = _cursor.getString(_cursorIndexOfNote);
        }
        _item.setNote(_tmpNote);
        final long _tmpCreatedAt;
        _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
        _item.setCreatedAt(_tmpCreatedAt);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<Savings> getRecentSavings(final int limit) {
    final String _sql = "SELECT * FROM savings ORDER BY date DESC, createdAt DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
      final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
      final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
      final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
      final List<Savings> _result = new ArrayList<Savings>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final Savings _item;
        _item = new Savings();
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        _item.setId(_tmpId);
        final double _tmpAmount;
        _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
        _item.setAmount(_tmpAmount);
        final String _tmpDate;
        if (_cursor.isNull(_cursorIndexOfDate)) {
          _tmpDate = null;
        } else {
          _tmpDate = _cursor.getString(_cursorIndexOfDate);
        }
        _item.setDate(_tmpDate);
        final String _tmpNote;
        if (_cursor.isNull(_cursorIndexOfNote)) {
          _tmpNote = null;
        } else {
          _tmpNote = _cursor.getString(_cursorIndexOfNote);
        }
        _item.setNote(_tmpNote);
        final long _tmpCreatedAt;
        _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
        _item.setCreatedAt(_tmpCreatedAt);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public Savings getSavingsById(final int id) {
    final String _sql = "SELECT * FROM savings WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
      final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
      final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
      final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
      final Savings _result;
      if (_cursor.moveToFirst()) {
        _result = new Savings();
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        _result.setId(_tmpId);
        final double _tmpAmount;
        _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
        _result.setAmount(_tmpAmount);
        final String _tmpDate;
        if (_cursor.isNull(_cursorIndexOfDate)) {
          _tmpDate = null;
        } else {
          _tmpDate = _cursor.getString(_cursorIndexOfDate);
        }
        _result.setDate(_tmpDate);
        final String _tmpNote;
        if (_cursor.isNull(_cursorIndexOfNote)) {
          _tmpNote = null;
        } else {
          _tmpNote = _cursor.getString(_cursorIndexOfNote);
        }
        _result.setNote(_tmpNote);
        final long _tmpCreatedAt;
        _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
        _result.setCreatedAt(_tmpCreatedAt);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public double getTotalBalance() {
    final String _sql = "SELECT COALESCE(SUM(amount), 0) FROM savings";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final double _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getDouble(0);
      } else {
        _result = 0.0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public double getTotalDeposit() {
    final String _sql = "SELECT COALESCE(SUM(amount), 0) FROM savings WHERE amount > 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final double _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getDouble(0);
      } else {
        _result = 0.0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public double getTotalWithdraw() {
    final String _sql = "SELECT COALESCE(SUM(amount), 0) FROM savings WHERE amount < 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final double _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getDouble(0);
      } else {
        _result = 0.0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<Savings> getAllSavingsAsc() {
    final String _sql = "SELECT * FROM savings ORDER BY date ASC, createdAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
      final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
      final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
      final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
      final List<Savings> _result = new ArrayList<Savings>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final Savings _item;
        _item = new Savings();
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        _item.setId(_tmpId);
        final double _tmpAmount;
        _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
        _item.setAmount(_tmpAmount);
        final String _tmpDate;
        if (_cursor.isNull(_cursorIndexOfDate)) {
          _tmpDate = null;
        } else {
          _tmpDate = _cursor.getString(_cursorIndexOfDate);
        }
        _item.setDate(_tmpDate);
        final String _tmpNote;
        if (_cursor.isNull(_cursorIndexOfNote)) {
          _tmpNote = null;
        } else {
          _tmpNote = _cursor.getString(_cursorIndexOfNote);
        }
        _item.setNote(_tmpNote);
        final long _tmpCreatedAt;
        _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
        _item.setCreatedAt(_tmpCreatedAt);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public int getSavingsCount() {
    final String _sql = "SELECT COUNT(*) FROM savings";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
