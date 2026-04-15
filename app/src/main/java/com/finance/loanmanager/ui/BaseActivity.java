/**
 * ============================================================================
 * 文件名: BaseActivity.java
 * 模块:   UI层 - 基础Activity
 * 功能:   Activity基类，统一处理主题应用和背景显示
 * 
 * 主要职责:
 *   1. 在 Activity 创建前应用用户选择的主题颜色
 *   2. 管理自定义背景图片的显示
 *   3. 设置透明状态栏和导航栏
 *   4. 提供背景缓存机制优化性能
 * 
 * 设计模式:
 *   - 模板方法模式: 子类继承此类，自动获得主题和背景功能
 *   - 缓存模式: 使用静态缓存避免重复加载背景资源
 * 
 * 背景显示逻辑:
 *   1. 如果用户设置了自定义背景图片，显示该图片
 *   2. 否则显示当前主题对应的渐变背景
 * 
 * 性能优化:
 *   - 静态缓存 Drawable 对象，避免每次 onResume 重复加载
 *   - 使用文件时间戳判断缓存是否过期
 *   - 同步加载优先，失败后异步备用
 * 
 * 使用方式:
 *   所有 Activity 都应继承此类而非 AppCompatActivity:
 *   public class MainActivity extends BaseActivity { ... }
 * 
 * @see ThemeManager 主题管理
 * @see BackgroundManager 背景管理
 * ============================================================================
 */
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
 * Activity 基类
 * 
 * 所有 Activity 都应继承此类，以获得统一的主题和背景处理能力。
 * 该类封装了主题应用、背景图片加载、状态栏透明化等通用功能。
 * 
 * 生命周期处理:
 *   - onCreate: 应用主题、初始化背景管理器、设置透明状态栏
 *   - onResume: 更新状态栏图标颜色、应用背景
 * 
 * 继承示例:
 *   public class MainActivity extends BaseActivity {
 *       protected void onCreate(Bundle savedInstanceState) {
 *           super.onCreate(savedInstanceState);
 *           setContentView(R.layout.activity_main);
 *           // 其他初始化...
 *       }
 *   }
 */
public abstract class BaseActivity extends AppCompatActivity {
    
    // ==================== 成员变量 ====================
    
    /** 背景管理器实例 */
    private BackgroundManager backgroundManager;
    
    /** 缓存的背景 Drawable（静态共享，避免重复加载） */
    private static Drawable cachedBackgroundDrawable;
    
    /** 缓存背景时背景文件的时间戳（用于判断缓存是否过期） */
    private static long cachedBackgroundTimestamp;
    
    /** 缓存的主题索引（用于判断主题是否变化） */
    private static int cachedThemeIndex = -1;
    
    /** 缓存的主题渐变背景 */
    private static GradientDrawable cachedThemeGradient;
    
    // ==================== 生命周期方法 ====================
    
    /**
     * Activity 创建时调用
     * 
     * 执行顺序:
     *   1. 应用主题（必须在 super.onCreate() 之前）
     *   2. 初始化背景管理器
     *   3. 调用父类 onCreate
     *   4. 设置透明状态栏
     * 
     * @param savedInstanceState 保存的实例状态
     */
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
        
        // 【关键修复】确保视图已经布局完成再设置背景
        rootView.post(() -> {
            if (backgroundManager.hasCustomBackground()) {
                setBackgroundImage(rootView);
            } else {
                setThemeBackground(rootView);
            }
        });
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
     * 设置背景图片（带缓存优化和错误重试）
     */
    private void setBackgroundImage(View view) {
        File bgFile = backgroundManager.getBackgroundFile();
        if (!bgFile.exists()) {
            // 文件不存在时使用主题背景
            setThemeBackground(view);
            return;
        }
        
        long fileTimestamp = bgFile.lastModified();
        
        // 如果缓存的背景未变化，直接复用
        if (cachedBackgroundDrawable != null && cachedBackgroundTimestamp == fileTimestamp) {
            view.setBackground(cachedBackgroundDrawable.getConstantState().newDrawable().mutate());
            return;
        }
        
        // 【修复】在异步加载完成前，先设置主题渐变背景作为占位，避免界面空白
        int themeIndex = ThemeManager.getSavedTheme(this);
        int[] gradientColors = getThemeGradientColors(themeIndex);
        GradientDrawable placeholderGradient = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            gradientColors
        );
        view.setBackground(placeholderGradient);
        
        // 【关键修复】使用同步方式加载背景图片，避免异步加载失败问题
        // 如果同步加载失败，再尝试异步加载
        try {
            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
            options.inSampleSize = 1;
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(bgFile.getAbsolutePath(), options);
            
            if (bitmap != null) {
                // 同步加载成功，设置背景
                android.graphics.drawable.BitmapDrawable drawable = new android.graphics.drawable.BitmapDrawable(getResources(), bitmap);
                view.setBackground(drawable);
                // 缓存背景 drawable
                cachedBackgroundDrawable = drawable;
                cachedBackgroundTimestamp = fileTimestamp;
                return;
            }
        } catch (Exception e) {
            // 同步加载失败，继续尝试异步加载
            e.printStackTrace();
        }
        
        // 【备用方案】异步加载（当同步加载失败时）
        loadBackgroundAsync(view, bgFile, fileTimestamp);
    }
    
    /**
     * 异步加载背景图片（备用方案）
     */
    private void loadBackgroundAsync(View view, File bgFile, long fileTimestamp) {
        Glide.with(this)
            .load(bgFile)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .signature(new ObjectKey(fileTimestamp))
            .centerCrop()
            .listener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(GlideException e, Object model, 
                        Target<Drawable> target, boolean isFirstResource) {
                    // 【关键修复】加载失败时使用主题背景
                    setThemeBackground(view);
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
     * 获取 BackgroundManager 实例，供子类使用
     */
    protected BackgroundManager getBackgroundManager() {
        return backgroundManager;
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
