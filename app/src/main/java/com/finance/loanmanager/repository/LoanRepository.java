/**
 * ============================================================================
 * 文件名: LoanRepository.java
 * 模块:   数据仓库层 (repository)
 * 功能:   贷款数据仓库，封装数据访问逻辑并提供统一的业务接口
 * 
 * 主要职责:
 *   1. 封装 LoanDao 和 PaymentDao 的数据访问操作
 *   2. 提供业务层面的数据操作方法
 *   3. 整合 LoanCalculator 进行贷款计算
 *   4. 实现自动备份功能
 *   5. 提供异步操作支持
 * 
 * 设计模式:
 *   - Repository 模式：隔离数据层和业务层
 *   - 单一数据入口：所有数据操作通过 Repository 进行
 *   - 异步处理：使用 ExecutorService 处理耗时操作
 * 
 * 架构位置:
 *   位于数据层和UI层之间，作为数据访问的唯一入口。
 *   UI层通过 Repository 访问数据，不直接操作 DAO。
 * 
 * 自动备份机制:
 *   每次数据变更后会触发自动备份，使用防抖机制避免频繁备份。
 * ============================================================================
 */
package com.finance.loanmanager.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.finance.loanmanager.data.AppDatabase;
import com.finance.loanmanager.data.dao.LoanDao;
import com.finance.loanmanager.data.dao.PaymentDao;
import com.finance.loanmanager.data.entity.Loan;
import com.finance.loanmanager.data.entity.LoanStatus;
import com.finance.loanmanager.data.entity.LoanWithPayments;
import com.finance.loanmanager.data.entity.Payment;
import com.finance.loanmanager.service.LoanCalculator;
import com.finance.loanmanager.util.BackupManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 贷款数据仓库类
 * 
 * 该类是应用数据访问的核心枢纽，封装了所有数据操作逻辑。
 * 遵循 Repository 模式，为 UI 层提供统一的数据访问接口。
 * 
 * 主要功能：
 *   1. 贷款数据的 CRUD 操作
 *   2. 还款记录的 CRUD 操作
 *   3. 贷款状态计算
 *   4. 还款计划生成
 *   5. 统计信息汇总
 *   6. 自动数据备份
 * 
 * @see LoanCalculator 贷款计算器
 * @see BackupManager 备份管理器
 */
public class LoanRepository {
    
    // ==================== 数据访问对象 ====================
    
    /** 贷款数据访问对象 */
    private final LoanDao loanDao;
    
    /** 还款记录数据访问对象 */
    private final PaymentDao paymentDao;
    
    /** 所有贷款的 LiveData（用于响应式UI更新） */
    private final LiveData<List<Loan>> allLoans;
    
    // ==================== 异步处理组件 ====================
    
    /** 线程池执行器，用于后台执行数据库操作 */
    private final ExecutorService executorService;
    
    /** 主线程 Handler，用于将结果切换回主线程 */
    private final Handler mainHandler;
    
    /** 应用上下文，用于创建 BackupManager */
    private final Application application;
    
    // ==================== 构造方法 ====================
    
    /**
     * 构造方法
     * 
     * 初始化数据访问对象和异步处理组件。
     * 
     * @param application 应用上下文
     */
    public LoanRepository(Application application) {
        this.application = application;
        // 获取数据库实例并初始化 DAO
        AppDatabase database = AppDatabase.getInstance(application);
        loanDao = database.loanDao();
        paymentDao = database.paymentDao();
        // 初始化 LiveData 观察
        allLoans = loanDao.getAllLoansLive();
        // 创建固定大小线程池，用于并发执行数据库操作
        executorService = Executors.newFixedThreadPool(4);
        // 创建主线程 Handler，用于 UI 更新
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    // ==================== 贷款操作方法 ====================
    
    /**
     * 获取所有贷款（响应式）
     * 
     * @return LiveData 包装的贷款列表，自动响应数据变化
     */
    public LiveData<List<Loan>> getAllLoans() {
        return allLoans;
    }
    
    /**
     * 获取所有贷款（同步）
     * 
     * 直接返回贷款列表，不包装为 LiveData。
     * 
     * @return 所有贷款列表
     */
    public List<Loan> getAllLoansSync() {
        return loanDao.getAllLoans();
    }
    
    /**
     * 根据ID获取贷款
     * 
     * @param loanId 贷款ID
     * @return 贷款对象，不存在则返回 null
     */
    public Loan getLoanById(int loanId) {
        return loanDao.getLoanById(loanId);
    }
    
    /**
     * 插入新贷款
     * 
     * 在插入前自动计算原始月供。
     * 插入后触发自动备份。
     * 
     * @param loan 要插入的贷款对象
     * @return 新插入记录的ID
     */
    public long insertLoan(Loan loan) {
        // 计算原始月供
        double originalPayment = LoanCalculator.calculateMonthlyPayment(
                loan.getPrincipal(),
                loan.getAnnualRate(),
                loan.getMonths(),
                loan.getRepaymentMethod()
        );
        loan.setOriginalMonthlyPayment(originalPayment);
        
        long id = loanDao.insertLoan(loan);
        // 数据变更后自动备份
        triggerAutoBackup();
        return id;
    }
    
    /**
     * 更新贷款
     * 
     * @param loan 要更新的贷款对象
     */
    public void updateLoan(Loan loan) {
        loanDao.updateLoan(loan);
        triggerAutoBackup();
    }
    
    /**
     * 删除贷款
     * 
     * @param loan 要删除的贷款对象
     */
    public void deleteLoan(Loan loan) {
        loanDao.deleteLoan(loan);
        triggerAutoBackup();
    }
    
    /**
     * 根据ID删除贷款
     * 
     * @param loanId 要删除的贷款ID
     */
    public void deleteLoanById(int loanId) {
        loanDao.deleteLoanById(loanId);
        triggerAutoBackup();
    }
    
    /**
     * 删除所有贷款
     */
    public void deleteAllLoans() {
        loanDao.deleteAllLoans();
        triggerAutoBackup();
    }
    
    // ==================== 还款记录操作方法 ====================
    
    /**
     * 获取所有还款记录（响应式）
     * 
     * @return LiveData 包装的还款记录列表
     */
    public LiveData<List<Payment>> getAllPayments() {
        return paymentDao.getAllPaymentsLive();
    }
    
    /**
     * 获取所有还款记录（同步）
     * 
     * @return 所有还款记录列表
     */
    public List<Payment> getAllPaymentsSync() {
        return paymentDao.getAllPayments();
    }
    
    /**
     * 根据贷款ID获取还款记录
     * 
     * @param loanId 贷款ID
     * @return 该贷款的所有还款记录
     */
    public List<Payment> getPaymentsByLoanId(int loanId) {
        return paymentDao.getPaymentsByLoanId(loanId);
    }
    
    /**
     * 插入还款记录
     * 
     * @param payment 要插入的还款记录
     * @return 新插入记录的ID
     */
    public long insertPayment(Payment payment) {
        long id = paymentDao.insertPayment(payment);
        triggerAutoBackup();
        return id;
    }
    
    /**
     * 删除还款记录
     * 
     * @param payment 要删除的还款记录
     */
    public void deletePayment(Payment payment) {
        paymentDao.deletePayment(payment);
        triggerAutoBackup();
    }
    
    /**
     * 删除所有还款记录
     */
    public void deleteAllPayments() {
        paymentDao.deleteAllPayments();
        triggerAutoBackup();
    }
    
    // ==================== 业务方法 ====================
    
    /**
     * 获取贷款及其状态
     * 
     * 遍历所有贷款，计算每个贷款的当前状态（剩余本金、剩余期数等）。
     * 这是统计页面和主页面核心数据的来源。
     * 
     * @return 贷款与状态的关联列表
     */
    public List<LoanWithStatus> getLoansWithStatus() {
        List<LoanWithPayments> loanWithPaymentsList = loanDao.getAllLoansWithPayments();
        List<LoanWithStatus> result = new ArrayList<>();
        
        for (LoanWithPayments lwp : loanWithPaymentsList) {
            // 调用 LoanCalculator 计算贷款状态
            LoanStatus status = LoanCalculator.calculateRemainingLoan(
                    lwp.getLoan(), lwp.getPayments());
            result.add(new LoanWithStatus(lwp.getLoan(), status));
        }
        
        return result;
    }
    
    /**
     * 获取单个贷款的状态
     * 
     * @param loanId 贷款ID
     * @return 贷款状态对象，贷款不存在则返回 null
     */
    public LoanStatus getLoanStatus(int loanId) {
        Loan loan = loanDao.getLoanById(loanId);
        if (loan == null) return null;
        
        List<Payment> payments = paymentDao.getPaymentsByLoanId(loanId);
        return LoanCalculator.calculateRemainingLoan(loan, payments);
    }
    
    /**
     * 获取还款计划
     * 
     * 生成该贷款的还款计划表，包含每期的还款金额、本金、利息等。
     * 
     * @param loanId 贷款ID
     * @return 还款计划列表
     */
    public List<LoanCalculator.PaymentScheduleItem> getPaymentSchedule(int loanId) {
        Loan loan = loanDao.getLoanById(loanId);
        if (loan == null) return new ArrayList<>();
        
        List<Payment> payments = paymentDao.getPaymentsByLoanId(loanId);
        return LoanCalculator.getPaymentSchedule(loan, payments);
    }
    
    /**
     * 获取总还款金额
     * 
     * @param loanId 贷款ID
     * @return 该贷款的累计还款总额
     */
    public double getTotalPaidByLoanId(int loanId) {
        return paymentDao.getTotalPaidByLoanId(loanId);
    }
    
    /**
     * 获取所有贷款统计信息
     * 
     * 汇总所有贷款的关键统计数据：总数、本金总额、剩余总额、已还总额、已还清数量。
     * 用于首页统计卡片显示。
     * 
     * @return 贷款统计信息对象
     */
    public LoanStatistics getStatistics() {
        List<LoanWithStatus> loans = getLoansWithStatus();
        
        int totalCount = loans.size();
        double totalPrincipal = 0;
        double totalRemaining = 0;
        double totalPaid = 0;
        int paidOffCount = 0;
        
        for (LoanWithStatus lws : loans) {
            totalPrincipal += lws.loan.getPrincipal();
            totalRemaining += lws.status.getRemainingPrincipal();
            totalPaid += lws.status.getTotalPaid();
            if (lws.status.isPaidOff()) {
                paidOffCount++;
            }
        }
        
        return new LoanStatistics(totalCount, totalPrincipal, totalRemaining, totalPaid, paidOffCount);
    }
    
    /**
     * 获取今日到期的信用卡
     * 
     * 筛选出还款日为今天且未还清的信用卡贷款。
     * 用于还款提醒功能。
     * 
     * @return 今日到期的信用卡列表
     */
    public List<Loan> getDueTodayLoans() {
        List<Loan> creditCards = loanDao.getCreditCardLoans();
        List<Loan> dueToday = new ArrayList<>();
        
        java.util.Calendar today = java.util.Calendar.getInstance();
        int todayDay = today.get(java.util.Calendar.DAY_OF_MONTH);
        
        for (Loan loan : creditCards) {
            if (loan.getDueDate() == todayDay) {
                // 检查是否已还清
                LoanStatus status = getLoanStatus(loan.getId());
                if (status != null && !status.isPaidOff()) {
                    dueToday.add(loan);
                }
            }
        }
        
        return dueToday;
    }
    
    // ==================== 异步操作方法 ====================
    
    /**
     * 异步插入贷款
     * 
     * 在后台线程执行插入操作，避免阻塞主线程。
     * 通过回调返回插入结果。
     * 
     * @param loan 要插入的贷款对象
     * @param callback 插入完成回调
     */
    public void insertLoanAsync(Loan loan, InsertCallback callback) {
        executorService.execute(() -> {
            // 计算原始月供
            double originalPayment = LoanCalculator.calculateMonthlyPayment(
                    loan.getPrincipal(),
                    loan.getAnnualRate(),
                    loan.getMonths(),
                    loan.getRepaymentMethod()
            );
            loan.setOriginalMonthlyPayment(originalPayment);
            long id = loanDao.insertLoan(loan);
            triggerAutoBackup();
            // 切换到主线程执行回调
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onComplete(id);
                }
            });
        });
    }
    
    /**
     * 异步插入还款记录
     * 
     * @param payment 要插入的还款记录
     * @param callback 插入完成回调
     */
    public void insertPaymentAsync(Payment payment, InsertCallback callback) {
        executorService.execute(() -> {
            long id = paymentDao.insertPayment(payment);
            triggerAutoBackup();
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onComplete(id);
                }
            });
        });
    }
    
    /**
     * 异步删除贷款
     * 
     * 同时删除该贷款的所有还款记录。
     * 
     * @param loanId 要删除的贷款ID
     * @param callback 删除完成回调
     */
    public void deleteLoanAsync(int loanId, DeleteCallback callback) {
        executorService.execute(() -> {
            // 先删除关联的还款记录
            paymentDao.deletePaymentsByLoanId(loanId);
            // 再删除贷款
            loanDao.deleteLoanById(loanId);
            triggerAutoBackup();
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onComplete();
                }
            });
        });
    }
    
    // ==================== 自动备份机制 ====================
    
    /** 防抖间隔时间（毫秒）- 避免频繁备份 */
    private static final long BACKUP_DEBOUNCE_MS = 3000; // 3秒防抖
    
    /** 上次备份时间戳 */
    private static long lastBackupTime = 0;
    
    /** 备份锁对象，确保线程安全 */
    private static final Object backupLock = new Object();
    
    /**
     * 触发自动备份（带防抖机制）
     * 
     * 防抖机制说明：
     *   - 如果距离上次备份不足3秒，跳过本次备份
     *   - 这样可以合并连续的数据操作，避免频繁备份
     * 
     * 备份流程：
     *   1. 检查是否满足防抖条件
     *   2. 延迟1秒执行（等待可能的其他操作）
     *   3. 调用 BackupManager 执行备份
     */
    private void triggerAutoBackup() {
        synchronized (backupLock) {
            long currentTime = System.currentTimeMillis();
            // 如果距离上次备份不足3秒，跳过
            if (currentTime - lastBackupTime < BACKUP_DEBOUNCE_MS) {
                android.util.Log.d("LoanRepository", "Auto backup skipped (debounce)");
                return;
            }
            lastBackupTime = currentTime;
        }
        
        executorService.execute(() -> {
            try {
                // 延迟执行，合并连续操作
                Thread.sleep(1000);
                
                BackupManager backupManager = new BackupManager(application);
                backupManager.performAutoBackup(new BackupManager.BackupCallback() {
                    @Override
                    public void onSuccess(String message) {
                        android.util.Log.d("LoanRepository", "Auto backup success: " + message);
                        backupManager.shutdown();
                    }
                    
                    @Override
                    public void onError(String error) {
                        android.util.Log.e("LoanRepository", "Auto backup failed: " + error);
                        backupManager.shutdown();
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("LoanRepository", "Auto backup exception: " + e.getMessage());
            }
        });
    }
    
    // ==================== 回调接口定义 ====================
    
    /**
     * 插入操作回调接口
     */
    public interface InsertCallback {
        /**
         * 插入完成回调
         * @param id 新插入记录的ID
         */
        void onComplete(long id);
    }
    
    /**
     * 删除操作回调接口
     */
    public interface DeleteCallback {
        /**
         * 删除完成回调
         */
        void onComplete();
    }
    
    // ==================== 数据类定义 ====================
    
    /**
     * 贷款与状态的关联类
     * 
     * 将贷款信息和计算后的状态组合在一起，
     * 方便在列表中同时访问两者。
     */
    public static class LoanWithStatus {
        /** 贷款信息 */
        public final Loan loan;
        
        /** 贷款状态 */
        public final LoanStatus status;
        
        public LoanWithStatus(Loan loan, LoanStatus status) {
            this.loan = loan;
            this.status = status;
        }
    }
    
    /**
     * 贷款统计信息类
     * 
     * 汇总所有贷款的关键统计数据，
     * 用于首页统计卡片显示。
     */
    public static class LoanStatistics {
        /** 贷款总数 */
        public final int totalCount;
        
        /** 本金总额 */
        public final double totalPrincipal;
        
        /** 剩余本金总额 */
        public final double totalRemaining;
        
        /** 已还总额 */
        public final double totalPaid;
        
        /** 已还清数量 */
        public final int paidOffCount;
        
        public LoanStatistics(int totalCount, double totalPrincipal, 
                              double totalRemaining, double totalPaid, int paidOffCount) {
            this.totalCount = totalCount;
            this.totalPrincipal = totalPrincipal;
            this.totalRemaining = totalRemaining;
            this.totalPaid = totalPaid;
            this.paidOffCount = paidOffCount;
        }
    }
}
