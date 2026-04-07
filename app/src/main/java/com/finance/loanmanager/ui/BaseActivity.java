package com.finance.loanmanager.ui;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.finance.loanmanager.util.BackgroundManager;
import com.finance.loanmanager.util.ThemeManager;

/**
 * Activity基类，负责处理自定义背景
 */
public abstract class BaseActivity extends AppCompatActivity {
    
    private BackgroundManager backgroundManager;
    private View backgroundView;
    private static Bitmap cachedBackgroundBitmap;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 必须在 super.onCreate() 之前应用主题，确保颜色在布局渲染前生效
        ThemeManager.applyTheme(this);
        
        // 在setContentView之前设置主题背景
        backgroundManager = new BackgroundManager(this);
        
        super.onCreate(savedInstanceState);
        
        // 设置透明状态栏和ActionBar
        setupTransparentStatusBarAndActionBar();
    }
    
    /**
     * 设置透明状态栏和ActionBar背景
     */
    protected void setupTransparentStatusBarAndActionBar() {
        // 设置状态栏透明
        Window window = getWindow();
        if (window != null) {
            // 关键：让内容延伸到系统栏区域（Edge-to-Edge），状态栏才真正透明
            WindowCompat.setDecorFitsSystemWindows(window, false);
            // 状态栏透明
            window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
        }
        
        // 设置ActionBar背景透明
        if (getSupportActionBar() != null) {
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            // 禁用ActionBar阴影
            getSupportActionBar().setElevation(0);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        applyBackground();
    }
    
    /**
     * 应用自定义背景
     * 在子类setContentView之后调用
     */
    protected void applyBackground() {
        View rootView = findViewById(android.R.id.content);
        if (rootView != null && rootView instanceof ViewGroup) {
            ViewGroup contentView = (ViewGroup) rootView;
            View mainView = contentView.getChildAt(0);
            if (mainView != null) {
                if (backgroundManager.hasCustomBackground()) {
                    setBackgroundImage(mainView);
                } else {
                    // 无自定义背景时，使用主题色渐变背景
                    setThemeBackground(mainView);
                }
            }
        }
    }
    
    /**
     * 设置主题色渐变背景
     */
    private void setThemeBackground(View view) {
        int themeIndex = ThemeManager.getSavedTheme(this);
        int[] gradientColors = getThemeGradientColors(themeIndex);
        
        GradientDrawable gradient = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            gradientColors
        );
        view.setBackground(gradient);
    }
    
    /**
     * 获取主题对应的渐变颜色数组
     */
    private int[] getThemeGradientColors(int themeIndex) {
        switch (themeIndex) {
            case ThemeManager.THEME_BLUE:
                return new int[] { 0xFF42A5F5, 0xFF64B5F6, 0xFF90CAF9 }; // 深蓝渐变（柔和）
            case ThemeManager.THEME_ORANGE:
                return new int[] { 0xFFFFA726, 0xFFFFB74D, 0xFFFFCC80 }; // 橙色渐变（柔和）
            case ThemeManager.THEME_PURPLE:
                return new int[] { 0xFFAB47BC, 0xFFBA68C8, 0xFFCE93D8 }; // 紫色渐变（柔和）
            case ThemeManager.THEME_GREEN:
                return new int[] { 0xFF66BB6A, 0xFF81C784, 0xFFA5D6A7 }; // 绿色渐变（柔和）
            case ThemeManager.THEME_ROSE:
                return new int[] { 0xFFEC407A, 0xFFF06292, 0xFFF48FB1 }; // 玫瑰渐变（柔和）
            case ThemeManager.THEME_CYAN:
            default:
                return new int[] { 0xFF4DD0E1, 0xFF63D7E5, 0xFF80DEEA }; // 青色渐变（柔和）
        }
    }
    
    /**
     * 设置背景图片
     */
    private void setBackgroundImage(View view) {
        // 获取屏幕尺寸
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        
        // 使用Glide加载背景图片
        Glide.with(this)
            .load(backgroundManager.getBackgroundFile())
            .override(screenWidth, screenHeight)
            .centerCrop()
            .listener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(GlideException e, Object model, 
                        Target<Drawable> target, boolean isFirstResource) {
                    return false;
                }
                
                @Override
                public boolean onResourceReady(Drawable resource, Object model, 
                        Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    view.setBackground(resource);
                    return true;
                }
            })
            .submit();
    }
    
    /**
     * 清除缓存的背景图片
     */
    public static void clearBackgroundCache() {
        cachedBackgroundBitmap = null;
    }
    
    /**
     * 设置Edge-to-Edge安全区域
     */
    protected void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
