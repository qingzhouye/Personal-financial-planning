/**
 * ============================================================================
 * 文件名: LoanManagerApplication.java
 * 模块:   应用入口
 * 功能:   Android Application 类，作为整个应用的入口点
 * 
 * 主要职责:
 *   1. 在应用启动时初始化全局资源（数据库实例）
 *   2. 在应用终止时清理全局资源
 *   3. 提供全局上下文访问点
 * 
 * 架构说明:
 *   - 继承自 Application 类，在 AndroidManifest.xml 中注册
 *   - 使用单例模式管理数据库实例，确保全局只有一个数据库连接
 *   - 数据库初始化在 onCreate() 中完成，保证在任何 Activity 启动前数据库已就绪
 * 
 * 使用方式:
 *   该类由 Android 系统自动实例化，无需手动创建
 *   可通过 getApplicationContext() 获取应用上下文
 * ============================================================================
 */
package com.finance.loanmanager;

import android.app.Application;
import android.util.Log;

import com.finance.loanmanager.data.AppDatabase;

/**
 * 贷款管理器应用入口类
 * 
 * 该类继承自 Application，是整个应用的入口点。
 * 负责在应用启动时初始化全局组件（如数据库），
 * 并在应用退出时清理资源。
 * 
 * @author LoanManager Team
 * @version 1.0
 */
public class LoanManagerApplication extends Application {
    
    /** 日志标签，用于识别该类的日志输出 */
    private static final String TAG = "LoanManagerApp";
    
    /**
     * 应用启动时回调
     * 
     * 该方法在应用启动时由系统调用，早于任何 Activity 的创建。
     * 主要功能：
     *   - 初始化 Room 数据库实例（单例模式）
     *   - 执行其他全局初始化操作
     * 
     * 注意事项：
     *   - 该方法在主线程执行，避免执行耗时操作
     *   - 如果初始化失败，记录错误日志但不崩溃应用
     */
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            // 初始化数据库实例
            // 使用单例模式确保整个应用共享同一个数据库连接
            // 数据库初始化会创建或打开 loan_manager.db 文件
            AppDatabase.getInstance(this);
            Log.d(TAG, "Database initialized successfully");
        } catch (Exception e) {
            // 记录初始化失败日志
            // 即使数据库初始化失败，应用也不会崩溃
            // 后续使用数据库时会再次尝试初始化或抛出异常
            Log.e(TAG, "Failed to initialize database: " + e.getMessage(), e);
        }
    }
    
    /**
     * 应用终止时回调
     * 
     * 该方法在应用进程被终止时调用（注意：在真实设备上可能不会被调用）。
     * 主要功能：
     *   - 销毁数据库实例，释放资源
     *   - 清理其他全局资源
     * 
     * 注意事项：
     *   - 在生产环境中，该方法可能不会被系统调用
     *   - 不应在此方法中保存关键数据
     */
    @Override
    public void onTerminate() {
        super.onTerminate();
        // 销毁数据库单例实例，释放数据库连接资源
        // 这有助于在应用退出时正确关闭数据库连接
        AppDatabase.destroyInstance();
    }
}
