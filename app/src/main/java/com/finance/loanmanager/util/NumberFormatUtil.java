package com.finance.loanmanager.util;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * 数字格式化工具类
 */
public class NumberFormatUtil {
    
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.CHINA);
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.CHINA);
    
    static {
        CURRENCY_FORMAT.setMaximumFractionDigits(2);
        CURRENCY_FORMAT.setMinimumFractionDigits(2);
        NUMBER_FORMAT.setMaximumFractionDigits(2);
        NUMBER_FORMAT.setMinimumFractionDigits(2);
    }
    
    /**
     * 格式化为货币格式（带¥符号）
     */
    public static String formatCurrency(double amount) {
        return CURRENCY_FORMAT.format(amount);
    }
    
    /**
     * 格式化为数字格式（不带¥符号）
     */
    public static String formatNumber(double number) {
        return NUMBER_FORMAT.format(number);
    }
    
    /**
     * 格式化为百分比
     */
    public static String formatPercent(double value) {
        return String.format(Locale.CHINA, "%.1f%%", value);
    }
    
    /**
     * 格式化为整数
     */
    public static String formatInt(double value) {
        return String.format(Locale.CHINA, "%.0f", value);
    }
    
    /**
     * 安全解析字符串为double
     */
    public static double parseDouble(String str) {
        try {
            return Double.parseDouble(str.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * 安全解析字符串为int
     */
    public static int parseInt(String str) {
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * 四舍五入到2位小数
     */
    public static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
