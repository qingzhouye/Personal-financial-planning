package com.finance.loanmanager.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 日期工具类
 */
public class DateUtil {
    
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String DATE_FORMAT_CN = "yyyy年MM月dd日";
    private static final String MONTH_FORMAT = "yyyy年MM月";
    
    /**
     * 获取当前日期字符串
     */
    public static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        return sdf.format(new Date());
    }
    
    /**
     * 获取当前月份字符串
     */
    public static String getCurrentMonth() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        return sdf.format(new Date());
    }
    
    /**
     * 格式化日期为中文格式
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
    
    /**
     * 获取今天是几号
     */
    public static int getTodayDay() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.DAY_OF_MONTH);
    }
    
    /**
     * 获取本月剩余天数
     */
    public static int getRemainingDaysInMonth() {
        Calendar cal = Calendar.getInstance();
        int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int today = cal.get(Calendar.DAY_OF_MONTH);
        return lastDay - today + 1;
    }
    
    /**
     * 获取月份中的最后一天
     */
    public static int getLastDayOfMonth(int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1);
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }
    
    /**
     * 计算两个日期之间的月份差
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
    
    /**
     * 解析日期字符串
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
     */
    public static String formatDate(Calendar cal) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        return sdf.format(cal.getTime());
    }
    
    /**
     * 添加月份
     */
    public static Calendar addMonths(Calendar cal, int months) {
        Calendar result = (Calendar) cal.clone();
        result.add(Calendar.MONTH, months);
        return result;
    }
}
