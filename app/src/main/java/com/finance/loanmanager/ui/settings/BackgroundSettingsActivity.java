package com.finance.loanmanager.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.finance.loanmanager.R;
import com.finance.loanmanager.ui.BaseActivity;
import com.finance.loanmanager.util.BackgroundManager;
import com.finance.loanmanager.util.ThemeManager;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.InputStream;

/**
 * 背景与主题个性化设置 Activity
 * 允许用户选择主题色调和自定义背景图片
 */
public class BackgroundSettingsActivity extends BaseActivity {
    
    private static final int REQUEST_CODE_UCROP = 1001;
    
    private BackgroundManager backgroundManager;
    private ImageView ivPreview;
    private TextView tvStatus;
    private Button btnSelectImage;
    private Button btnResetBackground;
    private View previewContainer;

    // 主题色块容器
    private LinearLayout themeItemCyan;
    private LinearLayout themeItemBlue;
    private LinearLayout themeItemOrange;
    private LinearLayout themeItemPurple;
    private LinearLayout themeItemGreen;
    private LinearLayout themeItemRose;

    // 主题勾选图标
    private ImageView checkCyan;
    private ImageView checkBlue;
    private ImageView checkOrange;
    private ImageView checkPurple;
    private ImageView checkGreen;
    private ImageView checkRose;
    
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    
    private int screenWidth;
    private int screenHeight;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_background_settings);
        
        // 适配 Android 15/16 Edge-to-Edge 安全区域
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.personalization_settings);
        
        backgroundManager = new BackgroundManager(this);
        
        // 获取屏幕尺寸
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        
        initViews();
        setupLaunchers();
        updateUI();
    }
    
    private void initViews() {
        // 背景相关
        ivPreview = findViewById(R.id.ivPreview);
        tvStatus = findViewById(R.id.tvStatus);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnResetBackground = findViewById(R.id.btnResetBackground);
        previewContainer = findViewById(R.id.previewContainer);
        
        btnSelectImage.setOnClickListener(v -> selectImage());
        btnResetBackground.setOnClickListener(v -> confirmResetBackground());

        // 主题色块
        themeItemCyan   = findViewById(R.id.themeItemCyan);
        themeItemBlue   = findViewById(R.id.themeItemBlue);
        themeItemOrange = findViewById(R.id.themeItemOrange);
        themeItemPurple = findViewById(R.id.themeItemPurple);
        themeItemGreen  = findViewById(R.id.themeItemGreen);
        themeItemRose   = findViewById(R.id.themeItemRose);

        checkCyan   = findViewById(R.id.checkCyan);
        checkBlue   = findViewById(R.id.checkBlue);
        checkOrange = findViewById(R.id.checkOrange);
        checkPurple = findViewById(R.id.checkPurple);
        checkGreen  = findViewById(R.id.checkGreen);
        checkRose   = findViewById(R.id.checkRose);

        themeItemCyan.setOnClickListener(v -> applySelectedTheme(ThemeManager.THEME_CYAN));
        themeItemBlue.setOnClickListener(v -> applySelectedTheme(ThemeManager.THEME_BLUE));
        themeItemOrange.setOnClickListener(v -> applySelectedTheme(ThemeManager.THEME_ORANGE));
        themeItemPurple.setOnClickListener(v -> applySelectedTheme(ThemeManager.THEME_PURPLE));
        themeItemGreen.setOnClickListener(v -> applySelectedTheme(ThemeManager.THEME_GREEN));
        themeItemRose.setOnClickListener(v -> applySelectedTheme(ThemeManager.THEME_ROSE));

        updateThemeSelection();
    }
    
    private void setupLaunchers() {
        // 图片选择器
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri sourceUri = result.getData().getData();
                    if (sourceUri != null) {
                        startCrop(sourceUri);
                    }
                }
            }
        );
    }
    
    private void updateUI() {
        if (backgroundManager.hasCustomBackground()) {
            previewContainer.setVisibility(View.VISIBLE);
            tvStatus.setText(R.string.custom_background_set);
            btnResetBackground.setVisibility(View.VISIBLE);
            
            // 加载预览图
            Glide.with(this)
                .load(backgroundManager.getBackgroundFile())
                .centerCrop()
                .into(ivPreview);
        } else {
            previewContainer.setVisibility(View.GONE);
            tvStatus.setText(R.string.no_custom_background);
            btnResetBackground.setVisibility(View.GONE);
        }
    }

    /**
     * 应用用户选择的主题，保存后重迟 Activity 生效
     */
    private void applySelectedTheme(int themeIndex) {
        int current = ThemeManager.getSavedTheme(this);
        if (current == themeIndex) return; // 尚未改变，无需操作
        ThemeManager.saveTheme(this, themeIndex);
        // 重迟当前 Activity，使主题立即生效
        recreate();
    }

    /**
     * 更新主题选中状态：显示当前选中主题的勾选图标
     */
    private void updateThemeSelection() {
        int selected = ThemeManager.getSavedTheme(this);
        checkCyan.setVisibility(selected == ThemeManager.THEME_CYAN ? View.VISIBLE : View.GONE);
        checkBlue.setVisibility(selected == ThemeManager.THEME_BLUE ? View.VISIBLE : View.GONE);
        checkOrange.setVisibility(selected == ThemeManager.THEME_ORANGE ? View.VISIBLE : View.GONE);
        checkPurple.setVisibility(selected == ThemeManager.THEME_PURPLE ? View.VISIBLE : View.GONE);
        checkGreen.setVisibility(selected == ThemeManager.THEME_GREEN ? View.VISIBLE : View.GONE);
        checkRose.setVisibility(selected == ThemeManager.THEME_ROSE ? View.VISIBLE : View.GONE);

        // 选中的色块加粗边框高亮
        highlightThemeItem(themeItemCyan, selected == ThemeManager.THEME_CYAN);
        highlightThemeItem(themeItemBlue, selected == ThemeManager.THEME_BLUE);
        highlightThemeItem(themeItemOrange, selected == ThemeManager.THEME_ORANGE);
        highlightThemeItem(themeItemPurple, selected == ThemeManager.THEME_PURPLE);
        highlightThemeItem(themeItemGreen, selected == ThemeManager.THEME_GREEN);
        highlightThemeItem(themeItemRose, selected == ThemeManager.THEME_ROSE);
    }

    /**
     * 高亮或取消高亮主题色块容器
     */
    private void highlightThemeItem(LinearLayout item, boolean selected) {
        float scale = selected ? 1.1f : 1.0f;
        item.setScaleX(scale);
        item.setScaleY(scale);
        item.setAlpha(selected ? 1.0f : 0.75f);
    }
    
    /**
     * 选择图片
     */
    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        // 支持多种图片格式
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/bmp"
        });
        imagePickerLauncher.launch(intent);
    }
    
    /**
     * 开始裁切图片
     */
    private void startCrop(Uri sourceUri) {
        // 创建临时裁切输出文件
        File cropFile = new File(getCacheDir(), "crop_temp_" + System.currentTimeMillis() + ".jpg");
        Uri destinationUri = Uri.fromFile(cropFile);
        
        // 设置裁切比例为屏幕比例
        float aspectRatioX = screenWidth;
        float aspectRatioY = screenHeight;
        
        // 简化比例
        int gcd = gcd((int) aspectRatioX, (int) aspectRatioY);
        aspectRatioX = aspectRatioX / gcd;
        aspectRatioY = aspectRatioY / gcd;
        
        UCrop.Options options = new UCrop.Options();
        options.setCompressionQuality(90);
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        options.withAspectRatio(aspectRatioX, aspectRatioY);
        options.withMaxResultSize(screenWidth, screenHeight);
        options.setToolbarTitle(getString(R.string.crop_background));
        options.setStatusBarColor(getResources().getColor(R.color.primary_dark, null));
        options.setToolbarColor(getResources().getColor(R.color.primary, null));
        options.setActiveControlsWidgetColor(getResources().getColor(R.color.accent, null));
        
        UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(aspectRatioX, aspectRatioY)
            .withMaxResultSize(screenWidth, screenHeight)
            .withOptions(options)
            .start(this, REQUEST_CODE_UCROP);
    }
    
    /**
     * 计算最大公约数
     */
    private int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }
    
    /**
     * 保存裁切后的图片
     */
    private void saveCroppedImage(Uri croppedUri) {
        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(croppedUri);
                if (inputStream != null) {
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();
                    
                    if (bitmap != null) {
                        // 保存到背景管理器
                        File outputFile = backgroundManager.getBackgroundFile();
                        java.io.FileOutputStream fos = new java.io.FileOutputStream(outputFile);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                        fos.flush();
                        fos.close();
                        bitmap.recycle();
                        
                        // 更新路径
                        backgroundManager.setBackgroundPath(outputFile.getAbsolutePath());
                        
                        runOnUiThread(() -> {
                            Toast.makeText(this, R.string.background_set_success, Toast.LENGTH_SHORT).show();
                            updateUI();
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.background_set_failed, e.getMessage()), 
                        Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_UCROP) {
            if (resultCode == RESULT_OK && data != null) {
                Uri resultUri = UCrop.getOutput(data);
                if (resultUri != null) {
                    saveCroppedImage(resultUri);
                }
            } else if (resultCode == UCrop.RESULT_ERROR && data != null) {
                Throwable error = UCrop.getError(data);
                if (error != null) {
                    Toast.makeText(this, "裁切失败: " + error.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    /**
     * 确认重置背景
     */
    private void confirmResetBackground() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.reset_background)
            .setMessage(R.string.reset_background_confirm)
            .setPositiveButton(R.string.confirm, (dialog, which) -> {
                backgroundManager.clearCustomBackground();
                BaseActivity.clearBackgroundCache();
                Toast.makeText(this, R.string.background_reset_success, Toast.LENGTH_SHORT).show();
                updateUI();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
