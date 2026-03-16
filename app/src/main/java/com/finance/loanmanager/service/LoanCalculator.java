package com.finance.loanmanager.service;

import com.finance.loanmanager.data.entity.Loan;
import com.finance.loanmanager.data.entity.LoanStatus;
import com.finance.loanmanager.data.entity.Payment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 贷款计算工具类
 * 提供各种贷款计算功能
 */
public class LoanCalculator {
    
    // 缓存计算结果，避免重复计算
    private static final java.util.Map<String, Double> calculationCache = new java.util.HashMap<>();
    
    /**
     * 生成缓存键
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
        try {
            String[] parts = dateStr.split("-");
            cal.set(Integer.parseInt(parts[0]), 
                    Integer.parseInt(parts[1]) - 1, 
                    Integer.parseInt(parts[2]));
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
