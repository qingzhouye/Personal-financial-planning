/**
 * ============================================================================
 * 文件名: Loan.java
 * 模块:   数据实体层 (data/entity)
 * 功能:   贷款信息实体类，映射数据库中的 loans 表
 * 
 * 主要职责:
 *   1. 定义贷款信息的数据结构
 *   2. 支持多种贷款类型（普通贷款、信用卡、国家助学贷款）
 *   3. 支持多种还款方式（等额本息、等额本金、先息后本、利随本清）
 *   4. 提供贷款相关的业务方法（类型判断、名称获取等）
 * 
 * 数据库映射:
 *   - 使用 Room 持久化库的 @Entity 注解
 *   - 表名: loans
 *   - 主键: id (自增)
 * 
 * 贷款类型说明:
 *   - normal: 普通贷款（如房贷、车贷、消费贷等）
 *   - credit_card: 信用卡分期
 *   - student_loan: 国家助学贷款（特殊的还款规则）
 * 
 * 还款方式说明:
 *   - equal_interest: 等额本息 - 每月还款额固定
 *   - equal_principal: 等额本金 - 每月本金固定，利息递减
 *   - interest_first: 先息后本 - 前期只还利息，末期还本金
 *   - lump_sum: 利随本清 - 到期一次性还本付息
 * ============================================================================
 */
package com.finance.loanmanager.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/**
 * 贷款实体类
 * 
 * 该类表示一笔贷款的完整信息，包括贷款基本信息、还款方式和状态等。
 * 使用 Room 库的注解将此类映射到数据库表。
 * 
 * 支持三种贷款类型：
 *   1. 普通贷款 (normal) - 常规的分期贷款
 *   2. 信用卡 (credit_card) - 信用卡分期付款
 *   3. 国家助学贷款 (student_loan) - 具有特殊还款规则的贷款类型
 * 
 * @see Payment 关联的还款记录实体
 * @see LoanStatus 计算后的贷款状态
 */
@Entity(tableName = "loans")
public class Loan {
    
    // ==================== 主键字段 ====================
    
    /**
     * 贷款唯一标识符
     * 由数据库自动生成，用于关联还款记录
     */
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    // ==================== 基本信息 ====================
    
    /**
     * 贷款名称
     * 用户自定义的贷款名称，如"房贷"、"车贷"等
     */
    @NonNull
    private String name;
    
    /**
     * 贷款类型
     * 可选值: normal(普通贷款), credit_card(信用卡), student_loan(国家助学贷款)
     * 默认值: normal
     */
    @NonNull
    private String loanType;
    
    /**
     * 还款方式
     * 可选值: 
     *   - equal_interest: 等额本息
     *   - equal_principal: 等额本金
     *   - interest_first: 先息后本
     *   - lump_sum: 利随本清
     * 默认值: equal_interest
     */
    @NonNull
    private String repaymentMethod;
    
    // ==================== 金额与期限 ====================
    
    /**
     * 贷款本金/分期总额
     * 对于普通贷款：表示贷款总额
     * 对于信用卡：表示分期金额
     * 对于助学贷款：表示初始余额（firstYearBalance）
     */
    private double principal;
    
    /**
     * 年利率(%)
     * 例如：5.6 表示年利率为5.6%
     * 对于信用卡，默认为18%
     */
    private double annualRate;
    
    /**
     * 贷款期限(月)
     * 例如：360 表示30年房贷
     */
    private int months;
    
    /**
     * 开始日期
     * 格式: yyyy-MM-dd，如 "2026-01-15"
     */
    @NonNull
    private String startDate;
    
    // ==================== 信用卡特有字段 ====================
    
    /**
     * 信用卡额度
     * 仅当 loanType 为 credit_card 时有效
     */
    private double creditLimit;
    
    /**
     * 还款日(每月几号)
     * 仅当 loanType 为 credit_card 时有效
     * 例如：15 表示每月15号为还款日
     */
    private int dueDate;
    
    // ==================== 国家助学贷款特有字段 ====================
    
    /**
     * 每年固定还款金额
     * 仅当 loanType 为 student_loan 时有效
     * 国家助学贷款采用固定年还款方式
     */
    private double yearlyPayment;
    
    /**
     * 第一年还款后余额（初始本金）
     * 仅当 loanType 为 student_loan 时有效
     * 用于计算剩余还款年限
     */
    private double firstYearBalance;
    
    // ==================== 计算字段 ====================
    
    /**
     * 原始月供
     * 创建贷款时计算的初始月供金额
     * 用于后续比较和参考
     */
    private double originalMonthlyPayment;
    
    // ==================== 构造方法 ====================
    
    /**
     * 默认构造方法（Room 要求）
     * 
     * Room 框架要求实体类必须有一个空构造方法。
     * 在此初始化所有字段为合理的默认值，避免空指针异常。
     */
    public Loan() {
        // 设置默认值，避免空指针
        this.name = "";
        this.loanType = "normal";
        this.repaymentMethod = "equal_interest";
        this.startDate = "2026-01-01";
    }
    
    /**
     * 便捷构造方法（不用于 Room）
     * 
     * 用于在代码中快速创建 Loan 对象，Room 不会使用此构造方法。
     * 
     * @param name 贷款名称
     * @param loanType 贷款类型
     * @param repaymentMethod 还款方式
     * @param principal 本金
     * @param annualRate 年利率(%)
     * @param months 期限(月)
     * @param startDate 开始日期
     */
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
    
    // ==================== Getter 和 Setter 方法 ====================
    // 以下为所有字段的访问器方法，Room 框架需要这些方法进行数据绑定
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
    
    // ==================== 业务方法 ====================
    
    /**
     * 判断是否为信用卡
     * 
     * @return 如果贷款类型为信用卡返回 true，否则返回 false
     */
    public boolean isCreditCard() {
        return "credit_card".equals(loanType);
    }
    
    /**
     * 判断是否为国家助学贷款
     * 
     * 国家助学贷款具有特殊的还款规则：
     *   - 每年固定还款金额
     *   - 每年12月20日还款
     *   - 利息按年计算
     * 
     * @return 如果贷款类型为国家助学贷款返回 true，否则返回 false
     */
    public boolean isStudentLoan() {
        return "student_loan".equals(loanType);
    }
    
    /**
     * 获取还款方式的中文名称
     * 
     * 将英文标识转换为用户友好的中文名称。
     * 国家助学贷款有特殊的名称处理。
     * 
     * @return 还款方式的中文名称
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
     * 
     * 计算公式：还款年限 = 初始本金 / 每年固定还款金额
     * 使用向上取整确保不会低估还款年限。
     * 
     * @return 还款年限（年），如果不是国家助学贷款或参数无效返回 0
     */
    public int getStudentLoanYears() {
        if (!isStudentLoan() || yearlyPayment <= 0) {
            return 0;
        }
        return (int) Math.ceil(firstYearBalance / yearlyPayment);
    }
    
    /**
     * 获取国家助学贷款当年应还总额
     * 
     * 计算公式：当年应还 = 固定本金还款 + 当年利息
     * 当年利息 = 当前余额 × 年利率
     * 
     * @param currentBalance 当前余额（剩余本金）
     * @return 当年应还总额，如果不是国家助学贷款返回 0
     */
    public double getStudentLoanYearTotalPayment(double currentBalance) {
        if (!isStudentLoan()) {
            return 0;
        }
        // 计算当年利息
        double yearInterest = currentBalance * (annualRate / 100);
        // 当年本金还款（不超过当前余额）
        double principalPayment = Math.min(yearlyPayment, currentBalance);
        return principalPayment + yearInterest;
    }
    
    /**
     * 获取贷款类型的中文名称
     * 
     * @return 贷款类型的中文名称
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
