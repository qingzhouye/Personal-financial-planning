package com.finance.loanmanager;

import android.app.Application;

import com.finance.loanmanager.data.AppDatabase;

/**
 * 应用入口类
 */
public class LoanManagerApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化数据库
        AppDatabase.getInstance(this);
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        // 清理资源
        AppDatabase.destroyInstance();
    }
}
