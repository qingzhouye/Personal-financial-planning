package com.finance.loanmanager.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.finance.loanmanager.R;
import com.finance.loanmanager.ui.BaseActivity;
import com.finance.loanmanager.util.BackgroundManager;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.InputStream;

/**
 * 背景设置Activity
 * 允许用户选择、裁切和预览自定义背景
 */
public class BackgroundSettingsActivity extends BaseActivity {
    
    private static final int REQUEST_CODE_UCROP = 1001;
    
    private BackgroundManager backgroundManager;
    private ImageView ivPreview;
    private TextView tvStatus;
    private Button btnSelectImage;
    private Button btnResetBackground;
    private View previewContainer;
    
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    
    private int screenWidth;
    private int screenHeight;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_background_settings);
        
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.background_settings);
        
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
        ivPreview = findViewById(R.id.ivPreview);
        tvStatus = findViewById(R.id.tvStatus);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnResetBackground = findViewById(R.id.btnResetBackground);
        previewContainer = findViewById(R.id.previewContainer);
        
        btnSelectImage.setOnClickListener(v -> selectImage());
        btnResetBackground.setOnClickListener(v -> confirmResetBackground());
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
