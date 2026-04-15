/**
 * ============================================================================
 * 文件名: ThemeManager.java
 * 模块:   工具类层 (util)
 * 功能:   主题管理器，负责应用主题颜色的保存和应用
 * 
 * 主要职责:
 *   1. 保存用户选择的主题颜色
 *   2. 在 Activity 创建时应用对应主题
 *   3. 提供主题颜色值供 UI 组件使用
 * 
 * 可用主题:
 *   - THEME_CYAN (0):   青色主题（默认）- 清新活力
 *   - THEME_BLUE (1):   深蓝商务主题 - 稳重专业
 *   - THEME_ORANGE (2): 暖橙活力主题 - 温暖积极
 *   - THEME_PURPLE (3): 紫色优雅主题 - 优雅高贵
 *   - THEME_GREEN (4):  深绿自然主题 - 自然稳重
 *   - THEME_ROSE (5):   玫瑰粉主题 - 温柔浪漫
 * 
 * 使用方式:
 *   1. 在 Activity.onCreate() 的 super.onCreate() 之前调用 applyTheme()
 *   2. 在设置页面使用 saveTheme() 保存用户选择
 *   3. 使用 getThemePrimaryColor() 获取主题色用于 UI 渲染
 * 
 * 持久化方式:
 *   - 使用 SharedPreferences 存储主题索引
 *   - 配置文件名: "ThemePrefs"
 *   - 配置键: "selected_theme"
 * 
 * @see BackgroundManager 背景图片管理
 * ============================================================================
 */
package com.finance.loanmanager.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.finance.loanmanager.R;

/**
 * 主题管理器
 * 
 * 该类负责管理应用的主题颜色功能。用户可以在设置页面选择喜欢的主题颜色，
 * 系统会在每次启动 Activity 时自动应用保存的主题。
 * 
 * 主题切换原理:
 *   Android 不支持运行时动态切换主题，需要在 Activity 创建前设置。
 *   因此每个 Activity 都需要在 onCreate() 开头调用 applyTheme()。
 * 
 * 使用示例:
 *   // 在 Activity 中
 *   protected void onCreate(Bundle savedInstanceState) {
 *       ThemeManager.applyTheme(this);  // 必须在 super.onCreate() 之前
 *       super.onCreate(savedInstanceState);
 *       // ...
 *   }
 *   
 *   // 保存用户选择的主题
 *   ThemeManager.saveTheme(context, ThemeManager.THEME_BLUE);
 */
public class ThemeManager {

    // ==================== 常量定义 ====================
    
    /** SharedPreferences 配置文件名 */
    private static final String PREFS_NAME = "ThemePrefs";
    
    /** 主题索引配置键 */
    private static final String KEY_THEME = "selected_theme";

    // ==================== 主题索引常量 ====================
    
    /** 青色主题（默认）- 清新活力风格 */
    public static final int THEME_CYAN = 0;
    
    /** 深蓝商务主题 - 稳重专业风格 */
    public static final int THEME_BLUE = 1;
    
    /** 暖橙活力主题 - 温暖积极风格 */
    public static final int THEME_ORANGE = 2;
    
    /** 紫色优雅主题 - 优雅高贵风格 */
    public static final int THEME_PURPLE = 3;
    
    /** 深绿自然主题 - 自然稳重风格 */
    public static final int THEME_GREEN = 4;
    
    /** 玫瑰粉主题 - 温柔浪漫风格 */
    public static final int THEME_ROSE = 5;

    /** 主题总数 */
    public static final int THEME_COUNT = 6;

    // ==================== 主题保存与读取 ====================

    /**
     * 保存用户选择的主题
     */
    public static void saveTheme(Context context, int themeIndex) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME, themeIndex).apply();
    }

    /**
     * 获取当前保存的主题索引，默认返回青色主题（0）
     */
    public static int getSavedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME, THEME_CYAN);
    }

    /**
     * 根据保存的主题设置 Activity 主题，必须在 super.onCreate() 之前调用
     */
    public static void applyTheme(Activity activity) {
        int themeIndex = getSavedTheme(activity);
        switch (themeIndex) {
            case THEME_BLUE:
                activity.setTheme(R.style.Theme_LoanManager_Blue);
                break;
            case THEME_ORANGE:
                activity.setTheme(R.style.Theme_LoanManager_Orange);
                break;
            case THEME_PURPLE:
                activity.setTheme(R.style.Theme_LoanManager_Purple);
                break;
            case THEME_GREEN:
                activity.setTheme(R.style.Theme_LoanManager_Green);
                break;
            case THEME_ROSE:
                activity.setTheme(R.style.Theme_LoanManager_Rose);
                break;
            case THEME_CYAN:
            default:
                activity.setTheme(R.style.Theme_LoanManager);
                break;
        }
    }

    /**
     * 获取主题对应的主色值（用于 UI 预览色块）
     */
    public static int getThemePrimaryColor(int themeIndex) {
        switch (themeIndex) {
            case THEME_BLUE:   return 0xFF1976D2;
            case THEME_ORANGE: return 0xFFEF6C00;
            case THEME_PURPLE: return 0xFF8E24AA;
            case THEME_GREEN:  return 0xFF43A047;
            case THEME_ROSE:   return 0xFFC2185B;
            case THEME_CYAN:
            default:           return 0xFF00ACC1;
        }
    }

    /**
     * 获取主题对应的深色值（用于色块边框等）
     */
    public static int getThemeDarkColor(int themeIndex) {
        switch (themeIndex) {
            case THEME_BLUE:   return 0xFF1565C0;
            case THEME_ORANGE: return 0xFFE65100;
            case THEME_PURPLE: return 0xFF7B1FA2;
            case THEME_GREEN:  return 0xFF388E3C;
            case THEME_ROSE:   return 0xFFAD1457;
            case THEME_CYAN:
            default:           return 0xFF00838F;
        }
    }
}
