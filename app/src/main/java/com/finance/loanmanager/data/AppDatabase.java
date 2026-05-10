package com.finance.loanmanager.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.finance.loanmanager.data.dao.LoanDao;
import com.finance.loanmanager.data.dao.PaymentDao;
import com.finance.loanmanager.data.dao.SavingsDao;
import com.finance.loanmanager.data.entity.Loan;
import com.finance.loanmanager.data.entity.Payment;
import com.finance.loanmanager.data.entity.Savings;

/**
 * Room数据库
 */
@Database(
    entities = {Loan.class, Payment.class, Savings.class},
    version = 2,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "loan_manager.db";
    private static volatile AppDatabase instance;
    
    public abstract LoanDao loanDao();
    public abstract PaymentDao paymentDao();
    public abstract SavingsDao savingsDao();

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS savings ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + "amount REAL NOT NULL DEFAULT 0, "
                + "date TEXT NOT NULL DEFAULT '', "
                + "note TEXT, "
                + "createdAt INTEGER NOT NULL DEFAULT 0)");
        }
    };
    
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
                .addMigrations(MIGRATION_1_2)
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
