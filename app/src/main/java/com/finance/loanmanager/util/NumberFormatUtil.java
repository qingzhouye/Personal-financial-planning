/**
 * ============================================================================
 * 文件名: NumberFormatUtil.java
 * 模块:   工具类层 (util)
 * 功能:   数字格式化工具类，提供金额和数字的格式化显示
 * 
 * 主要职责:
 *   1. 格式化金额显示（带货币符号）
 *   2. 格式化数字显示（千分位分隔）
 *   3. 格式化百分比显示
 *   4. 安全的字符串解析功能
 * 
 * 格式化规则:
 *   - 货币格式: ¥1,234.56（带人民币符号，千分位，两位小数）
 *   - 数字格式: 1,234.56（不带符号，千分位，两位小数）
 *   - 百分比格式: 12.3%
 *   - 整数格式: 1,234（千分位，无小数）
 * 
 * 使用场景:
 *   - 显示贷款本金、余额等金额
 *   - 显示利率百分比
 *   - 显示期数等整数
 *   - 解析用户输入的数字
 * 
 * 线程安全:
 *   该类是纯静态工具类，使用线程安全的 NumberFormat 实例
 * ============================================================================
 */
package com.finance.loanmanager.util;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * 数字格式化工具类
 * 
 * 该类提供各种数字格式化方法，使用中国区域设置进行格式化。
 * 所有方法都是静态的，可直接调用，无需创建实例。
 * 
 * 内部使用 NumberFormat 实例，配置为：
 *   - 使用中国区域设置 (Locale.CHINA)
 *   - 保留2位小数
 * 
 * 使用示例:
 *   String money = NumberFormatUtil.formatCurrency(123456.78);  // ¥123,456.78
 *   String num = NumberFormatUtil.formatNumber(123456.78);       // 123,456.78
 *   String pct = NumberFormatUtil.formatPercent(12.5);           // 12.5%
 */
public class NumberFormatUtil {
    
    // ==================== 格式化器实例 ====================
    
    /** 货币格式化器（带 ¥ 符号） */
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.CHINA);
    
    /** 数字格式化器（不带符号） */
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.CHINA);
    
    // ==================== 静态初始化块 ====================
    
    /**
     * 静态初始化块
     * 配置格式化器的小数位数为2位
     */
    static {
        CURRENCY_FORMAT.setMaximumFractionDigits(2);
        CURRENCY_FORMAT.setMinimumFractionDigits(2);
        NUMBER_FORMAT.setMaximumFractionDigits(2);
        NUMBER_FORMAT.setMinimumFractionDigits(2);
    }
    
    // ==================== 格式化方法 ====================
    
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
        if (str == null || str.trim().isEmpty()) {
            return 0;
        }
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
        if (str == null || str.trim().isEmpty()) {
            return 0;
        }
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
