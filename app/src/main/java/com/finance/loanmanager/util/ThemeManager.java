package com.finance.loanmanager.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.finance.loanmanager.R;

/**
 * 主题管理器
 * 负责保存和应用用户选择的主题颜色
 */
public class ThemeManager {

    private static final String PREFS_NAME = "ThemePrefs";
    private static final String KEY_THEME = "selected_theme";

    // 主题索引常量
    public static final int THEME_CYAN = 0;    // 青色（默认）
    public static final int THEME_BLUE = 1;    // 深蓝商务
    public static final int THEME_ORANGE = 2;  // 暖橙活力
    public static final int THEME_PURPLE = 3;  // 紫色优雅
    public static final int THEME_GREEN = 4;   // 深绿自然
    public static final int THEME_ROSE = 5;    // 玫瑰粉

    public static final int THEME_COUNT = 6;

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
            case THEME_BLUE:   return 0xFF1565C0;
            case THEME_ORANGE: return 0xFFE65100;
            case THEME_PURPLE: return 0xFF6A1B9A;
            case THEME_GREEN:  return 0xFF2E7D32;
            case THEME_ROSE:   return 0xFFAD1457;
            case THEME_CYAN:
            default:           return 0xFF00838F;
        }
    }

    /**
     * 获取主题对应的深色值（用于色块边框等）
     */
    public static int getThemeDarkColor(int themeIndex) {
        switch (themeIndex) {
            case THEME_BLUE:   return 0xFF0D47A1;
            case THEME_ORANGE: return 0xFFBF360C;
            case THEME_PURPLE: return 0xFF4A148C;
            case THEME_GREEN:  return 0xFF1B5E20;
            case THEME_ROSE:   return 0xFF880E4F;
            case THEME_CYAN:
            default:           return 0xFF006064;
        }
    }
}
