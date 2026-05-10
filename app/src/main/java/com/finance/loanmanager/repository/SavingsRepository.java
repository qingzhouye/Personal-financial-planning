package com.finance.loanmanager.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.finance.loanmanager.data.AppDatabase;
import com.finance.loanmanager.data.dao.SavingsDao;
import com.finance.loanmanager.data.entity.Savings;
import com.finance.loanmanager.util.BackupManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 储蓄数据仓库
 */
public class SavingsRepository {

    private final SavingsDao savingsDao;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final Application application;

    public SavingsRepository(Application application) {
        this.application = application;
        AppDatabase database = AppDatabase.getInstance(application);
        savingsDao = database.savingsDao();
        executorService = Executors.newFixedThreadPool(4);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    // ==================== 查询操作 ====================

    public LiveData<List<Savings>> getAllSavingsLive() {
        return savingsDao.getAllSavingsLive();
    }

    public List<Savings> getAllSavingsSync() {
        return savingsDao.getAllSavings();
    }

    public List<Savings> getRecentSavings(int limit) {
        return savingsDao.getRecentSavings(limit);
    }

    public Savings getSavingsById(int id) {
        return savingsDao.getSavingsById(id);
    }

    public double getTotalBalance() {
        return savingsDao.getTotalBalance();
    }

    public double getTotalDeposit() {
        return savingsDao.getTotalDeposit();
    }

    public double getTotalWithdraw() {
        return savingsDao.getTotalWithdraw();
    }

    public List<Savings> getAllSavingsForChart() {
        return savingsDao.getAllSavingsAsc();
    }

    // ==================== 同步写操作 ====================

    public long insertSavings(Savings savings) {
        long id = savingsDao.insertSavings(savings);
        triggerAutoBackup();
        return id;
    }

    public void updateSavings(Savings savings) {
        savingsDao.updateSavings(savings);
        triggerAutoBackup();
    }

    public void deleteSavings(Savings savings) {
        savingsDao.deleteSavings(savings);
        triggerAutoBackup();
    }

    public void deleteSavingsById(int id) {
        savingsDao.deleteSavingsById(id);
        triggerAutoBackup();
    }

    public void deleteAllSavings() {
        savingsDao.deleteAllSavings();
    }

    // ==================== 异步操作 ====================

    public void insertSavingsAsync(Savings savings, InsertCallback callback) {
        executorService.execute(() -> {
            long id = savingsDao.insertSavings(savings);
            triggerAutoBackup();
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onComplete(id);
                }
            });
        });
    }

    public void updateSavingsAsync(Savings savings, UpdateCallback callback) {
        executorService.execute(() -> {
            savingsDao.updateSavings(savings);
            triggerAutoBackup();
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onComplete();
                }
            });
        });
    }

    public void deleteSavingsAsync(int id, DeleteCallback callback) {
        executorService.execute(() -> {
            savingsDao.deleteSavingsById(id);
            triggerAutoBackup();
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onComplete();
                }
            });
        });
    }

    // ==================== 自动备份 ====================

    private static final long BACKUP_DEBOUNCE_MS = 3000;
    private static long lastBackupTime = 0;
    private static final Object backupLock = new Object();

    private void triggerAutoBackup() {
        synchronized (backupLock) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastBackupTime < BACKUP_DEBOUNCE_MS) {
                return;
            }
            lastBackupTime = currentTime;
        }

        executorService.execute(() -> {
            try {
                Thread.sleep(1000);
                BackupManager backupManager = new BackupManager(application);
                backupManager.performAutoBackup(new BackupManager.BackupCallback() {
                    @Override
                    public void onSuccess(String message) {
                        backupManager.shutdown();
                    }

                    @Override
                    public void onError(String error) {
                        android.util.Log.e("SavingsRepository", "Auto backup failed: " + error);
                        backupManager.shutdown();
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("SavingsRepository", "Auto backup exception: " + e.getMessage());
            }
        });
    }

    // ==================== 回调接口 ====================

    public interface InsertCallback {
        void onComplete(long id);
    }

    public interface UpdateCallback {
        void onComplete();
    }

    public interface DeleteCallback {
        void onComplete();
    }
}
