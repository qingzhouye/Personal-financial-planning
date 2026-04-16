package com.finance.loanmanager.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 背景图片管理工具类
 * 负责保存、读取、裁切背景图片
 */
public class BackgroundManager {
    
    private static final String PREFS_NAME = "BackgroundPrefs";
    private static final String KEY_BACKGROUND_PATH = "background_path";
    private static final String BACKGROUND_DIR = "backgrounds";
    private static final String BACKGROUND_FILENAME = "custom_background.jpg";
    
    private final Context context;
    private final SharedPreferences prefs;
    
    public BackgroundManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * 检查是否设置了自定义背景
     */
    public boolean hasCustomBackground() {
        String path = getBackgroundPath();
        if (path == null || path.isEmpty()) {
            return false;
        }
        File file = new File(path);
        return file.exists();
    }
    
    /**
     * 获取背景图片路径
     */
    public String getBackgroundPath() {
        return prefs.getString(KEY_BACKGROUND_PATH, null);
    }
    
    /**
     * 设置背景图片路径
     */
    public void setBackgroundPath(String path) {
        prefs.edit().putString(KEY_BACKGROUND_PATH, path).apply();
    }
    
    /**
     * 清除自定义背景
     */
    public void clearCustomBackground() {
        String path = getBackgroundPath();
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                file.delete();
            }
        }
        prefs.edit().remove(KEY_BACKGROUND_PATH).apply();
    }
    
    /**
     * 获取背景图片文件
     */
    public File getBackgroundFile() {
        File dir = new File(context.getFilesDir(), BACKGROUND_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, BACKGROUND_FILENAME);
    }
    
    /**
     * 保存并裁切图片以适应屏幕
     * @param sourceUri 原始图片URI
     * @param screenWidth 屏幕宽度
     * @param screenHeight 屏幕高度
     * @return 保存后的文件路径，失败返回null
     */
    public String saveAndCropBackground(Uri sourceUri, int screenWidth, int screenHeight) {
        try {
            // 读取原始图片
            InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
            if (inputStream == null) {
                return null;
            }
            
            // 先获取图片尺寸
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();
            
            // 计算采样率
            int sampleSize = calculateInSampleSize(options.outWidth, options.outHeight, 
                    screenWidth, screenHeight);
            
            // 重新读取图片
            inputStream = context.getContentResolver().openInputStream(sourceUri);
            options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize;
            Bitmap sourceBitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();
            
            if (sourceBitmap == null) {
                return null;
            }
            
            // 裁切图片以适应屏幕比例
            Bitmap croppedBitmap = cropToScreenRatio(sourceBitmap, screenWidth, screenHeight);
            
            // 保存到内部存储
            File outputFile = getBackgroundFile();
            FileOutputStream fos = new FileOutputStream(outputFile);
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();
            
            // 回收资源
            if (croppedBitmap != sourceBitmap) {
                croppedBitmap.recycle();
            }
            sourceBitmap.recycle();
            
            // 保存路径
            String path = outputFile.getAbsolutePath();
            setBackgroundPath(path);
            
            return path;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 计算采样率
     */
    private int calculateInSampleSize(int width, int height, int reqWidth, int reqHeight) {
        int inSampleSize = 1;
        
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        
        return inSampleSize;
    }
    
    /**
     * 裁切图片以适应屏幕比例（居中裁切）
     */
    private Bitmap cropToScreenRatio(Bitmap source, int screenWidth, int screenHeight) {
        int srcWidth = source.getWidth();
        int srcHeight = source.getHeight();
        
        // 计算目标比例
        float targetRatio = (float) screenWidth / screenHeight;
        float srcRatio = (float) srcWidth / srcHeight;
        
        int cropWidth, cropHeight, cropX, cropY;
        
        if (srcRatio > targetRatio) {
            // 源图片更宽，裁切左右
            cropHeight = srcHeight;
            cropWidth = (int) (srcHeight * targetRatio);
            cropX = (srcWidth - cropWidth) / 2;
            cropY = 0;
        } else {
            // 源图片更高，裁切上下
            cropWidth = srcWidth;
            cropHeight = (int) (srcWidth / targetRatio);
            cropX = 0;
            cropY = (srcHeight - cropHeight) / 2;
        }
        
        // 确保裁切区域不超过图片范围
        cropWidth = Math.min(cropWidth, srcWidth - cropX);
        cropHeight = Math.min(cropHeight, srcHeight - cropY);
        
        return Bitmap.createBitmap(source, cropX, cropY, cropWidth, cropHeight);
    }
    
    /**
     * 获取背景图片的Bitmap
     */
    public Bitmap getBackgroundBitmap() {
        String path = getBackgroundPath();
        if (path == null) {
            return null;
        }
        
        try {
            File file = new File(path);
            if (!file.exists()) {
                return null;
            }
            
            FileInputStream fis = new FileInputStream(file);
            Bitmap bitmap = BitmapFactory.decodeStream(fis);
            fis.close();
            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 获取背景图片URI
     */
    public Uri getBackgroundUri() {
        String path = getBackgroundPath();
        if (path == null) {
            return null;
        }
        
        File file = new File(path);
        if (!file.exists()) {
            return null;
        }
        
        return Uri.fromFile(file);
    }
}
