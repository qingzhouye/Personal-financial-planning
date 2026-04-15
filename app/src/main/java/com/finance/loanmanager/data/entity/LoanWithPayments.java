/**
 * ============================================================================
 * 文件名: LoanWithPayments.java
 * 模块:   数据实体层 (data/entity)
 * 功能:   贷款与还款记录的关联类（一对多关系）
 * 
 * 主要职责:
 *   1. 定义 Loan 与 Payment 的一对多关系
 *   2. 支持 Room 的关联查询（@Relation 注解）
 *   3. 提供聚合计算方法
 * 
 * 设计模式:
 *   - 使用 Room 的 @Embedded 和 @Relation 注解实现关联查询
 *   - 一次查询即可获取贷款及其所有还款记录
 * 
 * 使用场景:
 *   - 显示贷款详情时同时显示还款历史
 *   - 计算贷款状态时需要获取所有还款记录
 *   - 数据导出时需要完整的贷款和还款信息
 * ============================================================================
 */
package com.finance.loanmanager.data.entity;

import androidx.room.Embedded;
import androidx.room.Relation;
import java.util.List;

/**
 * 贷款及其还款记录的关系类
 * 
 * 该类用于 Room 的一对多关联查询，将一个 Loan 对象和其关联的所有 Payment 记录
 * 组合在一起。通过 @Relation 注解，Room 会自动将查询结果组装成此结构。
 * 
 * 关联关系说明：
 *   - 一个 Loan 可以有多个 Payment（一对多）
 *   - 通过 loanId 字段进行关联
 *   - Room 会自动执行两次查询并组装结果
 * 
 * @see Loan 贷款实体
 * @see Payment 还款记录实体
 */
public class LoanWithPayments {
    
    /**
     * 嵌入的贷款对象
     * 
     * @Embedded 注解将 Loan 的所有字段嵌入到此类的属性中，
     * 相当于将 Loan 对象"展开"到此类中。
     */
    @Embedded
    private Loan loan;
    
    /**
     * 关联的还款记录列表
     * 
     * @Relation 注解定义了与 Payment 的关联关系：
     *   - parentColumn: 父表（Loan）的主键字段名
     *   - entityColumn: 子表（Payment）的外键字段名
     * 
     * Room 会根据这个关系自动查询并填充所有关联的 Payment 记录
     */
    @Relation(
        parentColumn = "id",      // Loan 表的主键
        entityColumn = "loanId"   // Payment 表的外键
    )
    private List<Payment> payments;
    
    // ==================== Getter 和 Setter 方法 ====================
    
    /**
     * 获取贷款对象
     * @return 贷款实体
     */
    public Loan getLoan() {
        return loan;
    }
    
    /**
     * 设置贷款对象
     * @param loan 贷款实体
     */
    public void setLoan(Loan loan) {
        this.loan = loan;
    }
    
    /**
     * 获取还款记录列表
     * @return 还款记录列表，可能为 null 或空列表
     */
    public List<Payment> getPayments() {
        return payments;
    }
    
    /**
     * 设置还款记录列表
     * @param payments 还款记录列表
     */
    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }
    
    // ==================== 聚合计算方法 ====================
    
    /**
     * 计算该贷款的总还款金额
     * 
     * 遍历所有还款记录，累加还款金额。
     * 如果没有还款记录，返回 0。
     * 
     * @return 累计还款总额
     */
    public double getTotalPaidAmount() {
        if (payments == null || payments.isEmpty()) {
            return 0;
        }
        double total = 0;
        for (Payment payment : payments) {
            total += payment.getAmount();
        }
        return total;
    }
}
