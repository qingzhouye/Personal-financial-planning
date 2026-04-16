package com.finance.loanmanager.data.entity;

/**
 * 贷款状态类
 * 包含计算后的贷款状态信息
 */
public class LoanStatus {
    
    private double remainingPrincipal;  // 剩余本金
    private int remainingMonths;        // 剩余期数
    private double newMonthlyPayment;   // 新的月供
    private double totalPaid;           // 已还总额
    private boolean paidOff;            // 是否已还清
    private String repaymentMethod;     // 还款方式
    
    public LoanStatus() {
    }
    
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
    
    // Getters and Setters
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
    
    /**
     * 获取还款进度百分比
     */
    public double getProgressPercentage(double originalPrincipal) {
        if (originalPrincipal <= 0) return 100.0;
        return (totalPaid / originalPrincipal) * 100;
    }
    
    /**
     * 获取剩余百分比
     */
    public double getRemainingPercentage(double originalPrincipal) {
        if (originalPrincipal <= 0) return 0.0;
        return (remainingPrincipal / originalPrincipal) * 100;
    }
}
