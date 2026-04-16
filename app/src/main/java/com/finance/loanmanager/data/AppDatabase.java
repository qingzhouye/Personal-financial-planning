package com.finance.loanmanager.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.finance.loanmanager.data.dao.LoanDao;
import com.finance.loanmanager.data.dao.PaymentDao;
import com.finance.loanmanager.data.entity.Loan;
import com.finance.loanmanager.data.entity.Payment;

/**
 * Room数据库
 */
@Database(
    entities = {Loan.class, Payment.class},
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "loan_manager.db";
    private static volatile AppDatabase instance;
    
    public abstract LoanDao loanDao();
    public abstract PaymentDao paymentDao();
    
    /**
     * 获取数据库实例（单例模式）
     */
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build();
        }
        return instance;
    }
    
    /**
     * 销毁数据库实例
     */
    public static void destroyInstance() {
        instance = null;
    }
}
