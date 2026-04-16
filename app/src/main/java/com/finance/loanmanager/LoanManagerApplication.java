package com.finance.loanmanager;

import android.app.Application;
import android.util.Log;

import com.finance.loanmanager.data.AppDatabase;

/**
 * 应用入口类
 */
public class LoanManagerApplication extends Application {
    
    private static final String TAG = "LoanManagerApp";
    
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            // 初始化数据库
            AppDatabase.getInstance(this);
            Log.d(TAG, "Database initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize database: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        // 清理资源
        AppDatabase.destroyInstance();
    }
}
