package com.finance.loanmanager.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.finance.loanmanager.data.entity.Payment;

import java.util.List;

/**
 * 还款记录数据访问对象
 */
@Dao
public interface PaymentDao {
    
    /**
     * 获取所有还款记录
     */
    @Query("SELECT * FROM payments ORDER BY date DESC")
    LiveData<List<Payment>> getAllPaymentsLive();
    
    /**
     * 获取所有还款记录（同步）
     */
    @Query("SELECT * FROM payments ORDER BY date DESC")
    List<Payment> getAllPayments();
    
    /**
     * 根据贷款ID获取还款记录
     */
    @Query("SELECT * FROM payments WHERE loanId = :loanId ORDER BY date DESC")
    List<Payment> getPaymentsByLoanId(int loanId);
    
    /**
     * 根据贷款ID获取还款记录（LiveData）
     */
    @Query("SELECT * FROM payments WHERE loanId = :loanId ORDER BY date DESC")
    LiveData<List<Payment>> getPaymentsByLoanIdLive(int loanId);
    
    /**
     * 根据ID获取还款记录
     */
    @Query("SELECT * FROM payments WHERE id = :paymentId")
    Payment getPaymentById(int paymentId);
    
    /**
     * 获取贷款的总还款金额
     */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE loanId = :loanId")
    double getTotalPaidByLoanId(int loanId);
    
    /**
     * 插入还款记录
     */
    @Insert
    long insertPayment(Payment payment);
    
    /**
     * 批量插入还款记录
     */
    @Insert
    void insertPayments(List<Payment> payments);
    
    /**
     * 更新还款记录
     */
    @Update
    void updatePayment(Payment payment);
    
    /**
     * 删除还款记录
     */
    @Delete
    void deletePayment(Payment payment);
    
    /**
     * 根据ID删除还款记录
     */
    @Query("DELETE FROM payments WHERE id = :paymentId")
    void deletePaymentById(int paymentId);
    
    /**
     * 根据贷款ID删除还款记录
     */
    @Query("DELETE FROM payments WHERE loanId = :loanId")
    void deletePaymentsByLoanId(int loanId);
    
    /**
     * 删除所有还款记录
     */
    @Query("DELETE FROM payments")
    void deleteAllPayments();
    
    /**
     * 获取还款记录数量
     */
    @Query("SELECT COUNT(*) FROM payments")
    int getPaymentCount();
}
