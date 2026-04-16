package com.finance.loanmanager.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/**
 * 贷款实体类
 * 对应数据库中的贷款表
 */
@Entity(tableName = "loans")
public class Loan {
    
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    @NonNull
    private String name;
    
    // 贷款类型: normal(普通贷款), credit_card(信用卡), student_loan(国家助学贷款)
    @NonNull
    private String loanType;
    
    // 还款方式: equal_interest(等额本息), equal_principal(等额本金), 
    // interest_first(先息后本), lump_sum(利随本清)
    @NonNull
    private String repaymentMethod;
    
    // 贷款本金/分期总额
    private double principal;
    
    // 年利率(%)
    private double annualRate;
    
    // 贷款期限(月)
    private int months;
    
    // 开始日期(格式: yyyy-MM-dd)
    @NonNull
    private String startDate;
    
    // 信用卡特有字段
    private double creditLimit;  // 信用卡额度
    private int dueDate;         // 还款日(每月几号)
    
    // 国家助学贷款特有字段
    private double yearlyPayment;     // 每年固定还款金额
    private double firstYearBalance;  // 第一年还款后余额（初始本金）
    
    // 原始月供(创建时计算)
    private double originalMonthlyPayment;
    
    public Loan() {
        // 设置默认值，避免空指针
        this.name = "";
        this.loanType = "normal";
        this.repaymentMethod = "equal_interest";
        this.startDate = "2026-01-01";
    }
    
    @Ignore
    public Loan(@NonNull String name, @NonNull String loanType, @NonNull String repaymentMethod,
                double principal, double annualRate, int months, @NonNull String startDate) {
        this.name = name;
        this.loanType = loanType;
        this.repaymentMethod = repaymentMethod;
        this.principal = principal;
        this.annualRate = annualRate;
        this.months = months;
        this.startDate = startDate;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    @NonNull
    public String getName() {
        return name;
    }
    
    public void setName(@NonNull String name) {
        this.name = name;
    }
    
    @NonNull
    public String getLoanType() {
        return loanType;
    }
    
    public void setLoanType(@NonNull String loanType) {
        this.loanType = loanType;
    }
    
    @NonNull
    public String getRepaymentMethod() {
        return repaymentMethod;
    }
    
    public void setRepaymentMethod(@NonNull String repaymentMethod) {
        this.repaymentMethod = repaymentMethod;
    }
    
    public double getPrincipal() {
        return principal;
    }
    
    public void setPrincipal(double principal) {
        this.principal = principal;
    }
    
    public double getAnnualRate() {
        return annualRate;
    }
    
    public void setAnnualRate(double annualRate) {
        this.annualRate = annualRate;
    }
    
    public int getMonths() {
        return months;
    }
    
    public void setMonths(int months) {
        this.months = months;
    }
    
    @NonNull
    public String getStartDate() {
        return startDate;
    }
    
    public void setStartDate(@NonNull String startDate) {
        this.startDate = startDate;
    }
    
    public double getCreditLimit() {
        return creditLimit;
    }
    
    public void setCreditLimit(double creditLimit) {
        this.creditLimit = creditLimit;
    }
    
    public int getDueDate() {
        return dueDate;
    }
    
    public void setDueDate(int dueDate) {
        this.dueDate = dueDate;
    }
    
    public double getOriginalMonthlyPayment() {
        return originalMonthlyPayment;
    }
    
    public void setOriginalMonthlyPayment(double originalMonthlyPayment) {
        this.originalMonthlyPayment = originalMonthlyPayment;
    }
    
    /**
     * 判断是否为信用卡
     */
    public boolean isCreditCard() {
        return "credit_card".equals(loanType);
    }
    
    /**
     * 判断是否为国家助学贷款
     */
    public boolean isStudentLoan() {
        return "student_loan".equals(loanType);
    }
    
    /**
     * 获取还款方式的中文名称
     */
    public String getRepaymentMethodName() {
        // 国家助学贷款特殊处理
        if (isStudentLoan()) {
            return "固定年还款";
        }
        switch (repaymentMethod) {
            case "equal_interest":
                return "等额本息";
            case "equal_principal":
                return "等额本金";
            case "interest_first":
                return "先息后本";
            case "lump_sum":
                return "利随本清";
            default:
                return "未知";
        }
    }
    
    /**
     * 获取国家助学贷款的还款年限
     * 还款年限 = 初始本金 / 每年固定还款金额
     */
    public int getStudentLoanYears() {
        if (!isStudentLoan() || yearlyPayment <= 0) {
            return 0;
        }
        return (int) Math.ceil(firstYearBalance / yearlyPayment);
    }
    
    /**
     * 获取国家助学贷款当年应还总额
     * 当年应还 = 固定本金还款 + 当年利息
     * @param currentBalance 当前余额
     * @return 当年应还总额
     */
    public double getStudentLoanYearTotalPayment(double currentBalance) {
        if (!isStudentLoan()) {
            return 0;
        }
        double yearInterest = currentBalance * (annualRate / 100);
        double principalPayment = Math.min(yearlyPayment, currentBalance);
        return principalPayment + yearInterest;
    }
    
    /**
     * 获取贷款类型的中文名称
     */
    public String getLoanTypeName() {
        if (isCreditCard()) {
            return "信用卡";
        } else if (isStudentLoan()) {
            return "国家助学贷款";
        }
        return "普通贷款";
    }
    
    public double getYearlyPayment() {
        return yearlyPayment;
    }
    
    public void setYearlyPayment(double yearlyPayment) {
        this.yearlyPayment = yearlyPayment;
    }
    
    public double getFirstYearBalance() {
        return firstYearBalance;
    }
    
    public void setFirstYearBalance(double firstYearBalance) {
        this.firstYearBalance = firstYearBalance;
    }
}
