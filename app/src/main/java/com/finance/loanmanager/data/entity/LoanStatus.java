/**
 * ============================================================================
 * 文件名: LoanStatus.java
 * 模块:   数据实体层 (data/entity)
 * 功能:   贷款状态数据类，存储计算后的贷款状态信息
 * 
 * 主要职责:
 *   1. 封装贷款的实时状态（剩余本金、剩余期数等）
 *   2. 提供还款进度计算方法
 *   3. 作为 LoanCalculator 的返回值类型
 * 
 * 计算说明:
 *   - 该类的数据由 LoanCalculator 根据贷款信息和还款记录动态计算
 *   - 不存储在数据库中，每次需要时重新计算
 *   - 包含实时状态和进度信息
 * 
 * 使用场景:
 *   - 显示贷款详情时获取当前状态
 *   - 统计报表计算
 *   - 还款提醒功能
 * ============================================================================
 */
package com.finance.loanmanager.data.entity;

/**
 * 贷款状态类
 * 
 * 该类存储贷款经过计算后的状态信息，包括剩余本金、剩余期数、
 * 新月供、已还总额等。这些数据不是静态存储的，而是根据
 * 贷款信息和还款记录动态计算的。
 * 
 * 该类不映射到数据库表，仅作为计算结果的载体。
 * 
 * @see LoanCalculator#calculateRemainingLoan 计算方法
 */
public class LoanStatus {
    
    // ==================== 状态字段 ====================
    
    /** 剩余本金 - 扣除已还款后的未偿还本金 */
    private double remainingPrincipal;
    
    /** 剩余期数 - 还需要还款的期数（月） */
    private int remainingMonths;
    
    /** 新的月供 - 基于剩余本金重新计算的月供金额 */
    private double newMonthlyPayment;
    
    /** 已还总额 - 累计已偿还的总金额 */
    private double totalPaid;
    
    /** 是否已还清 - 标记贷款是否已完全偿还 */
    private boolean paidOff;
    
    /** 还款方式 - 记录贷款的还款方式，用于显示 */
    private String repaymentMethod;
    
    // ==================== 构造方法 ====================
    
    /**
     * 默认构造方法
     */
    public LoanStatus() {
    }
    
    /**
     * 完整参数构造方法
     * 
     * @param remainingPrincipal 剩余本金
     * @param remainingMonths 剩余期数
     * @param newMonthlyPayment 新月供
     * @param totalPaid 已还总额
     * @param paidOff 是否已还清
     * @param repaymentMethod 还款方式
     */
    public LoanStatus(double remainingPrincipal, int remainingMonths, 
                      double newMonthlyPayment, double totalPaid, 
                      boolean paidOff, String repaymentMethod) {
        this.remainingPrincipal = remainingPrincipal;
        this.remainingMonths = remainingMonths;
        this.newMonthlyPayment = newMonthlyPayment;
        this.totalPaid = totalPaid;
        this.paidOff = paidOff;
        this.repaymentMethod = repaymentMethod;
    }
    
    // ==================== Getter 和 Setter 方法 ====================
    
    public double getRemainingPrincipal() {
        return remainingPrincipal;
    }

    public void setRemainingPrincipal(double remainingPrincipal) {
        this.remainingPrincipal = remainingPrincipal;
    }

    public int getRemainingMonths() {
        return remainingMonths;
    }

    public void setRemainingMonths(int remainingMonths) {
        this.remainingMonths = remainingMonths;
    }

    public double getNewMonthlyPayment() {
        return newMonthlyPayment;
    }

    public void setNewMonthlyPayment(double newMonthlyPayment) {
        this.newMonthlyPayment = newMonthlyPayment;
    }

    public double getTotalPaid() {
        return totalPaid;
    }

    public void setTotalPaid(double totalPaid) {
        this.totalPaid = totalPaid;
    }

    public boolean isPaidOff() {
        return paidOff;
    }

    public void setPaidOff(boolean paidOff) {
        this.paidOff = paidOff;
    }

    public String getRepaymentMethod() {
        return repaymentMethod;
    }

    public void setRepaymentMethod(String repaymentMethod) {
        this.repaymentMethod = repaymentMethod;
    }
    
    // ==================== 计算方法 ====================
    
    /**
     * 获取还款进度百分比
     * 
     * 计算已还款占原始本金的比例。
     * 公式：进度% = (已还总额 / 原始本金) × 100
     * 
     * @param originalPrincipal 原始本金
     * @return 还款进度百分比，如果本金为0则返回100.0
     */
    public double getProgressPercentage(double originalPrincipal) {
        if (originalPrincipal <= 0) return 100.0;
        return (totalPaid / originalPrincipal) * 100;
    }
    
    /**
     * 获取剩余百分比
     * 
     * 计算剩余本金占原始本金的比例。
     * 公式：剩余% = (剩余本金 / 原始本金) × 100
     * 
     * @param originalPrincipal 原始本金
     * @return 剩余百分比，如果本金为0则返回0.0
     */
    public double getRemainingPercentage(double originalPrincipal) {
        if (originalPrincipal <= 0) return 0.0;
        return (remainingPrincipal / originalPrincipal) * 100;
    }
}
