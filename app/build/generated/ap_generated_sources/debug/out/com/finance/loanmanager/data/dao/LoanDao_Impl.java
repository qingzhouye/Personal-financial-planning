package com.finance.loanmanager.data.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import androidx.lifecycle.LiveData;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.RelationUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.finance.loanmanager.data.entity.Loan;
import com.finance.loanmanager.data.entity.LoanWithPayments;
import com.finance.loanmanager.data.entity.Payment;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class LoanDao_Impl implements LoanDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Loan> __insertionAdapterOfLoan;

  private final EntityDeletionOrUpdateAdapter<Loan> __deletionAdapterOfLoan;

  private final EntityDeletionOrUpdateAdapter<Loan> __updateAdapterOfLoan;

  private final SharedSQLiteStatement __preparedStmtOfDeleteLoanById;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllLoans;

  public LoanDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLoan = new EntityInsertionAdapter<Loan>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `loans` (`id`,`name`,`loanType`,`repaymentMethod`,`principal`,`annualRate`,`months`,`startDate`,`creditLimit`,`dueDate`,`originalMonthlyPayment`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Loan entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getName());
        }
        if (entity.getLoanType() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getLoanType());
        }
        if (entity.getRepaymentMethod() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getRepaymentMethod());
        }
        statement.bindDouble(5, entity.getPrincipal());
        statement.bindDouble(6, entity.getAnnualRate());
        statement.bindLong(7, entity.getMonths());
        if (entity.getStartDate() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getStartDate());
        }
        statement.bindDouble(9, entity.getCreditLimit());
        statement.bindLong(10, entity.getDueDate());
        statement.bindDouble(11, entity.getOriginalMonthlyPayment());
      }
    };
    this.__deletionAdapterOfLoan = new EntityDeletionOrUpdateAdapter<Loan>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `loans` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Loan entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfLoan = new EntityDeletionOrUpdateAdapter<Loan>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `loans` SET `id` = ?,`name` = ?,`loanType` = ?,`repaymentMethod` = ?,`principal` = ?,`annualRate` = ?,`months` = ?,`startDate` = ?,`creditLimit` = ?,`dueDate` = ?,`originalMonthlyPayment` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Loan entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getName());
        }
        if (entity.getLoanType() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getLoanType());
        }
        if (entity.getRepaymentMethod() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getRepaymentMethod());
        }
        statement.bindDouble(5, entity.getPrincipal());
        statement.bindDouble(6, entity.getAnnualRate());
        statement.bindLong(7, entity.getMonths());
        if (entity.getStartDate() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getStartDate());
        }
        statement.bindDouble(9, entity.getCreditLimit());
        statement.bindLong(10, entity.getDueDate());
        statement.bindDouble(11, entity.getOriginalMonthlyPayment());
        statement.bindLong(12, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteLoanById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM loans WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllLoans = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM loans";
        return _query;
      }
    };
  }

  @Override
  public long insertLoan(final Loan loan) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      final long _result = __insertionAdapterOfLoan.insertAndReturnId(loan);
      __db.setTransactionSuccessful();
      return _result;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteLoan(final Loan loan) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfLoan.handle(loan);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void updateLoan(final Loan loan) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfLoan.handle(loan);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteLoanById(final int loanId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteLoanById.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, loanId);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfDeleteLoanById.release(_stmt);
    }
  }

  @Override
  public void deleteAllLoans() {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllLoans.acquire();
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfDeleteAllLoans.release(_stmt);
    }
  }

  @Override
  public LiveData<List<Loan>> getAllLoansLive() {
    final String _sql = "SELECT * FROM loans ORDER BY id DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"loans"}, false, new Callable<List<Loan>>() {
      @Override
      @Nullable
      public List<Loan> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfLoanType = CursorUtil.getColumnIndexOrThrow(_cursor, "loanType");
          final int _cursorIndexOfRepaymentMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "repaymentMethod");
          final int _cursorIndexOfPrincipal = CursorUtil.getColumnIndexOrThrow(_cursor, "principal");
          final int _cursorIndexOfAnnualRate = CursorUtil.getColumnIndexOrThrow(_cursor, "annualRate");
          final int _cursorIndexOfMonths = CursorUtil.getColumnIndexOrThrow(_cursor, "months");
          final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
          final int _cursorIndexOfCreditLimit = CursorUtil.getColumnIndexOrThrow(_cursor, "creditLimit");
          final int _cursorIndexOfDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "dueDate");
          final int _cursorIndexOfOriginalMonthlyPayment = CursorUtil.getColumnIndexOrThrow(_cursor, "originalMonthlyPayment");
          final List<Loan> _result = new ArrayList<Loan>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Loan _item;
            _item = new Loan();
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            _item.setId(_tmpId);
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            _item.setName(_tmpName);
            final String _tmpLoanType;
            if (_cursor.isNull(_cursorIndexOfLoanType)) {
              _tmpLoanType = null;
            } else {
              _tmpLoanType = _cursor.getString(_cursorIndexOfLoanType);
            }
            _item.setLoanType(_tmpLoanType);
            final String _tmpRepaymentMethod;
            if (_cursor.isNull(_cursorIndexOfRepaymentMethod)) {
              _tmpRepaymentMethod = null;
            } else {
              _tmpRepaymentMethod = _cursor.getString(_cursorIndexOfRepaymentMethod);
            }
            _item.setRepaymentMethod(_tmpRepaymentMethod);
            final double _tmpPrincipal;
            _tmpPrincipal = _cursor.getDouble(_cursorIndexOfPrincipal);
            _item.setPrincipal(_tmpPrincipal);
            final double _tmpAnnualRate;
            _tmpAnnualRate = _cursor.getDouble(_cursorIndexOfAnnualRate);
            _item.setAnnualRate(_tmpAnnualRate);
            final int _tmpMonths;
            _tmpMonths = _cursor.getInt(_cursorIndexOfMonths);
            _item.setMonths(_tmpMonths);
            final String _tmpStartDate;
            if (_cursor.isNull(_cursorIndexOfStartDate)) {
              _tmpStartDate = null;
            } else {
              _tmpStartDate = _cursor.getString(_cursorIndexOfStartDate);
            }
            _item.setStartDate(_tmpStartDate);
            final double _tmpCreditLimit;
            _tmpCreditLimit = _cursor.getDouble(_cursorIndexOfCreditLimit);
            _item.setCreditLimit(_tmpCreditLimit);
            final int _tmpDueDate;
            _tmpDueDate = _cursor.getInt(_cursorIndexOfDueDate);
            _item.setDueDate(_tmpDueDate);
            final double _tmpOriginalMonthlyPayment;
            _tmpOriginalMonthlyPayment = _cursor.getDouble(_cursorIndexOfOriginalMonthlyPayment);
            _item.setOriginalMonthlyPayment(_tmpOriginalMonthlyPayment);
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
  public List<Loan> getAllLoans() {
    final String _sql = "SELECT * FROM loans ORDER BY id DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
      final int _cursorIndexOfLoanType = CursorUtil.getColumnIndexOrThrow(_cursor, "loanType");
      final int _cursorIndexOfRepaymentMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "repaymentMethod");
      final int _cursorIndexOfPrincipal = CursorUtil.getColumnIndexOrThrow(_cursor, "principal");
      final int _cursorIndexOfAnnualRate = CursorUtil.getColumnIndexOrThrow(_cursor, "annualRate");
      final int _cursorIndexOfMonths = CursorUtil.getColumnIndexOrThrow(_cursor, "months");
      final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
      final int _cursorIndexOfCreditLimit = CursorUtil.getColumnIndexOrThrow(_cursor, "creditLimit");
      final int _cursorIndexOfDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "dueDate");
      final int _cursorIndexOfOriginalMonthlyPayment = CursorUtil.getColumnIndexOrThrow(_cursor, "originalMonthlyPayment");
      final List<Loan> _result = new ArrayList<Loan>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final Loan _item;
        _item = new Loan();
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        _item.setId(_tmpId);
        final String _tmpName;
        if (_cursor.isNull(_cursorIndexOfName)) {
          _tmpName = null;
        } else {
          _tmpName = _cursor.getString(_cursorIndexOfName);
        }
        _item.setName(_tmpName);
        final String _tmpLoanType;
        if (_cursor.isNull(_cursorIndexOfLoanType)) {
          _tmpLoanType = null;
        } else {
          _tmpLoanType = _cursor.getString(_cursorIndexOfLoanType);
        }
        _item.setLoanType(_tmpLoanType);
        final String _tmpRepaymentMethod;
        if (_cursor.isNull(_cursorIndexOfRepaymentMethod)) {
          _tmpRepaymentMethod = null;
        } else {
          _tmpRepaymentMethod = _cursor.getString(_cursorIndexOfRepaymentMethod);
        }
        _item.setRepaymentMethod(_tmpRepaymentMethod);
        final double _tmpPrincipal;
        _tmpPrincipal = _cursor.getDouble(_cursorIndexOfPrincipal);
        _item.setPrincipal(_tmpPrincipal);
        final double _tmpAnnualRate;
        _tmpAnnualRate = _cursor.getDouble(_cursorIndexOfAnnualRate);
        _item.setAnnualRate(_tmpAnnualRate);
        final int _tmpMonths;
        _tmpMonths = _cursor.getInt(_cursorIndexOfMonths);
        _item.setMonths(_tmpMonths);
        final String _tmpStartDate;
        if (_cursor.isNull(_cursorIndexOfStartDate)) {
          _tmpStartDate = null;
        } else {
          _tmpStartDate = _cursor.getString(_cursorIndexOfStartDate);
        }
        _item.setStartDate(_tmpStartDate);
        final double _tmpCreditLimit;
        _tmpCreditLimit = _cursor.getDouble(_cursorIndexOfCreditLimit);
        _item.setCreditLimit(_tmpCreditLimit);
        final int _tmpDueDate;
        _tmpDueDate = _cursor.getInt(_cursorIndexOfDueDate);
        _item.setDueDate(_tmpDueDate);
        final double _tmpOriginalMonthlyPayment;
        _tmpOriginalMonthlyPayment = _cursor.getDouble(_cursorIndexOfOriginalMonthlyPayment);
        _item.setOriginalMonthlyPayment(_tmpOriginalMonthlyPayment);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public Loan getLoanById(final int loanId) {
    final String _sql = "SELECT * FROM loans WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, loanId);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
      final int _cursorIndexOfLoanType = CursorUtil.getColumnIndexOrThrow(_cursor, "loanType");
      final int _cursorIndexOfRepaymentMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "repaymentMethod");
      final int _cursorIndexOfPrincipal = CursorUtil.getColumnIndexOrThrow(_cursor, "principal");
      final int _cursorIndexOfAnnualRate = CursorUtil.getColumnIndexOrThrow(_cursor, "annualRate");
      final int _cursorIndexOfMonths = CursorUtil.getColumnIndexOrThrow(_cursor, "months");
      final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
      final int _cursorIndexOfCreditLimit = CursorUtil.getColumnIndexOrThrow(_cursor, "creditLimit");
      final int _cursorIndexOfDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "dueDate");
      final int _cursorIndexOfOriginalMonthlyPayment = CursorUtil.getColumnIndexOrThrow(_cursor, "originalMonthlyPayment");
      final Loan _result;
      if (_cursor.moveToFirst()) {
        _result = new Loan();
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        _result.setId(_tmpId);
        final String _tmpName;
        if (_cursor.isNull(_cursorIndexOfName)) {
          _tmpName = null;
        } else {
          _tmpName = _cursor.getString(_cursorIndexOfName);
        }
        _result.setName(_tmpName);
        final String _tmpLoanType;
        if (_cursor.isNull(_cursorIndexOfLoanType)) {
          _tmpLoanType = null;
        } else {
          _tmpLoanType = _cursor.getString(_cursorIndexOfLoanType);
        }
        _result.setLoanType(_tmpLoanType);
        final String _tmpRepaymentMethod;
        if (_cursor.isNull(_cursorIndexOfRepaymentMethod)) {
          _tmpRepaymentMethod = null;
        } else {
          _tmpRepaymentMethod = _cursor.getString(_cursorIndexOfRepaymentMethod);
        }
        _result.setRepaymentMethod(_tmpRepaymentMethod);
        final double _tmpPrincipal;
        _tmpPrincipal = _cursor.getDouble(_cursorIndexOfPrincipal);
        _result.setPrincipal(_tmpPrincipal);
        final double _tmpAnnualRate;
        _tmpAnnualRate = _cursor.getDouble(_cursorIndexOfAnnualRate);
        _result.setAnnualRate(_tmpAnnualRate);
        final int _tmpMonths;
        _tmpMonths = _cursor.getInt(_cursorIndexOfMonths);
        _result.setMonths(_tmpMonths);
        final String _tmpStartDate;
        if (_cursor.isNull(_cursorIndexOfStartDate)) {
          _tmpStartDate = null;
        } else {
          _tmpStartDate = _cursor.getString(_cursorIndexOfStartDate);
        }
        _result.setStartDate(_tmpStartDate);
        final double _tmpCreditLimit;
        _tmpCreditLimit = _cursor.getDouble(_cursorIndexOfCreditLimit);
        _result.setCreditLimit(_tmpCreditLimit);
        final int _tmpDueDate;
        _tmpDueDate = _cursor.getInt(_cursorIndexOfDueDate);
        _result.setDueDate(_tmpDueDate);
        final double _tmpOriginalMonthlyPayment;
        _tmpOriginalMonthlyPayment = _cursor.getDouble(_cursorIndexOfOriginalMonthlyPayment);
        _result.setOriginalMonthlyPayment(_tmpOriginalMonthlyPayment);
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
  public LiveData<Loan> getLoanByIdLive(final int loanId) {
    final String _sql = "SELECT * FROM loans WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, loanId);
    return __db.getInvalidationTracker().createLiveData(new String[] {"loans"}, false, new Callable<Loan>() {
      @Override
      @Nullable
      public Loan call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfLoanType = CursorUtil.getColumnIndexOrThrow(_cursor, "loanType");
          final int _cursorIndexOfRepaymentMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "repaymentMethod");
          final int _cursorIndexOfPrincipal = CursorUtil.getColumnIndexOrThrow(_cursor, "principal");
          final int _cursorIndexOfAnnualRate = CursorUtil.getColumnIndexOrThrow(_cursor, "annualRate");
          final int _cursorIndexOfMonths = CursorUtil.getColumnIndexOrThrow(_cursor, "months");
          final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
          final int _cursorIndexOfCreditLimit = CursorUtil.getColumnIndexOrThrow(_cursor, "creditLimit");
          final int _cursorIndexOfDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "dueDate");
          final int _cursorIndexOfOriginalMonthlyPayment = CursorUtil.getColumnIndexOrThrow(_cursor, "originalMonthlyPayment");
          final Loan _result;
          if (_cursor.moveToFirst()) {
            _result = new Loan();
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            _result.setId(_tmpId);
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            _result.setName(_tmpName);
            final String _tmpLoanType;
            if (_cursor.isNull(_cursorIndexOfLoanType)) {
              _tmpLoanType = null;
            } else {
              _tmpLoanType = _cursor.getString(_cursorIndexOfLoanType);
            }
            _result.setLoanType(_tmpLoanType);
            final String _tmpRepaymentMethod;
            if (_cursor.isNull(_cursorIndexOfRepaymentMethod)) {
              _tmpRepaymentMethod = null;
            } else {
              _tmpRepaymentMethod = _cursor.getString(_cursorIndexOfRepaymentMethod);
            }
            _result.setRepaymentMethod(_tmpRepaymentMethod);
            final double _tmpPrincipal;
            _tmpPrincipal = _cursor.getDouble(_cursorIndexOfPrincipal);
            _result.setPrincipal(_tmpPrincipal);
            final double _tmpAnnualRate;
            _tmpAnnualRate = _cursor.getDouble(_cursorIndexOfAnnualRate);
            _result.setAnnualRate(_tmpAnnualRate);
            final int _tmpMonths;
            _tmpMonths = _cursor.getInt(_cursorIndexOfMonths);
            _result.setMonths(_tmpMonths);
            final String _tmpStartDate;
            if (_cursor.isNull(_cursorIndexOfStartDate)) {
              _tmpStartDate = null;
            } else {
              _tmpStartDate = _cursor.getString(_cursorIndexOfStartDate);
            }
            _result.setStartDate(_tmpStartDate);
            final double _tmpCreditLimit;
            _tmpCreditLimit = _cursor.getDouble(_cursorIndexOfCreditLimit);
            _result.setCreditLimit(_tmpCreditLimit);
            final int _tmpDueDate;
            _tmpDueDate = _cursor.getInt(_cursorIndexOfDueDate);
            _result.setDueDate(_tmpDueDate);
            final double _tmpOriginalMonthlyPayment;
            _tmpOriginalMonthlyPayment = _cursor.getDouble(_cursorIndexOfOriginalMonthlyPayment);
            _result.setOriginalMonthlyPayment(_tmpOriginalMonthlyPayment);
          } else {
            _result = null;
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
  public LoanWithPayments getLoanWithPayments(final int loanId) {
    final String _sql = "SELECT * FROM loans WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, loanId);
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
      try {
        final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
        final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
        final int _cursorIndexOfLoanType = CursorUtil.getColumnIndexOrThrow(_cursor, "loanType");
        final int _cursorIndexOfRepaymentMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "repaymentMethod");
        final int _cursorIndexOfPrincipal = CursorUtil.getColumnIndexOrThrow(_cursor, "principal");
        final int _cursorIndexOfAnnualRate = CursorUtil.getColumnIndexOrThrow(_cursor, "annualRate");
        final int _cursorIndexOfMonths = CursorUtil.getColumnIndexOrThrow(_cursor, "months");
        final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
        final int _cursorIndexOfCreditLimit = CursorUtil.getColumnIndexOrThrow(_cursor, "creditLimit");
        final int _cursorIndexOfDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "dueDate");
        final int _cursorIndexOfOriginalMonthlyPayment = CursorUtil.getColumnIndexOrThrow(_cursor, "originalMonthlyPayment");
        final LongSparseArray<ArrayList<Payment>> _collectionPayments = new LongSparseArray<ArrayList<Payment>>();
        while (_cursor.moveToNext()) {
          final Long _tmpKey;
          if (_cursor.isNull(_cursorIndexOfId)) {
            _tmpKey = null;
          } else {
            _tmpKey = _cursor.getLong(_cursorIndexOfId);
          }
          if (_tmpKey != null) {
            if (!_collectionPayments.containsKey(_tmpKey)) {
              _collectionPayments.put(_tmpKey, new ArrayList<Payment>());
            }
          }
        }
        _cursor.moveToPosition(-1);
        __fetchRelationshippaymentsAscomFinanceLoanmanagerDataEntityPayment(_collectionPayments);
        final LoanWithPayments _result;
        if (_cursor.moveToFirst()) {
          final Loan _tmpLoan;
          if (!(_cursor.isNull(_cursorIndexOfId) && _cursor.isNull(_cursorIndexOfName) && _cursor.isNull(_cursorIndexOfLoanType) && _cursor.isNull(_cursorIndexOfRepaymentMethod) && _cursor.isNull(_cursorIndexOfPrincipal) && _cursor.isNull(_cursorIndexOfAnnualRate) && _cursor.isNull(_cursorIndexOfMonths) && _cursor.isNull(_cursorIndexOfStartDate) && _cursor.isNull(_cursorIndexOfCreditLimit) && _cursor.isNull(_cursorIndexOfDueDate) && _cursor.isNull(_cursorIndexOfOriginalMonthlyPayment))) {
            _tmpLoan = new Loan();
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            _tmpLoan.setId(_tmpId);
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            _tmpLoan.setName(_tmpName);
            final String _tmpLoanType;
            if (_cursor.isNull(_cursorIndexOfLoanType)) {
              _tmpLoanType = null;
            } else {
              _tmpLoanType = _cursor.getString(_cursorIndexOfLoanType);
            }
            _tmpLoan.setLoanType(_tmpLoanType);
            final String _tmpRepaymentMethod;
            if (_cursor.isNull(_cursorIndexOfRepaymentMethod)) {
              _tmpRepaymentMethod = null;
            } else {
              _tmpRepaymentMethod = _cursor.getString(_cursorIndexOfRepaymentMethod);
            }
            _tmpLoan.setRepaymentMethod(_tmpRepaymentMethod);
            final double _tmpPrincipal;
            _tmpPrincipal = _cursor.getDouble(_cursorIndexOfPrincipal);
            _tmpLoan.setPrincipal(_tmpPrincipal);
            final double _tmpAnnualRate;
            _tmpAnnualRate = _cursor.getDouble(_cursorIndexOfAnnualRate);
            _tmpLoan.setAnnualRate(_tmpAnnualRate);
            final int _tmpMonths;
            _tmpMonths = _cursor.getInt(_cursorIndexOfMonths);
            _tmpLoan.setMonths(_tmpMonths);
            final String _tmpStartDate;
            if (_cursor.isNull(_cursorIndexOfStartDate)) {
              _tmpStartDate = null;
            } else {
              _tmpStartDate = _cursor.getString(_cursorIndexOfStartDate);
            }
            _tmpLoan.setStartDate(_tmpStartDate);
            final double _tmpCreditLimit;
            _tmpCreditLimit = _cursor.getDouble(_cursorIndexOfCreditLimit);
            _tmpLoan.setCreditLimit(_tmpCreditLimit);
            final int _tmpDueDate;
            _tmpDueDate = _cursor.getInt(_cursorIndexOfDueDate);
            _tmpLoan.setDueDate(_tmpDueDate);
            final double _tmpOriginalMonthlyPayment;
            _tmpOriginalMonthlyPayment = _cursor.getDouble(_cursorIndexOfOriginalMonthlyPayment);
            _tmpLoan.setOriginalMonthlyPayment(_tmpOriginalMonthlyPayment);
          } else {
            _tmpLoan = null;
          }
          final ArrayList<Payment> _tmpPaymentsCollection;
          final Long _tmpKey_1;
          if (_cursor.isNull(_cursorIndexOfId)) {
            _tmpKey_1 = null;
          } else {
            _tmpKey_1 = _cursor.getLong(_cursorIndexOfId);
          }
          if (_tmpKey_1 != null) {
            _tmpPaymentsCollection = _collectionPayments.get(_tmpKey_1);
          } else {
            _tmpPaymentsCollection = new ArrayList<Payment>();
          }
          _result = new LoanWithPayments();
          _result.setLoan(_tmpLoan);
          _result.setPayments(_tmpPaymentsCollection);
        } else {
          _result = null;
        }
        __db.setTransactionSuccessful();
        return _result;
      } finally {
        _cursor.close();
        _statement.release();
      }
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public List<LoanWithPayments> getAllLoansWithPayments() {
    final String _sql = "SELECT * FROM loans ORDER BY id DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
      try {
        final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
        final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
        final int _cursorIndexOfLoanType = CursorUtil.getColumnIndexOrThrow(_cursor, "loanType");
        final int _cursorIndexOfRepaymentMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "repaymentMethod");
        final int _cursorIndexOfPrincipal = CursorUtil.getColumnIndexOrThrow(_cursor, "principal");
        final int _cursorIndexOfAnnualRate = CursorUtil.getColumnIndexOrThrow(_cursor, "annualRate");
        final int _cursorIndexOfMonths = CursorUtil.getColumnIndexOrThrow(_cursor, "months");
        final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
        final int _cursorIndexOfCreditLimit = CursorUtil.getColumnIndexOrThrow(_cursor, "creditLimit");
        final int _cursorIndexOfDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "dueDate");
        final int _cursorIndexOfOriginalMonthlyPayment = CursorUtil.getColumnIndexOrThrow(_cursor, "originalMonthlyPayment");
        final LongSparseArray<ArrayList<Payment>> _collectionPayments = new LongSparseArray<ArrayList<Payment>>();
        while (_cursor.moveToNext()) {
          final Long _tmpKey;
          if (_cursor.isNull(_cursorIndexOfId)) {
            _tmpKey = null;
          } else {
            _tmpKey = _cursor.getLong(_cursorIndexOfId);
          }
          if (_tmpKey != null) {
            if (!_collectionPayments.containsKey(_tmpKey)) {
              _collectionPayments.put(_tmpKey, new ArrayList<Payment>());
            }
          }
        }
        _cursor.moveToPosition(-1);
        __fetchRelationshippaymentsAscomFinanceLoanmanagerDataEntityPayment(_collectionPayments);
        final List<LoanWithPayments> _result = new ArrayList<LoanWithPayments>(_cursor.getCount());
        while (_cursor.moveToNext()) {
          final LoanWithPayments _item;
          final Loan _tmpLoan;
          if (!(_cursor.isNull(_cursorIndexOfId) && _cursor.isNull(_cursorIndexOfName) && _cursor.isNull(_cursorIndexOfLoanType) && _cursor.isNull(_cursorIndexOfRepaymentMethod) && _cursor.isNull(_cursorIndexOfPrincipal) && _cursor.isNull(_cursorIndexOfAnnualRate) && _cursor.isNull(_cursorIndexOfMonths) && _cursor.isNull(_cursorIndexOfStartDate) && _cursor.isNull(_cursorIndexOfCreditLimit) && _cursor.isNull(_cursorIndexOfDueDate) && _cursor.isNull(_cursorIndexOfOriginalMonthlyPayment))) {
            _tmpLoan = new Loan();
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            _tmpLoan.setId(_tmpId);
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            _tmpLoan.setName(_tmpName);
            final String _tmpLoanType;
            if (_cursor.isNull(_cursorIndexOfLoanType)) {
              _tmpLoanType = null;
            } else {
              _tmpLoanType = _cursor.getString(_cursorIndexOfLoanType);
            }
            _tmpLoan.setLoanType(_tmpLoanType);
            final String _tmpRepaymentMethod;
            if (_cursor.isNull(_cursorIndexOfRepaymentMethod)) {
              _tmpRepaymentMethod = null;
            } else {
              _tmpRepaymentMethod = _cursor.getString(_cursorIndexOfRepaymentMethod);
            }
            _tmpLoan.setRepaymentMethod(_tmpRepaymentMethod);
            final double _tmpPrincipal;
            _tmpPrincipal = _cursor.getDouble(_cursorIndexOfPrincipal);
            _tmpLoan.setPrincipal(_tmpPrincipal);
            final double _tmpAnnualRate;
            _tmpAnnualRate = _cursor.getDouble(_cursorIndexOfAnnualRate);
            _tmpLoan.setAnnualRate(_tmpAnnualRate);
            final int _tmpMonths;
            _tmpMonths = _cursor.getInt(_cursorIndexOfMonths);
            _tmpLoan.setMonths(_tmpMonths);
            final String _tmpStartDate;
            if (_cursor.isNull(_cursorIndexOfStartDate)) {
              _tmpStartDate = null;
            } else {
              _tmpStartDate = _cursor.getString(_cursorIndexOfStartDate);
            }
            _tmpLoan.setStartDate(_tmpStartDate);
            final double _tmpCreditLimit;
            _tmpCreditLimit = _cursor.getDouble(_cursorIndexOfCreditLimit);
            _tmpLoan.setCreditLimit(_tmpCreditLimit);
            final int _tmpDueDate;
            _tmpDueDate = _cursor.getInt(_cursorIndexOfDueDate);
            _tmpLoan.setDueDate(_tmpDueDate);
            final double _tmpOriginalMonthlyPayment;
            _tmpOriginalMonthlyPayment = _cursor.getDouble(_cursorIndexOfOriginalMonthlyPayment);
            _tmpLoan.setOriginalMonthlyPayment(_tmpOriginalMonthlyPayment);
          } else {
            _tmpLoan = null;
          }
          final ArrayList<Payment> _tmpPaymentsCollection;
          final Long _tmpKey_1;
          if (_cursor.isNull(_cursorIndexOfId)) {
            _tmpKey_1 = null;
          } else {
            _tmpKey_1 = _cursor.getLong(_cursorIndexOfId);
          }
          if (_tmpKey_1 != null) {
            _tmpPaymentsCollection = _collectionPayments.get(_tmpKey_1);
          } else {
            _tmpPaymentsCollection = new ArrayList<Payment>();
          }
          _item = new LoanWithPayments();
          _item.setLoan(_tmpLoan);
          _item.setPayments(_tmpPaymentsCollection);
          _result.add(_item);
        }
        __db.setTransactionSuccessful();
        return _result;
      } finally {
        _cursor.close();
        _statement.release();
      }
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public List<Loan> getCreditCardLoans() {
    final String _sql = "SELECT * FROM loans WHERE loanType = 'credit_card'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
      final int _cursorIndexOfLoanType = CursorUtil.getColumnIndexOrThrow(_cursor, "loanType");
      final int _cursorIndexOfRepaymentMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "repaymentMethod");
      final int _cursorIndexOfPrincipal = CursorUtil.getColumnIndexOrThrow(_cursor, "principal");
      final int _cursorIndexOfAnnualRate = CursorUtil.getColumnIndexOrThrow(_cursor, "annualRate");
      final int _cursorIndexOfMonths = CursorUtil.getColumnIndexOrThrow(_cursor, "months");
      final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
      final int _cursorIndexOfCreditLimit = CursorUtil.getColumnIndexOrThrow(_cursor, "creditLimit");
      final int _cursorIndexOfDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "dueDate");
      final int _cursorIndexOfOriginalMonthlyPayment = CursorUtil.getColumnIndexOrThrow(_cursor, "originalMonthlyPayment");
      final List<Loan> _result = new ArrayList<Loan>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final Loan _item;
        _item = new Loan();
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        _item.setId(_tmpId);
        final String _tmpName;
        if (_cursor.isNull(_cursorIndexOfName)) {
          _tmpName = null;
        } else {
          _tmpName = _cursor.getString(_cursorIndexOfName);
        }
        _item.setName(_tmpName);
        final String _tmpLoanType;
        if (_cursor.isNull(_cursorIndexOfLoanType)) {
          _tmpLoanType = null;
        } else {
          _tmpLoanType = _cursor.getString(_cursorIndexOfLoanType);
        }
        _item.setLoanType(_tmpLoanType);
        final String _tmpRepaymentMethod;
        if (_cursor.isNull(_cursorIndexOfRepaymentMethod)) {
          _tmpRepaymentMethod = null;
        } else {
          _tmpRepaymentMethod = _cursor.getString(_cursorIndexOfRepaymentMethod);
        }
        _item.setRepaymentMethod(_tmpRepaymentMethod);
        final double _tmpPrincipal;
        _tmpPrincipal = _cursor.getDouble(_cursorIndexOfPrincipal);
        _item.setPrincipal(_tmpPrincipal);
        final double _tmpAnnualRate;
        _tmpAnnualRate = _cursor.getDouble(_cursorIndexOfAnnualRate);
        _item.setAnnualRate(_tmpAnnualRate);
        final int _tmpMonths;
        _tmpMonths = _cursor.getInt(_cursorIndexOfMonths);
        _item.setMonths(_tmpMonths);
        final String _tmpStartDate;
        if (_cursor.isNull(_cursorIndexOfStartDate)) {
          _tmpStartDate = null;
        } else {
          _tmpStartDate = _cursor.getString(_cursorIndexOfStartDate);
        }
        _item.setStartDate(_tmpStartDate);
        final double _tmpCreditLimit;
        _tmpCreditLimit = _cursor.getDouble(_cursorIndexOfCreditLimit);
        _item.setCreditLimit(_tmpCreditLimit);
        final int _tmpDueDate;
        _tmpDueDate = _cursor.getInt(_cursorIndexOfDueDate);
        _item.setDueDate(_tmpDueDate);
        final double _tmpOriginalMonthlyPayment;
        _tmpOriginalMonthlyPayment = _cursor.getDouble(_cursorIndexOfOriginalMonthlyPayment);
        _item.setOriginalMonthlyPayment(_tmpOriginalMonthlyPayment);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<Loan> getNormalLoans() {
    final String _sql = "SELECT * FROM loans WHERE loanType = 'normal'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
      final int _cursorIndexOfLoanType = CursorUtil.getColumnIndexOrThrow(_cursor, "loanType");
      final int _cursorIndexOfRepaymentMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "repaymentMethod");
      final int _cursorIndexOfPrincipal = CursorUtil.getColumnIndexOrThrow(_cursor, "principal");
      final int _cursorIndexOfAnnualRate = CursorUtil.getColumnIndexOrThrow(_cursor, "annualRate");
      final int _cursorIndexOfMonths = CursorUtil.getColumnIndexOrThrow(_cursor, "months");
      final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
      final int _cursorIndexOfCreditLimit = CursorUtil.getColumnIndexOrThrow(_cursor, "creditLimit");
      final int _cursorIndexOfDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "dueDate");
      final int _cursorIndexOfOriginalMonthlyPayment = CursorUtil.getColumnIndexOrThrow(_cursor, "originalMonthlyPayment");
      final List<Loan> _result = new ArrayList<Loan>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final Loan _item;
        _item = new Loan();
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        _item.setId(_tmpId);
        final String _tmpName;
        if (_cursor.isNull(_cursorIndexOfName)) {
          _tmpName = null;
        } else {
          _tmpName = _cursor.getString(_cursorIndexOfName);
        }
        _item.setName(_tmpName);
        final String _tmpLoanType;
        if (_cursor.isNull(_cursorIndexOfLoanType)) {
          _tmpLoanType = null;
        } else {
          _tmpLoanType = _cursor.getString(_cursorIndexOfLoanType);
        }
        _item.setLoanType(_tmpLoanType);
        final String _tmpRepaymentMethod;
        if (_cursor.isNull(_cursorIndexOfRepaymentMethod)) {
          _tmpRepaymentMethod = null;
        } else {
          _tmpRepaymentMethod = _cursor.getString(_cursorIndexOfRepaymentMethod);
        }
        _item.setRepaymentMethod(_tmpRepaymentMethod);
        final double _tmpPrincipal;
        _tmpPrincipal = _cursor.getDouble(_cursorIndexOfPrincipal);
        _item.setPrincipal(_tmpPrincipal);
        final double _tmpAnnualRate;
        _tmpAnnualRate = _cursor.getDouble(_cursorIndexOfAnnualRate);
        _item.setAnnualRate(_tmpAnnualRate);
        final int _tmpMonths;
        _tmpMonths = _cursor.getInt(_cursorIndexOfMonths);
        _item.setMonths(_tmpMonths);
        final String _tmpStartDate;
        if (_cursor.isNull(_cursorIndexOfStartDate)) {
          _tmpStartDate = null;
        } else {
          _tmpStartDate = _cursor.getString(_cursorIndexOfStartDate);
        }
        _item.setStartDate(_tmpStartDate);
        final double _tmpCreditLimit;
        _tmpCreditLimit = _cursor.getDouble(_cursorIndexOfCreditLimit);
        _item.setCreditLimit(_tmpCreditLimit);
        final int _tmpDueDate;
        _tmpDueDate = _cursor.getInt(_cursorIndexOfDueDate);
        _item.setDueDate(_tmpDueDate);
        final double _tmpOriginalMonthlyPayment;
        _tmpOriginalMonthlyPayment = _cursor.getDouble(_cursorIndexOfOriginalMonthlyPayment);
        _item.setOriginalMonthlyPayment(_tmpOriginalMonthlyPayment);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public int getLoanCount() {
    final String _sql = "SELECT COUNT(*) FROM loans";
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

  @Override
  public int getPaidOffCount() {
    final String _sql = "SELECT COUNT(*) FROM loans WHERE id NOT IN (SELECT DISTINCT loanId FROM payments)";
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

  private void __fetchRelationshippaymentsAscomFinanceLoanmanagerDataEntityPayment(
      @NonNull final LongSparseArray<ArrayList<Payment>> _map) {
    if (_map.isEmpty()) {
      return;
    }
    if (_map.size() > RoomDatabase.MAX_BIND_PARAMETER_CNT) {
      RelationUtil.recursiveFetchLongSparseArray(_map, true, (map) -> {
        __fetchRelationshippaymentsAscomFinanceLoanmanagerDataEntityPayment(map);
        return Unit.INSTANCE;
      });
      return;
    }
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT `id`,`loanId`,`amount`,`date`,`note` FROM `payments` WHERE `loanId` IN (");
    final int _inputSize = _map.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _stmt = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (int i = 0; i < _map.size(); i++) {
      final long _item = _map.keyAt(i);
      _stmt.bindLong(_argIndex, _item);
      _argIndex++;
    }
    final Cursor _cursor = DBUtil.query(__db, _stmt, false, null);
    try {
      final int _itemKeyIndex = CursorUtil.getColumnIndex(_cursor, "loanId");
      if (_itemKeyIndex == -1) {
        return;
      }
      final int _cursorIndexOfId = 0;
      final int _cursorIndexOfLoanId = 1;
      final int _cursorIndexOfAmount = 2;
      final int _cursorIndexOfDate = 3;
      final int _cursorIndexOfNote = 4;
      while (_cursor.moveToNext()) {
        final long _tmpKey;
        _tmpKey = _cursor.getLong(_itemKeyIndex);
        final ArrayList<Payment> _tmpRelation = _map.get(_tmpKey);
        if (_tmpRelation != null) {
          final Payment _item_1;
          _item_1 = new Payment();
          final int _tmpId;
          _tmpId = _cursor.getInt(_cursorIndexOfId);
          _item_1.setId(_tmpId);
          final int _tmpLoanId;
          _tmpLoanId = _cursor.getInt(_cursorIndexOfLoanId);
          _item_1.setLoanId(_tmpLoanId);
          final double _tmpAmount;
          _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
          _item_1.setAmount(_tmpAmount);
          final String _tmpDate;
          if (_cursor.isNull(_cursorIndexOfDate)) {
            _tmpDate = null;
          } else {
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
          }
          _item_1.setDate(_tmpDate);
          final String _tmpNote;
          if (_cursor.isNull(_cursorIndexOfNote)) {
            _tmpNote = null;
          } else {
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
          }
          _item_1.setNote(_tmpNote);
          _tmpRelation.add(_item_1);
        }
      }
    } finally {
      _cursor.close();
    }
  }
}
