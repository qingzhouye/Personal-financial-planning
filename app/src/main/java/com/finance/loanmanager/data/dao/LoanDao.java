package com.finance.loanmanager.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.finance.loanmanager.data.entity.Loan;
import com.finance.loanmanager.data.entity.LoanWithPayments;

import java.util.List;

/**
 * 贷款数据访问对象
 */
@Dao
public interface LoanDao {
    
    /**
     * 获取所有贷款
     */
    @Query("SELECT * FROM loans ORDER BY id DESC")
    LiveData<List<Loan>> getAllLoansLive();
    
    /**
     * 获取所有贷款（同步）
     */
    @Query("SELECT * FROM loans ORDER BY id DESC")
    List<Loan> getAllLoans();
    
    /**
     * 根据ID获取贷款
     */
    @Query("SELECT * FROM loans WHERE id = :loanId")
    Loan getLoanById(int loanId);
    
    /**
     * 根据ID获取贷款（LiveData）
     */
    @Query("SELECT * FROM loans WHERE id = :loanId")
    LiveData<Loan> getLoanByIdLive(int loanId);
    
    /**
     * 获取贷款及其还款记录
     */
    @Transaction
    @Query("SELECT * FROM loans WHERE id = :loanId")
    LoanWithPayments getLoanWithPayments(int loanId);
    
    /**
     * 获取所有贷款及其还款记录
     */
    @Transaction
    @Query("SELECT * FROM loans ORDER BY id DESC")
    List<LoanWithPayments> getAllLoansWithPayments();
    
    /**
     * 获取信用卡贷款
     */
    @Query("SELECT * FROM loans WHERE loanType = 'credit_card'")
    List<Loan> getCreditCardLoans();
    
    /**
     * 获取普通贷款
     */
    @Query("SELECT * FROM loans WHERE loanType = 'normal'")
    List<Loan> getNormalLoans();
    
    /**
     * 插入贷款
     */
    @Insert
    long insertLoan(Loan loan);
    
    /**
     * 更新贷款
     */
    @Update
    void updateLoan(Loan loan);
    
    /**
     * 删除贷款
     */
    @Delete
    void deleteLoan(Loan loan);
    
    /**
     * 根据ID删除贷款
     */
    @Query("DELETE FROM loans WHERE id = :loanId")
    void deleteLoanById(int loanId);
    
    /**
     * 删除所有贷款
     */
    @Query("DELETE FROM loans")
    void deleteAllLoans();
    
    /**
     * 获取贷款数量
     */
    @Query("SELECT COUNT(*) FROM loans")
    int getLoanCount();
    
    /**
     * 获取已还清的贷款数量
     */
    @Query("SELECT COUNT(*) FROM loans WHERE id NOT IN (SELECT DISTINCT loanId FROM payments)")
    int getPaidOffCount();
}
