/**
 * ============================================================================
 * 文件名: LoanDao.java
 * 模块:   数据访问层 (data/dao)
 * 功能:   贷款数据访问对象接口，定义贷款表的所有数据库操作
 * 
 * 主要职责:
 *   1. 定义贷款数据的 CRUD 操作（增删改查）
 *   2. 提供各种查询方式（按ID查询、按类型查询等）
 *   3. 支持关联查询（贷款与还款记录）
 *   4. 支持 LiveData 响应式查询
 * 
 * 设计模式:
 *   - DAO（Data Access Object）模式：封装数据库访问逻辑
 *   - 接口定义：Room 框架在编译时自动生成实现类
 * 
 * 使用方式:
 *   通过 AppDatabase.loanDao() 获取实例
 *   可返回同步结果或 LiveData（用于响应式UI更新）
 * ============================================================================
 */
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
 * 贷款数据访问对象接口
 * 
 * 该接口定义了所有针对 loans 表的数据库操作。
 * Room 框架在编译时会自动生成该接口的实现类。
 * 
 * @Dao 注解标识这是一个 Room DAO 接口
 * 
 * @see Loan 贷款实体类
 * @see LoanWithPayments 贷款与还款记录的关联类
 */
@Dao
public interface LoanDao {
    
    // ==================== 查询操作 ====================
    
    /**
     * 获取所有贷款（响应式）
     * 
     * 返回 LiveData 包装的贷款列表，当数据变化时自动通知观察者。
     * 适用于需要实时更新UI的场景。
     * 
     * @return 按ID降序排列的所有贷款列表（最新添加的在前）
     */
    @Query("SELECT * FROM loans ORDER BY id DESC")
    LiveData<List<Loan>> getAllLoansLive();
    
    /**
     * 获取所有贷款（同步）
     * 
     * 直接返回贷款列表，不包装为 LiveData。
     * 适用于一次性查询或后台线程操作。
     * 
     * @return 按ID降序排列的所有贷款列表
     */
    @Query("SELECT * FROM loans ORDER BY id DESC")
    List<Loan> getAllLoans();
    
    /**
     * 根据ID获取贷款
     * 
     * @param loanId 贷款ID
     * @return 贷款对象，如果不存在返回 null
     */
    @Query("SELECT * FROM loans WHERE id = :loanId")
    Loan getLoanById(int loanId);
    
    /**
     * 根据ID获取贷款（响应式）
     * 
     * 返回 LiveData 包装的单个贷款对象。
     * 当贷款信息变化时自动通知观察者。
     * 
     * @param loanId 贷款ID
     * @return LiveData 包装的贷款对象
     */
    @Query("SELECT * FROM loans WHERE id = :loanId")
    LiveData<Loan> getLoanByIdLive(int loanId);
    
    /**
     * 获取贷款及其还款记录
     * 
     * @Transaction 注解确保查询的原子性。
     * 返回 LoanWithPayments 对象，包含贷款信息和所有还款记录。
     * 
     * @param loanId 贷款ID
     * @return 贷款与还款记录的关联对象
     */
    @Transaction
    @Query("SELECT * FROM loans WHERE id = :loanId")
    LoanWithPayments getLoanWithPayments(int loanId);
    
    /**
     * 获取所有贷款及其还款记录
     * 
     * 返回所有贷款及其关联的还款记录。
     * 用于批量处理或导出数据。
     * 
     * @return 所有贷款与还款记录的关联列表
     */
    @Transaction
    @Query("SELECT * FROM loans ORDER BY id DESC")
    List<LoanWithPayments> getAllLoansWithPayments();
    
    /**
     * 获取信用卡贷款
     * 
     * 筛选贷款类型为信用卡的记录。
     * 用于信用卡相关的特殊处理（如还款日提醒）。
     * 
     * @return 所有信用卡贷款列表
     */
    @Query("SELECT * FROM loans WHERE loanType = 'credit_card'")
    List<Loan> getCreditCardLoans();
    
    /**
     * 获取普通贷款
     * 
     * 筛选贷款类型为普通贷款的记录。
     * 
     * @return 所有普通贷款列表
     */
    @Query("SELECT * FROM loans WHERE loanType = 'normal'")
    List<Loan> getNormalLoans();
    
    // ==================== 插入操作 ====================
    
    /**
     * 插入贷款
     * 
     * 将新的贷款记录插入数据库。
     * ID字段会自动生成并填充到返回值中。
     * 
     * @param loan 要插入的贷款对象
     * @return 新插入记录的行ID（即自动生成的贷款ID）
     */
    @Insert
    long insertLoan(Loan loan);
    
    // ==================== 更新操作 ====================
    
    /**
     * 更新贷款
     * 
     * 根据ID更新现有贷款记录。
     * ID字段用于定位要更新的记录。
     * 
     * @param loan 包含更新信息的贷款对象
     */
    @Update
    void updateLoan(Loan loan);
    
    // ==================== 删除操作 ====================
    
    /**
     * 删除贷款
     * 
     * 根据对象删除贷款记录。
     * 由于配置了级联删除，关联的还款记录也会被删除。
     * 
     * @param loan 要删除的贷款对象
     */
    @Delete
    void deleteLoan(Loan loan);
    
    /**
     * 根据ID删除贷款
     * 
     * 直接通过ID删除贷款记录。
     * 比先查询再删除更高效。
     * 
     * @param loanId 要删除的贷款ID
     */
    @Query("DELETE FROM loans WHERE id = :loanId")
    void deleteLoanById(int loanId);
    
    /**
     * 删除所有贷款
     * 
     * 清空 loans 表中的所有记录。
     * 谨慎使用，此操作不可逆。
     */
    @Query("DELETE FROM loans")
    void deleteAllLoans();
    
    // ==================== 统计操作 ====================
    
    /**
     * 获取贷款数量
     * 
     * @return 贷款总数
     */
    @Query("SELECT COUNT(*) FROM loans")
    int getLoanCount();
    
    /**
     * 获取已还清的贷款数量
     * 
     * 通过查询没有关联还款记录的贷款来计算已还清数量。
     * 注意：此逻辑假设没有还款记录的贷款已被视为还清，
     * 实际业务逻辑可能需要调整。
     * 
     * @return 已还清的贷款数量
     */
    @Query("SELECT COUNT(*) FROM loans WHERE id NOT IN (SELECT DISTINCT loanId FROM payments)")
    int getPaidOffCount();
}
