/**
 * ============================================================================
 * 文件名: AppDatabase.java
 * 模块:   数据库配置层 (data)
 * 功能:   Room 数据库配置类，管理数据库实例的创建和访问
 * 
 * 主要职责:
 *   1. 定义数据库配置（实体类、版本号等）
 *   2. 提供数据库实例的单例访问
 *   3. 提供 DAO 对象的访问入口
 *   4. 管理数据库生命周期
 * 
 * 设计模式:
 *   - 单例模式：确保整个应用只有一个数据库实例
 *   - 工厂模式：通过 Room.databaseBuilder() 创建数据库实例
 * 
 * 数据库配置:
 *   - 数据库文件名: loan_manager.db
 *   - 版本号: 1
 *   - 包含实体: Loan, Payment
 * 
 * 使用方式:
 *   AppDatabase database = AppDatabase.getInstance(context);
 *   LoanDao loanDao = database.loanDao();
 * ============================================================================
 */
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
 * Room 数据库配置类
 * 
 * 该类是 Room 持久化库的核心配置类，继承自 RoomDatabase。
 * 定义了数据库的结构和访问方式。
 * 
 * @Database 注解参数说明：
 *   - entities: 包含的实体类数组，每个实体对应一个数据库表
 *   - version: 数据库版本号，用于数据库迁移
 *   - exportSchema: 是否导出数据库结构到JSON文件（用于版本控制）
 * 
 * 注意事项：
 *   - 该类是抽象类，Room 会在编译时生成实现类
 *   - 每次修改数据库结构时需要更新版本号并提供迁移方案
 */
@Database(
    entities = {Loan.class, Payment.class},  // 数据库包含的实体类
    version = 1,                              // 数据库版本号
    exportSchema = false                      // 不导出数据库结构文件
)
public abstract class AppDatabase extends RoomDatabase {
    
    // ==================== 常量定义 ====================
    
    /** 数据库文件名称 */
    private static final String DATABASE_NAME = "loan_manager.db";
    
    /** 数据库单例实例（使用 volatile 确保多线程可见性） */
    private static volatile AppDatabase instance;
    
    // ==================== 抽象方法（DAO 访问入口） ====================
    
    /**
     * 获取贷款数据访问对象
     * 
     * Room 会自动实现该方法，返回 LoanDao 的实现类实例。
     * 
     * @return LoanDao 实例
     */
    public abstract LoanDao loanDao();
    
    /**
     * 获取还款记录数据访问对象
     * 
     * Room 会自动实现该方法，返回 PaymentDao 的实现类实例。
     * 
     * @return PaymentDao 实例
     */
    public abstract PaymentDao paymentDao();
    
    // ==================== 单例访问方法 ====================
    
    /**
     * 获取数据库实例（单例模式）
     * 
     * 使用双重检查锁定（Double-Checked Locking）确保：
     *   1. 线程安全
     *   2. 懒加载
     *   3. 高性能（避免每次调用都同步）
     * 
     * 数据库配置说明：
     *   - fallbackToDestructiveMigration: 版本升级时允许破坏性迁移（清空数据重建）
     *   - allowMainThreadQueries: 允许在主线程执行查询（简单应用可用，生产环境建议使用异步）
     * 
     * @param context 应用上下文
     * @return 数据库单例实例
     */
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),  // 使用应用上下文避免内存泄漏
                    AppDatabase.class,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()  // 版本升级时清空数据重建
                .allowMainThreadQueries()          // 允许主线程查询
                .build();
        }
        return instance;
    }
    
    /**
     * 销毁数据库实例
     * 
     * 在应用退出时调用，释放数据库资源。
     * 通常在 Application.onTerminate() 中调用。
     * 
     * 注意：在实际设备上，Application.onTerminate() 可能不会被调用，
     * 因此这个方法主要用于测试环境或特殊场景。
     */
    public static void destroyInstance() {
        instance = null;
    }
}
