/**
 * ============================================================================
 * 文件名: DateUtil.java
 * 模块:   工具类层 (util)
 * 功能:   日期处理工具类，提供日期格式化和计算功能
 * 
 * 主要职责:
 *   1. 获取当前日期和时间
 *   2. 日期格式转换（标准格式 ↔ 中文格式）
 *   3. 日期计算（月份差、剩余天数等）
 *   4. 日期解析和格式化
 * 
 * 日期格式说明:
 *   - 标准格式: yyyy-MM-dd (如 2026-01-15)
 *   - 中文格式: yyyy年MM月dd日 (如 2026年01月15日)
 *   - 月份格式: yyyy年MM月 (如 2026年01月)
 * ============================================================================
 */
package com.finance.loanmanager.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 日期工具类
 * 
 * 提供日期相关的常用操作方法。
 * 所有方法都是静态的，可直接调用。
 */
public class DateUtil {
    
    // ==================== 日期格式常量 ====================
    
    /** 标准日期格式: yyyy-MM-dd */
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    
    /** 中文日期格式: yyyy年MM月dd日 */
    private static final String DATE_FORMAT_CN = "yyyy年MM月dd日";
    
    /** 中文月份格式: yyyy年MM月 */
    private static final String MONTH_FORMAT = "yyyy年MM月";
    
    // ==================== 获取当前日期 ====================
    
    /**
     * 获取当前日期字符串
     * 
     * @return 格式为 yyyy-MM-dd 的当前日期字符串
     */
    public static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        return sdf.format(new Date());
    }
    
    /**
     * 获取当前月份字符串
     * 
     * @return 格式为 yyyy-MM 的当前月份字符串
     */
    public static String getCurrentMonth() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        return sdf.format(new Date());
    }
    
    // ==================== 格式转换 ====================
    
    /**
     * 格式化日期为中文格式
     * 
     * 将 yyyy-MM-dd 格式转换为 yyyy年MM月dd日 格式。
     * 
     * @param dateStr 标准格式的日期字符串
     * @return 中文格式的日期字符串，解析失败返回原字符串
     */
    public static String formatDateCN(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
            Date date = sdf.parse(dateStr);
            SimpleDateFormat sdfCN = new SimpleDateFormat(DATE_FORMAT_CN, Locale.getDefault());
            return sdfCN.format(date);
        } catch (ParseException e) {
            return dateStr;
        }
    }
    
    /**
     * 格式化月份为中文格式
     * 
     * 将 yyyy-MM 格式转换为 yyyy年MM月 格式。
     * 
     * @param monthStr 标准格式的月份字符串
     * @return 中文格式的月份字符串，解析失败返回原字符串
     */
    public static String formatMonthCN(String monthStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
            Date date = sdf.parse(monthStr);
            SimpleDateFormat sdfCN = new SimpleDateFormat(MONTH_FORMAT, Locale.getDefault());
            return sdfCN.format(date);
        } catch (ParseException e) {
            return monthStr;
        }
    }
    
    // ==================== 日期计算 ====================
    
    /**
     * 获取今天是几号
     * 
     * @return 当前日期中的天数（1-31）
     */
    public static int getTodayDay() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.DAY_OF_MONTH);
    }
    
    /**
     * 获取本月剩余天数
     * 
     * 计算从今天到本月最后一天的天数。
     * 
     * @return 本月剩余天数（包含今天）
     */
    public static int getRemainingDaysInMonth() {
        Calendar cal = Calendar.getInstance();
        int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int today = cal.get(Calendar.DAY_OF_MONTH);
        return lastDay - today + 1;
    }
    
    /**
     * 获取月份中的最后一天
     * 
     * @param year 年份
     * @param month 月份（1-12）
     * @return 该月的最后一天（28-31）
     */
    public static int getLastDayOfMonth(int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1);
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }
    
    /**
     * 计算两个日期之间的月份差
     * 
     * @param startDate 开始日期 (yyyy-MM-dd)
     * @param endDate 结束日期 (yyyy-MM-dd)
     * @return 月份差值（结束日期 - 开始日期）
     */
    public static int getMonthsBetween(String startDate, String endDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
            Date start = sdf.parse(startDate);
            Date end = sdf.parse(endDate);
            
            Calendar startCal = Calendar.getInstance();
            Calendar endCal = Calendar.getInstance();
            startCal.setTime(start);
            endCal.setTime(end);
            
            return (endCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)) * 12
                    + (endCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH));
        } catch (ParseException e) {
            return 0;
        }
    }
    
    // ==================== 日期解析和格式化 ====================
    
    /**
     * 解析日期字符串
     * 
     * 将 yyyy-MM-dd 格式的字符串解析为 Calendar 对象。
     * 
     * @param dateStr 日期字符串
     * @return Calendar 对象，解析失败返回当前日期
     */
    public static Calendar parseDate(String dateStr) {
        Calendar cal = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
            Date date = sdf.parse(dateStr);
            cal.setTime(date);
        } catch (ParseException e) {
            // 使用当前日期作为默认值
        }
        return cal;
    }
    
    /**
     * 格式化日期为字符串
     * 
     * 将 Calendar 对象格式化为 yyyy-MM-dd 字符串。
     * 
     * @param cal Calendar 对象
     * @return 格式化的日期字符串
     */
    public static String formatDate(Calendar cal) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        return sdf.format(cal.getTime());
    }
    
    /**
     * 添加月份
     * 
     * 返回添加指定月份数后的新 Calendar 对象（不修改原对象）。
     * 
     * @param cal 原始 Calendar 对象
     * @param months 要添加的月份数（可为负数）
     * @return 新的 Calendar 对象
     */
    public static Calendar addMonths(Calendar cal, int months) {
        Calendar result = (Calendar) cal.clone();
        result.add(Calendar.MONTH, months);
        return result;
    }
}
