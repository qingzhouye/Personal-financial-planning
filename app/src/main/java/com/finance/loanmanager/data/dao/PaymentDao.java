/**
 * ============================================================================
 * 文件名: PaymentDao.java
 * 模块:   数据访问层 (data/dao)
 * 功能:   还款记录数据访问对象接口，定义还款记录表的所有数据库操作
 * 
 * 主要职责:
 *   1. 定义还款记录的 CRUD 操作（增删改查）
 *   2. 提供按贷款ID查询的功能
 *   3. 支持聚合查询（总还款金额统计）
 *   4. 支持 LiveData 响应式查询
 * 
 * 设计模式:
 *   - DAO（Data Access Object）模式：封装数据库访问逻辑
 *   - 接口定义：Room 框架在编译时自动生成实现类
 * 
 * 使用方式:
 *   通过 AppDatabase.paymentDao() 获取实例
 *   可返回同步结果或 LiveData（用于响应式UI更新）
 * ============================================================================
 */
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
 * 还款记录数据访问对象接口
 * 
 * 该接口定义了所有针对 payments 表的数据库操作。
 * Room 框架在编译时会自动生成该接口的实现类。
 * 
 * @Dao 注解标识这是一个 Room DAO 接口
 * 
 * @see Payment 还款记录实体类
 */
@Dao
public interface PaymentDao {
    
    // ==================== 查询操作 ====================
    
    /**
     * 获取所有还款记录（响应式）
     * 
     * 返回 LiveData 包装的还款记录列表，当数据变化时自动通知观察者。
     * 适用于需要实时更新UI的场景。
     * 
     * @return 按日期降序排列的所有还款记录列表（最新还款在前）
     */
    @Query("SELECT * FROM payments ORDER BY date DESC")
    LiveData<List<Payment>> getAllPaymentsLive();
    
    /**
     * 获取所有还款记录（同步）
     * 
     * 直接返回还款记录列表，不包装为 LiveData。
     * 适用于一次性查询或后台线程操作。
     * 
     * @return 按日期降序排列的所有还款记录列表
     */
    @Query("SELECT * FROM payments ORDER BY date DESC")
    List<Payment> getAllPayments();
    
    /**
     * 根据贷款ID获取还款记录
     * 
     * 获取指定贷款的所有还款记录，用于查看还款历史。
     * 
     * @param loanId 贷款ID
     * @return 该贷款的所有还款记录，按日期降序排列
     */
    @Query("SELECT * FROM payments WHERE loanId = :loanId ORDER BY date DESC")
    List<Payment> getPaymentsByLoanId(int loanId);
    
    /**
     * 根据贷款ID获取还款记录（响应式）
     * 
     * 返回 LiveData 包装的还款记录列表。
     * 当有新还款记录添加时会自动更新。
     * 
     * @param loanId 贷款ID
     * @return LiveData 包装的还款记录列表
     */
    @Query("SELECT * FROM payments WHERE loanId = :loanId ORDER BY date DESC")
    LiveData<List<Payment>> getPaymentsByLoanIdLive(int loanId);
    
    /**
     * 根据ID获取还款记录
     * 
     * @param paymentId 还款记录ID
     * @return 还款记录对象，如果不存在返回 null
     */
    @Query("SELECT * FROM payments WHERE id = :paymentId")
    Payment getPaymentById(int paymentId);
    
    /**
     * 获取贷款的总还款金额
     * 
     * 使用 COALESCE 函数确保在没有还款记录时返回0而非null。
     * 用于计算贷款状态和还款进度。
     * 
     * @param loanId 贷款ID
     * @return 该贷款的累计还款总额
     */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE loanId = :loanId")
    double getTotalPaidByLoanId(int loanId);
    
    // ==================== 插入操作 ====================
    
    /**
     * 插入还款记录
     * 
     * 将新的还款记录插入数据库。
     * ID字段会自动生成并填充到返回值中。
     * 
     * @param payment 要插入的还款记录对象
     * @return 新插入记录的行ID（即自动生成的还款记录ID）
     */
    @Insert
    long insertPayment(Payment payment);
    
    /**
     * 批量插入还款记录
     * 
     * 用于数据导入时批量插入还款记录。
     * 比逐条插入更高效。
     * 
     * @param payments 要插入的还款记录列表
     */
    @Insert
    void insertPayments(List<Payment> payments);
    
    // ==================== 更新操作 ====================
    
    /**
     * 更新还款记录
     * 
     * 根据ID更新现有还款记录。
     * 
     * @param payment 包含更新信息的还款记录对象
     */
    @Update
    void updatePayment(Payment payment);
    
    // ==================== 删除操作 ====================
    
    /**
     * 删除还款记录
     * 
     * 根据对象删除还款记录。
     * 
     * @param payment 要删除的还款记录对象
     */
    @Delete
    void deletePayment(Payment payment);
    
    /**
     * 根据ID删除还款记录
     * 
     * 直接通过ID删除还款记录。
     * 
     * @param paymentId 要删除的还款记录ID
     */
    @Query("DELETE FROM payments WHERE id = :paymentId")
    void deletePaymentById(int paymentId);
    
    /**
     * 根据贷款ID删除还款记录
     * 
     * 删除指定贷款的所有还款记录。
     * 通常在删除贷款时调用（虽然有级联删除，但有时需要手动清理）。
     * 
     * @param loanId 贷款ID
     */
    @Query("DELETE FROM payments WHERE loanId = :loanId")
    void deletePaymentsByLoanId(int loanId);
    
    /**
     * 删除所有还款记录
     * 
     * 清空 payments 表中的所有记录。
     * 谨慎使用，此操作不可逆。
     */
    @Query("DELETE FROM payments")
    void deleteAllPayments();
    
    // ==================== 统计操作 ====================
    
    /**
     * 获取还款记录数量
     * 
     * @return 还款记录总数
     */
    @Query("SELECT COUNT(*) FROM payments")
    int getPaymentCount();
}
