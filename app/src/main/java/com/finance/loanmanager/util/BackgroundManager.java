/**
 * ============================================================================
 * 文件名: BackgroundManager.java
 * 模块:   工具类层 (util)
 * 功能:   背景图片管理工具类，负责应用背景图片的存储和管理
 * 
 * 主要职责:
 *   1. 保存用户选择的自定义背景图片
 *   2. 自动裁切图片以适应屏幕比例
 *   3. 管理背景图片的读取和删除
 *   4. 图片采样和内存优化
 * 
 * 技术细节:
 *   - 使用 SharedPreferences 存储背景路径配置
 *   - 使用应用内部存储 (filesDir) 保存图片文件
 *   - 采用采样率压缩减少内存占用
 *   - 居中裁切保证图片比例适配屏幕
 * 
 * 使用场景:
 *   - 用户在设置页面选择自定义背景图片
 *   - 各 Activity 加载并显示背景图片
 *   - 用户清除自定义背景恢复默认
 * 
 * @see ThemeManager 主题颜色管理
 * ============================================================================
 */
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
 * 
 * 该类负责管理应用的自定义背景图片功能。用户可以选择相册中的图片作为应用背景，
 * 系统会自动将图片裁切为适合屏幕的比例并保存到内部存储中。
 * 
 * 内存优化策略:
 *   1. 使用 inSampleSize 进行图片采样，降低加载分辨率
 *   2. 及时回收 Bitmap 资源
 *   3. 使用 JPEG 格式压缩存储
 * 
 * 使用示例:
 *   BackgroundManager manager = new BackgroundManager(context);
 *   if (manager.hasCustomBackground()) {
 *       Bitmap bg = manager.getBackgroundBitmap();
 *       // 设置背景
 *   }
 */
public class BackgroundManager {
    
    // ==================== 常量定义 ====================
    
    /** SharedPreferences 配置文件名 */
    private static final String PREFS_NAME = "BackgroundPrefs";
    
    /** 背景图片路径的配置键 */
    private static final String KEY_BACKGROUND_PATH = "background_path";
    
    /** 背景图片存储目录名 */
    private static final String BACKGROUND_DIR = "backgrounds";
    
    /** 背景图片文件名 */
    private static final String BACKGROUND_FILENAME = "custom_background.jpg";
    
    // ==================== 成员变量 ====================
    
    /** 应用上下文（使用 ApplicationContext 避免内存泄漏） */
    private final Context context;
    
    /** SharedPreferences 用于持久化配置 */
    private final SharedPreferences prefs;
    
    // ==================== 构造方法 ====================
    
    /**
     * 构造背景管理器
     * 
     * @param context 上下文对象（内部会转换为 ApplicationContext）
     */
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
