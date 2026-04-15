/**
 * ============================================================================
 * 文件名: LoanCalculator.java
 * 模块:   业务服务层 (service)
 * 功能:   贷款计算工具类，提供各种贷款计算功能
 * 
 * 主要职责:
 *   1. 计算月供金额（支持多种还款方式）
 *   2. 计算贷款剩余状态
 *   3. 生成还款计划表
 *   4. 处理特殊贷款类型（信用卡、国家助学贷款）
 * 
 * 还款方式支持:
 *   - 等额本息 (equal_interest): 每月还款额固定
 *   - 等额本金 (equal_principal): 每月本金固定，利息递减
 *   - 先息后本 (interest_first): 前期只还利息，末期还本金
 *   - 利随本清 (lump_sum): 到期一次性还本付息
 * 
 * 性能优化:
 *   - 使用计算缓存避免重复计算相同参数的结果
 * 
 * 使用场景:
 *   - 新建贷款时计算原始月供
 *   - 显示贷款详情时计算当前状态
 *   - 查看还款计划时生成计划表
 * ============================================================================
 */
package com.finance.loanmanager.service;

import com.finance.loanmanager.data.entity.Loan;
import com.finance.loanmanager.data.entity.LoanStatus;
import com.finance.loanmanager.data.entity.Payment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 贷款计算工具类
 * 
 * 该类是一个纯静态工具类，提供各种贷款相关的计算功能。
 * 所有方法都是静态的，无需创建实例即可调用。
 * 
 * 计算公式参考：
 *   - 等额本息月供 = 本金 × [月利率 × (1+月利率)^期数] / [(1+月利率)^期数 - 1]
 *   - 等额本金首月 = (本金/期数) + 本金×月利率
 *   - 先息后本月供 = 本金 × 月利率（末月还本金）
 *   - 利随本清总额 = 本金 + 本金 × 月利率 × 期数
 * 
 * @see LoanStatus 贷款状态结果类
 * @see PaymentScheduleItem 还款计划项
 */
public class LoanCalculator {
    
    // ==================== 缓存机制 ====================
    
    /**
     * 计算结果缓存
     * 键：格式化的参数字符串
     * 值：计算得到的月供金额
     */
    private static final java.util.Map<String, Double> calculationCache = new java.util.HashMap<>();
    
    /**
     * 生成缓存键
     * 
     * 将计算参数组合成唯一字符串作为缓存键。
     * 
     * @param principal 本金
     * @param annualRate 年利率
     * @param months 期数
     * @param method 还款方式
     * @return 缓存键字符串
     */
    private static String getCacheKey(double principal, double annualRate, int months, String method) {
        return String.format("%.2f_%.2f_%d_%s", principal, annualRate, months, method);
    }
    
    /**
     * 计算每月还款金额
     * 
     * @param principal 本金
     * @param annualRate 年利率(%)
     * @param months 期数(月)
     * @param method 还款方式
     * @return 月供金额
     */
    public static double calculateMonthlyPayment(double principal, double annualRate, 
                                                  int months, String method) {
        // 检查缓存
        String cacheKey = getCacheKey(principal, annualRate, months, method);
        if (calculationCache.containsKey(cacheKey)) {
            return calculationCache.get(cacheKey);
        }
        
        double result;
        
        if (annualRate == 0) {
            result = principal / months;
            calculationCache.put(cacheKey, result);
            return result;
        }
        
        double monthlyRate = annualRate / 12 / 100;
        
        switch (method) {
            case "equal_interest":
                // 等额本息
                if (monthlyRate == 0) {
                    result = principal / months;
                } else {
                    double monthlyPayment = principal * (monthlyRate * Math.pow(1 + monthlyRate, months))
                            / (Math.pow(1 + monthlyRate, months) - 1);
                    result = Math.round(monthlyPayment * 100) / 100.0;
                }
                break;
                
            case "equal_principal":
                // 等额本金：首月还款 = (本金/期数) + 本金×月利率
                double basePrincipal = principal / months;
                double firstMonthInterest = principal * monthlyRate;
                result = Math.round((basePrincipal + firstMonthInterest) * 100) / 100.0;
                break;
                
            case "interest_first":
                // 先息后本：每月只还利息
                result = Math.round(principal * monthlyRate * 100) / 100.0;
                break;
                
            case "lump_sum":
                // 利随本清：到期一次性还本付息
                double totalInterest = principal * monthlyRate * months;
                result = Math.round((principal + totalInterest) * 100) / 100.0;
                break;
                
            default:
                // 默认等额本息
                if (monthlyRate == 0) {
                    result = principal / months;
                } else {
                    double monthlyPayment = principal * (monthlyRate * Math.pow(1 + monthlyRate, months))
                            / (Math.pow(1 + monthlyRate, months) - 1);
                    result = Math.round(monthlyPayment * 100) / 100.0;
                }
        }
        
        // 缓存结果
        calculationCache.put(cacheKey, result);
        return result;
    }
    
    /**
     * 计算贷款剩余状态
     * 
     * @param loan 贷款信息
     * @param payments 还款记录列表
     * @return 贷款状态
     */
    public static LoanStatus calculateRemainingLoan(Loan loan, List<Payment> payments) {
        double totalPaid = 0;
        if (payments != null) {
            for (Payment payment : payments) {
                totalPaid += payment.getAmount();
            }
        }
        
        double remainingPrincipal = loan.getPrincipal() - totalPaid;
        String repaymentMethod = loan.getRepaymentMethod();
        
        // 国家助学贷款特殊计算
        if (loan.isStudentLoan()) {
            return calculateStudentLoanStatus(loan, payments, totalPaid);
        }
        
        // 信用卡特殊计算
        if (loan.isCreditCard()) {
            boolean isPaidOff = remainingPrincipal <= 0;
            
            if (isPaidOff) {
                return new LoanStatus(0, 0, 0, totalPaid, true, repaymentMethod);
            }
            
            double newMonthlyPayment;
            int remainingMonths;
            
            switch (repaymentMethod) {
                case "lump_sum":
                    newMonthlyPayment = Math.round(remainingPrincipal * 100) / 100.0;
                    remainingMonths = 1;
                    break;
                case "equal_interest":
                case "equal_principal":
                    int months = loan.getMonths() > 0 ? loan.getMonths() : 12;
                    newMonthlyPayment = calculateMonthlyPayment(
                            remainingPrincipal, loan.getAnnualRate(), months, repaymentMethod);
                    remainingMonths = months;
                    break;
                case "interest_first":
                    newMonthlyPayment = calculateMonthlyPayment(
                            remainingPrincipal, loan.getAnnualRate(), 12, "interest_first");
                    remainingMonths = 12;
                    break;
                default:
                    newMonthlyPayment = Math.round(remainingPrincipal * 100) / 100.0;
                    remainingMonths = 1;
            }
            
            return new LoanStatus(
                    Math.max(0, Math.round(remainingPrincipal * 100) / 100.0),
                    remainingMonths,
                    newMonthlyPayment,
                    totalPaid,
                    false,
                    repaymentMethod
            );
        }
        
        // 普通贷款计算
        Calendar startCal = parseDate(loan.getStartDate());
        Calendar todayCal = Calendar.getInstance();
        
        int monthsPassed = (todayCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)) * 12
                + (todayCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH));
        
        int remainingMonths = Math.max(0, loan.getMonths() - monthsPassed);
        
        if (remainingMonths == 0 || remainingPrincipal <= 0) {
            return new LoanStatus(
                    Math.max(0, remainingPrincipal),
                    0,
                    0,
                    totalPaid,
                    remainingPrincipal <= 0,
                    repaymentMethod
            );
        }
        
        double newMonthlyPayment = calculateMonthlyPayment(
                remainingPrincipal,
                loan.getAnnualRate(),
                remainingMonths,
                repaymentMethod
        );
        
        return new LoanStatus(
                Math.round(remainingPrincipal * 100) / 100.0,
                remainingMonths,
                newMonthlyPayment,
                totalPaid,
                false,
                repaymentMethod
        );
    }
    
    /**
     * 获取还款计划表
     * 
     * @param loan 贷款信息
     * @param payments 已还款记录
     * @return 还款计划列表
     */
    public static List<PaymentScheduleItem> getPaymentSchedule(Loan loan, List<Payment> payments) {
        List<PaymentScheduleItem> schedule = new ArrayList<>();
        
        double totalPaid = 0;
        if (payments != null) {
            for (Payment payment : payments) {
                totalPaid += payment.getAmount();
            }
        }
        
        double remainingPrincipal = loan.getPrincipal() - totalPaid;
        double monthlyRate = loan.getAnnualRate() / 12 / 100;
        String repaymentMethod = loan.getRepaymentMethod();
        boolean isCreditCard = loan.isCreditCard();
        
        Calendar startCal = parseDate(loan.getStartDate());
        Calendar todayCal = Calendar.getInstance();
        
        int monthsPassed = (todayCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)) * 12
                + (todayCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH));
        
        int remainingMonths;
        if (isCreditCard) {
            remainingMonths = loan.getMonths();
        } else {
            remainingMonths = Math.max(0, loan.getMonths() - monthsPassed);
        }
        
        double currentBalance = remainingPrincipal;
        double originalPrincipal = loan.getPrincipal();
        
        if (remainingMonths > 0 && currentBalance > 0) {
            double basePrincipal = originalPrincipal / loan.getMonths();
            
            for (int month = 1; month <= remainingMonths; month++) {
                if (currentBalance <= 0) break;
                
                double monthlyPayment, principalPayment, interest;
                
                switch (repaymentMethod) {
                    case "equal_interest":
                        monthlyPayment = calculateMonthlyPayment(
                                currentBalance, loan.getAnnualRate(), 
                                remainingMonths - month + 1, "equal_interest");
                        interest = currentBalance * monthlyRate;
                        principalPayment = monthlyPayment - interest;
                        break;
                        
                    case "equal_principal":
                        interest = currentBalance * monthlyRate;
                        principalPayment = basePrincipal;
                        monthlyPayment = principalPayment + interest;
                        break;
                        
                    case "interest_first":
                        interest = currentBalance * monthlyRate;
                        if (month == remainingMonths) {
                            principalPayment = currentBalance;
                            monthlyPayment = principalPayment + interest;
                        } else {
                            principalPayment = 0;
                            monthlyPayment = interest;
                        }
                        break;
                        
                    case "lump_sum":
                        if (isCreditCard) {
                            interest = currentBalance * monthlyRate;
                            if (month == remainingMonths) {
                                principalPayment = currentBalance;
                                monthlyPayment = principalPayment + interest;
                            } else {
                                principalPayment = 0;
                                monthlyPayment = interest;
                            }
                        } else {
                            if (month == remainingMonths) {
                                interest = currentBalance * monthlyRate * remainingMonths;
                                principalPayment = currentBalance;
                                monthlyPayment = principalPayment + interest;
                            } else {
                                interest = 0;
                                principalPayment = 0;
                                monthlyPayment = 0;
                            }
                        }
                        break;
                        
                    default:
                        monthlyPayment = calculateMonthlyPayment(
                                currentBalance, loan.getAnnualRate(), 
                                remainingMonths - month + 1, "equal_interest");
                        interest = currentBalance * monthlyRate;
                        principalPayment = monthlyPayment - interest;
                }
                
                if (principalPayment > currentBalance) {
                    principalPayment = currentBalance;
                    monthlyPayment = principalPayment + interest;
                }
                
                currentBalance -= principalPayment;
                
                // 计算还款日期
                String paymentDate = calculatePaymentDate(loan, startCal, month, isCreditCard);
                
                schedule.add(new PaymentScheduleItem(
                        month,
                        paymentDate,
                        Math.round(monthlyPayment * 100) / 100.0,
                        Math.round(principalPayment * 100) / 100.0,
                        Math.round(interest * 100) / 100.0,
                        Math.round(currentBalance * 100) / 100.0
                ));
            }
        }
        
        // 国家助学贷款使用特殊的还款计划计算
        if (loan.isStudentLoan()) {
            return getStudentLoanPaymentSchedule(loan, payments, totalPaid, remainingPrincipal);
        }
        
        return schedule;
    }
    
    /**
     * 计算国家助学贷款状态
     * 国家助学贷款特殊还款规则：
     * 1. 每年固定还款金额全部用于抵扣本金
     * 2. 利息按剩余本金 × 年利率单独计算
     * 3. 还款年限 = 本金总额 / 每年固定还款金额
     * 4. 每年还款日固定为12月20日
     */
    private static LoanStatus calculateStudentLoanStatus(Loan loan, List<Payment> payments, double totalPaid) {
        double firstYearBalance = loan.getFirstYearBalance();
        double yearlyPayment = loan.getYearlyPayment();
        double annualRate = loan.getAnnualRate();
        Calendar startCal = parseDate(loan.getStartDate());
        Calendar todayCal = Calendar.getInstance();
        
        // 计算总还款年限（本金/每年固定还款）
        int totalYears = (int) Math.ceil(firstYearBalance / yearlyPayment);
        
        // 计算从第一年开始经过了多少年
        int yearsPassed = todayCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR);
        if (todayCal.get(Calendar.MONTH) < Calendar.DECEMBER || 
            (todayCal.get(Calendar.MONTH) == Calendar.DECEMBER && todayCal.get(Calendar.DAY_OF_MONTH) < 20)) {
            yearsPassed--; // 如果还没到当年12月20日，算上一年
        }
        yearsPassed = Math.max(0, yearsPassed);
        
        // 计算当前余额：每年固定还款全部用于还本金
        double currentBalance = firstYearBalance;
        double totalInterestPaid = 0;
        for (int i = 0; i < yearsPassed && i < totalYears; i++) {
            // 当年利息（按年初余额计算）
            double yearInterest = currentBalance * (annualRate / 100);
            totalInterestPaid += yearInterest;
            
            // 固定还款全部用于抵扣本金
            currentBalance = currentBalance - yearlyPayment;
            if (currentBalance < 0) {
                currentBalance = 0;
                break;
            }
        }
        
        // 计算当年应还利息（如果还没还款）
        double currentYearInterest = currentBalance * (annualRate / 100);
        
        // 扣除已还款金额（额外还款）
        double extraPaid = totalPaid - (yearsPassed * yearlyPayment) - totalInterestPaid;
        if (extraPaid > 0) {
            currentBalance -= extraPaid;
        }
        
        boolean isPaidOff = currentBalance <= 0;
        int remainingYears = Math.max(0, totalYears - yearsPassed);
        
        // 当年应还总额 = 固定本金还款 + 当年利息
        double yearTotalPayment = yearlyPayment + currentYearInterest;
        
        return new LoanStatus(
                Math.max(0, Math.round(currentBalance * 100) / 100.0),
                remainingYears,
                Math.round(yearTotalPayment * 100) / 100.0,
                totalPaid,
                isPaidOff,
                "student_loan"
        );
    }
    
    /**
     * 获取国家助学贷款还款计划表
     * 每年12月20日还款
     * 还款规则：
     * 1. 每年固定还款金额全部用于抵扣本金
     * 2. 利息按剩余本金 × 年利率单独计算
     * 3. 每年实际还款 = 固定本金还款 + 当年利息
     */
    private static List<PaymentScheduleItem> getStudentLoanPaymentSchedule(Loan loan, List<Payment> payments, 
                                                                          double totalPaid, double remainingPrincipal) {
        List<PaymentScheduleItem> schedule = new ArrayList<>();
        
        double firstYearBalance = loan.getFirstYearBalance();
        double yearlyPayment = loan.getYearlyPayment();
        double annualRate = loan.getAnnualRate();
        Calendar startCal = parseDate(loan.getStartDate());
        int startYear = startCal.get(Calendar.YEAR);
        
        // 计算总还款年限
        int totalYears = (int) Math.ceil(firstYearBalance / yearlyPayment);
        
        double currentBalance = firstYearBalance;
        
        // 生成还款计划
        for (int year = 1; year <= totalYears; year++) {
            // 如果余额已经为0，结束循环
            if (currentBalance <= 0) {
                break;
            }
            
            // 当年利息（按年初余额计算）
            double yearInterest = currentBalance * (annualRate / 100);
            
            // 当年本金还款（固定金额，但不超余额）
            double principalPayment = Math.min(yearlyPayment, currentBalance);
            
            // 当年实际还款总额 = 本金 + 利息
            double yearTotalPayment = principalPayment + yearInterest;
            
            // 还款后余额
            currentBalance = currentBalance - principalPayment;
            
            // 确保余额不为负
            if (currentBalance < 0) {
                currentBalance = 0;
            }
            
            // 还款日期：每年12月20日
            String paymentDate = String.format("%04d-12-20", startYear + year - 1);
            
            schedule.add(new PaymentScheduleItem(
                    year,
                    paymentDate,
                    Math.round(yearTotalPayment * 100) / 100.0,
                    Math.round(principalPayment * 100) / 100.0,
                    Math.round(yearInterest * 100) / 100.0,
                    Math.round(currentBalance * 100) / 100.0
            ));
        }
        
        return schedule;
    }
    
    /**
     * 计算还款日期
     */
    private static String calculatePaymentDate(Loan loan, Calendar startCal, int month, boolean isCreditCard) {
        Calendar paymentCal = (Calendar) startCal.clone();
        paymentCal.add(Calendar.MONTH, month);
        
        int dueDay;
        if (isCreditCard && loan.getDueDate() > 0) {
            dueDay = loan.getDueDate();
        } else {
            dueDay = startCal.get(Calendar.DAY_OF_MONTH);
        }
        
        int lastDayOfMonth = paymentCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int actualDay = Math.min(dueDay, lastDayOfMonth);
        paymentCal.set(Calendar.DAY_OF_MONTH, actualDay);
        
        return String.format("%04d-%02d-%02d",
                paymentCal.get(Calendar.YEAR),
                paymentCal.get(Calendar.MONTH) + 1,
                paymentCal.get(Calendar.DAY_OF_MONTH));
    }
    
    /**
     * 解析日期字符串
     */
    private static Calendar parseDate(String dateStr) {
        Calendar cal = Calendar.getInstance();
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return cal; // 返回当前日期
        }
        try {
            String[] parts = dateStr.split("-");
            if (parts.length >= 3) {
                cal.set(Integer.parseInt(parts[0]), 
                        Integer.parseInt(parts[1]) - 1, 
                        Integer.parseInt(parts[2]));
            }
        } catch (Exception e) {
            // 使用当前日期作为默认值
        }
        return cal;
    }
    
    /**
     * 清除计算缓存
     */
    public static void clearCache() {
        calculationCache.clear();
    }
    
    /**
     * 还款计划项
     */
    public static class PaymentScheduleItem {
        public final int month;
        public final String date;
        public final double payment;
        public final double principal;
        public final double interest;
        public final double remainingBalance;
        
        public PaymentScheduleItem(int month, String date, double payment, 
                                   double principal, double interest, double remainingBalance) {
            this.month = month;
            this.date = date;
            this.payment = payment;
            this.principal = principal;
            this.interest = interest;
            this.remainingBalance = remainingBalance;
        }
    }
}
