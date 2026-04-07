package com.finance.loanmanager.ui;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.ObjectKey;
import com.finance.loanmanager.util.BackgroundManager;
import com.finance.loanmanager.util.ThemeManager;

import java.io.File;

/**
 * Activity基类，负责处理自定义背景
 */
public abstract class BaseActivity extends AppCompatActivity {
    
    private BackgroundManager backgroundManager;
    // 静态缓存背景 Drawable，避免每次 onResume 重复加载
    private static Drawable cachedBackgroundDrawable;
    private static long cachedBackgroundTimestamp;
    private static int cachedThemeIndex = -1;
    private static GradientDrawable cachedThemeGradient;
    
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
        Window window = getWindow();
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false);
            window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
            window.setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        }
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            getSupportActionBar().setElevation(0);
        }
        
        updateStatusBarIconColor();
    }
    
    /**
     * 根据是否设置了自定义背景，动态设置状态栏图标颜色
     */
    protected void updateStatusBarIconColor() {
        Window window = getWindow();
        if (window == null) return;
        boolean hasCustomBg = backgroundManager != null && backgroundManager.hasCustomBackground();
        WindowInsetsControllerCompat controller =
            WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(!hasCustomBg);
            controller.setAppearanceLightNavigationBars(!hasCustomBg);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateStatusBarIconColor();
        applyBackground();
    }
    
    /**
     * 应用背景：将背景设置在 android.R.id.content 上，确保铺满整个屏幕含状态栏
     */
    protected void applyBackground() {
        View rootView = findViewById(android.R.id.content);
        if (rootView == null) return;
        
        if (backgroundManager.hasCustomBackground()) {
            setBackgroundImage(rootView);
        } else {
            setThemeBackground(rootView);
        }
    }
    
    /**
     * 设置主题色渐变背景（带缓存）
     */
    private void setThemeBackground(View view) {
        int themeIndex = ThemeManager.getSavedTheme(this);
        
        // 主题未变化时复用缓存的渐变背景
        if (cachedThemeGradient != null && cachedThemeIndex == themeIndex) {
            view.setBackground(cachedThemeGradient.getConstantState().newDrawable().mutate());
            return;
        }
        
        int[] gradientColors = getThemeGradientColors(themeIndex);
        GradientDrawable gradient = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            gradientColors
        );
        view.setBackground(gradient);
        
        // 缓存
        cachedThemeIndex = themeIndex;
        cachedThemeGradient = gradient;
    }
    
    /**
     * 获取主题对应的渐变颜色数组
     */
    private int[] getThemeGradientColors(int themeIndex) {
        switch (themeIndex) {
            case ThemeManager.THEME_BLUE:
                return new int[] { 0xFF42A5F5, 0xFF64B5F6, 0xFF90CAF9 };
            case ThemeManager.THEME_ORANGE:
                return new int[] { 0xFFFFA726, 0xFFFFB74D, 0xFFFFCC80 };
            case ThemeManager.THEME_PURPLE:
                return new int[] { 0xFFAB47BC, 0xFFBA68C8, 0xFFCE93D8 };
            case ThemeManager.THEME_GREEN:
                return new int[] { 0xFF66BB6A, 0xFF81C784, 0xFFA5D6A7 };
            case ThemeManager.THEME_ROSE:
                return new int[] { 0xFFEC407A, 0xFFF06292, 0xFFF48FB1 };
            case ThemeManager.THEME_CYAN:
            default:
                return new int[] { 0xFF4DD0E1, 0xFF63D7E5, 0xFF80DEEA };
        }
    }
    
    /**
     * 设置背景图片（带缓存优化）
     */
    private void setBackgroundImage(View view) {
        File bgFile = backgroundManager.getBackgroundFile();
        if (!bgFile.exists()) return;
        
        long fileTimestamp = bgFile.lastModified();
        
        // 如果缓存的背景未变化，直接复用
        if (cachedBackgroundDrawable != null && cachedBackgroundTimestamp == fileTimestamp) {
            view.setBackground(cachedBackgroundDrawable.getConstantState().newDrawable().mutate());
            return;
        }
        
        // 使用 Glide 加载，带磁盘缓存 + 文件签名用于缓存失效
        Glide.with(this)
            .load(bgFile)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .signature(new ObjectKey(fileTimestamp))
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
                    // 缓存背景 drawable
                    cachedBackgroundDrawable = resource;
                    cachedBackgroundTimestamp = fileTimestamp;
                    return true;
                }
            })
            .submit();
    }
    
    /**
     * 清除缓存的背景图片
     */
    public static void clearBackgroundCache() {
        cachedBackgroundDrawable = null;
        cachedBackgroundTimestamp = 0;
        cachedThemeIndex = -1;
        cachedThemeGradient = null;
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
