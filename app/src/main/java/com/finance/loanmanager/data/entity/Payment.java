/**
 * ============================================================================
 * 文件名: Payment.java
 * 模块:   数据实体层 (data/entity)
 * 功能:   还款记录实体类，映射数据库中的 payments 表
 * 
 * 主要职责:
 *   1. 定义还款记录的数据结构
 *   2. 与 Loan 实体建立外键关联
 *   3. 记录每次还款的详细信息
 * 
 * 数据库映射:
 *   - 使用 Room 持久化库的 @Entity 注解
 *   - 表名: payments
 *   - 主键: id (自增)
 *   - 外键: loanId 关联 loans 表
 * 
 * 关联关系:
 *   - 多对一：多个 Payment 记录属于一个 Loan
 *   - 级联删除：当 Loan 被删除时，关联的 Payment 记录自动删除
 * ============================================================================
 */
package com.finance.loanmanager.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/**
 * 还款记录实体类
 * 
 * 该类表示一笔具体的还款记录，包含还款金额、日期和关联的贷款ID。
 * 通过外键与 Loan 表建立关联，支持级联删除。
 * 
 * 外键关系说明：
 *   - 每条还款记录必须关联一个存在的贷款
 *   - 删除贷款时，其所有还款记录自动删除（CASCADE）
 * 
 * @see Loan 关联的贷款实体
 * @see LoanWithPayments 包含贷款和还款记录的关联类
 */
@Entity(
    tableName = "payments",
    foreignKeys = @ForeignKey(
        entity = Loan.class,           // 关联的实体类
        parentColumns = "id",          // 父表的主键列
        childColumns = "loanId",       // 子表的外键列
        onDelete = ForeignKey.CASCADE  // 级联删除：删除贷款时自动删除还款记录
    ),
    indices = {@Index("loanId")}       // 为外键创建索引，提高查询效率
)
public class Payment {
    
    // ==================== 主键字段 ====================
    
    /**
     * 还款记录唯一标识符
     * 由数据库自动生成
     */
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    // ==================== 外键字段 ====================
    
    /**
     * 关联的贷款ID
     * 外键，指向 loans 表的 id 字段
     */
    private int loanId;
    
    // ==================== 还款信息 ====================
    
    /**
     * 还款金额
     * 本次还款的实际金额
     */
    private double amount;
    
    /**
     * 还款日期
     * 格式: yyyy-MM-dd，如 "2026-01-15"
     */
    @NonNull
    private String date;
    
    /**
     * 备注
     * 可选的还款备注信息，如"提前还款"、"部分还款"等
     */
    private String note;
    
    // ==================== 构造方法 ====================
    
    /**
     * 默认构造方法（Room 要求）
     * Room 框架要求实体类必须有一个空构造方法
     */
    public Payment() {
    }
    
    /**
     * 简化构造方法
     * 
     * @param loanId 关联的贷款ID
     * @param amount 还款金额
     * @param date 还款日期
     */
    @Ignore
    public Payment(int loanId, double amount, @NonNull String date) {
        this.loanId = loanId;
        this.amount = amount;
        this.date = date;
    }
    
    /**
     * 完整构造方法
     * 
     * @param loanId 关联的贷款ID
     * @param amount 还款金额
     * @param date 还款日期
     * @param note 备注信息
     */
    @Ignore
    public Payment(int loanId, double amount, @NonNull String date, String note) {
        this.loanId = loanId;
        this.amount = amount;
        this.date = date;
        this.note = note;
    }
    
    // ==================== Getter 和 Setter 方法 ====================
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLoanId() {
        return loanId;
    }

    public void setLoanId(int loanId) {
        this.loanId = loanId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    @NonNull
    public String getDate() {
        return date;
    }

    public void setDate(@NonNull String date) {
        this.date = date;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
