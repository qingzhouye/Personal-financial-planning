package com.finance.loanmanager.ui;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
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
        if (backgroundManager.hasCustomBackground()) {
            View rootView = findViewById(android.R.id.content);
            if (rootView != null && rootView instanceof ViewGroup) {
                ViewGroup contentView = (ViewGroup) rootView;
                View mainView = contentView.getChildAt(0);
                if (mainView != null) {
                    setBackgroundImage(mainView);
                }
            }
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
