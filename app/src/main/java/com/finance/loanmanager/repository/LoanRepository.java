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
 * 贷款数据仓库
 * 封装数据访问逻辑，提供统一的接口
 */
public class LoanRepository {
    
    private final LoanDao loanDao;
    private final PaymentDao paymentDao;
    private final LiveData<List<Loan>> allLoans;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final Application application;
    
    public LoanRepository(Application application) {
        this.application = application;
        AppDatabase database = AppDatabase.getInstance(application);
        loanDao = database.loanDao();
        paymentDao = database.paymentDao();
        allLoans = loanDao.getAllLoansLive();
        executorService = Executors.newFixedThreadPool(4);
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    // ==================== 贷款操作 ====================
    
    public LiveData<List<Loan>> getAllLoans() {
        return allLoans;
    }
    
    public List<Loan> getAllLoansSync() {
        return loanDao.getAllLoans();
    }
    
    public Loan getLoanById(int loanId) {
        return loanDao.getLoanById(loanId);
    }
    
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
    
    public void updateLoan(Loan loan) {
        loanDao.updateLoan(loan);
        triggerAutoBackup();
    }
    
    public void deleteLoan(Loan loan) {
        loanDao.deleteLoan(loan);
        triggerAutoBackup();
    }
    
    public void deleteLoanById(int loanId) {
        loanDao.deleteLoanById(loanId);
        triggerAutoBackup();
    }
    
    public void deleteAllLoans() {
        loanDao.deleteAllLoans();
        triggerAutoBackup();
    }
    
    // ==================== 还款记录操作 ====================
    
    public LiveData<List<Payment>> getAllPayments() {
        return paymentDao.getAllPaymentsLive();
    }
    
    public List<Payment> getAllPaymentsSync() {
        return paymentDao.getAllPayments();
    }
    
    public List<Payment> getPaymentsByLoanId(int loanId) {
        return paymentDao.getPaymentsByLoanId(loanId);
    }
    
    public long insertPayment(Payment payment) {
        long id = paymentDao.insertPayment(payment);
        triggerAutoBackup();
        return id;
    }
    
    public void deletePayment(Payment payment) {
        paymentDao.deletePayment(payment);
        triggerAutoBackup();
    }
    
    public void deleteAllPayments() {
        paymentDao.deleteAllPayments();
        triggerAutoBackup();
    }
    
    // ==================== 业务方法 ====================
    
    /**
     * 获取贷款及其状态
     */
    public List<LoanWithStatus> getLoansWithStatus() {
        List<LoanWithPayments> loanWithPaymentsList = loanDao.getAllLoansWithPayments();
        List<LoanWithStatus> result = new ArrayList<>();
        
        for (LoanWithPayments lwp : loanWithPaymentsList) {
            LoanStatus status = LoanCalculator.calculateRemainingLoan(
                    lwp.getLoan(), lwp.getPayments());
            result.add(new LoanWithStatus(lwp.getLoan(), status));
        }
        
        return result;
    }
    
    /**
     * 获取单个贷款的状态
     */
    public LoanStatus getLoanStatus(int loanId) {
        Loan loan = loanDao.getLoanById(loanId);
        if (loan == null) return null;
        
        List<Payment> payments = paymentDao.getPaymentsByLoanId(loanId);
        return LoanCalculator.calculateRemainingLoan(loan, payments);
    }
    
    /**
     * 获取还款计划
     */
    public List<LoanCalculator.PaymentScheduleItem> getPaymentSchedule(int loanId) {
        Loan loan = loanDao.getLoanById(loanId);
        if (loan == null) return new ArrayList<>();
        
        List<Payment> payments = paymentDao.getPaymentsByLoanId(loanId);
        return LoanCalculator.getPaymentSchedule(loan, payments);
    }
    
    /**
     * 获取总还款金额
     */
    public double getTotalPaidByLoanId(int loanId) {
        return paymentDao.getTotalPaidByLoanId(loanId);
    }
    
    /**
     * 获取所有贷款统计信息
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
    
    // ==================== 异步操作 ====================
    
    public void insertLoanAsync(Loan loan, InsertCallback callback) {
        executorService.execute(() -> {
            double originalPayment = LoanCalculator.calculateMonthlyPayment(
                    loan.getPrincipal(),
                    loan.getAnnualRate(),
                    loan.getMonths(),
                    loan.getRepaymentMethod()
            );
            loan.setOriginalMonthlyPayment(originalPayment);
            long id = loanDao.insertLoan(loan);
            triggerAutoBackup();
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onComplete(id);
                }
            });
        });
    }
    
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
    
    public void deleteLoanAsync(int loanId, DeleteCallback callback) {
        executorService.execute(() -> {
            paymentDao.deletePaymentsByLoanId(loanId);
            loanDao.deleteLoanById(loanId);
            triggerAutoBackup();
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onComplete();
                }
            });
        });
    }
    
    // ==================== 自动备份 ====================
    
    /**
     * 触发自动备份（静默执行，不显示提示）
     */
    private void triggerAutoBackup() {
        try {
            BackupManager backupManager = new BackupManager(application);
            backupManager.performAutoBackup(new BackupManager.BackupCallback() {
                @Override
                public void onSuccess(String message) {
                    // 静默备份成功，不显示提示
                    android.util.Log.d("LoanRepository", "Auto backup success: " + message);
                    backupManager.shutdown();
                }
                
                @Override
                public void onError(String error) {
                    // 静默备份失败，记录日志
                    android.util.Log.e("LoanRepository", "Auto backup failed: " + error);
                    backupManager.shutdown();
                }
            });
        } catch (Exception e) {
            android.util.Log.e("LoanRepository", "Auto backup exception: " + e.getMessage());
        }
    }
    
    // ==================== 回调接口 ====================
    
    public interface InsertCallback {
        void onComplete(long id);
    }
    
    public interface DeleteCallback {
        void onComplete();
    }
    
    // ==================== 数据类 ====================
    
    public static class LoanWithStatus {
        public final Loan loan;
        public final LoanStatus status;
        
        public LoanWithStatus(Loan loan, LoanStatus status) {
            this.loan = loan;
            this.status = status;
        }
    }
    
    public static class LoanStatistics {
        public final int totalCount;
        public final double totalPrincipal;
        public final double totalRemaining;
        public final double totalPaid;
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
