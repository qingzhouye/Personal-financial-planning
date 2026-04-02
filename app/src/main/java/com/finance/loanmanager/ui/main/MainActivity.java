package com.finance.loanmanager.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.finance.loanmanager.R;
import com.finance.loanmanager.data.entity.Loan;
import com.finance.loanmanager.data.entity.LoanStatus;
import com.finance.loanmanager.repository.LoanRepository;
import com.finance.loanmanager.ui.data.BackupRestoreDialog;
import com.finance.loanmanager.ui.data.DataManagementActivity;
import com.finance.loanmanager.ui.loan.AddLoanActivity;
import com.finance.loanmanager.ui.loan.LoanListActivity;
import com.finance.loanmanager.ui.monthly.MonthlyTotalActivity;
import com.finance.loanmanager.util.BackupManager;
import com.finance.loanmanager.util.DateUtil;
import com.finance.loanmanager.util.NumberFormatUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.content.SharedPreferences;

public class MainActivity extends AppCompatActivity {

    private LoanRepository repository;
    private ExecutorService executorService;
    
    private CardView cardCurrentMonth;
    private CardView cardStats;
    private CardView cardLoanManage;
    private TextView tvCurrentDate;
    private TextView tvCurrentMonthTotal;
    private TextView tvActiveLoanCount;
    private TextView tvDailyPayment;
    private TextView tvRemainingDays;
    private TextView tvCurrentMonthDetails;
    private GridLayout gridStats;
    private Button btnAddLoan;
    private Button btnViewLoans;
    private Button btnViewMonthlyTotal;
    private Button btnExport;
    private Button btnImport;
    private Button btnClear;
    
    private List<LoanRepository.LoanWithStatus> loansWithStatus = new ArrayList<>();
    private BackupManager backupManager;
    private static final String PREFS_NAME = "LoanManagerPrefs";
    private static final String KEY_BACKUP_CHECKED = "backup_checked";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 适配 Android 15/16 Edge-to-Edge 安全区域
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        try {
            repository = new LoanRepository(getApplication());
            backupManager = new BackupManager(this);
            executorService = Executors.newSingleThreadExecutor();
            initViews();
            setupListeners();
            observeData();
            
            // 检查是否需要显示备份恢复对话框
            checkAndShowBackupRestoreDialog();
        } catch (Exception e) {
            Toast.makeText(this, "应用初始化失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }
    
    /**
     * 检查并显示备份恢复对话框
     */
    private void checkAndShowBackupRestoreDialog() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean alreadyChecked = prefs.getBoolean(KEY_BACKUP_CHECKED, false);
        
        // 如果已经检查过，不再显示
        if (alreadyChecked) {
            return;
        }
        
        // 检查是否存在备份文件
        if (backupManager.hasAutoBackup()) {
            // 延迟一点显示，等UI完全加载
            findViewById(android.R.id.content).postDelayed(() -> {
                showBackupRestoreDialog();
            }, 500);
        }
        
        // 标记已检查（无论是否有备份都标记，避免反复检查）
        prefs.edit().putBoolean(KEY_BACKUP_CHECKED, true).apply();
    }
    
    /**
     * 显示备份恢复对话框
     */
    private void showBackupRestoreDialog() {
        BackupRestoreDialog dialog = new BackupRestoreDialog();
        dialog.setRestoreListener(new BackupRestoreDialog.RestoreListener() {
            @Override
            public void onRestoreComplete(boolean success) {
                if (success) {
                    // 恢复成功后刷新数据
                    refreshData();
                }
            }
            
            @Override
            public void onDismiss() {
                // 对话框关闭后的处理
            }
        });
        dialog.show(getSupportFragmentManager(), "BackupRestoreDialog");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (backupManager != null) {
            backupManager.shutdown();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        refreshData();
    }
    
    private void initViews() {
        cardCurrentMonth = findViewById(R.id.cardCurrentMonth);
        cardStats = findViewById(R.id.cardStats);
        cardLoanManage = findViewById(R.id.cardLoanManage);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        tvCurrentMonthTotal = findViewById(R.id.tvCurrentMonthTotal);
        tvActiveLoanCount = findViewById(R.id.tvActiveLoanCount);
        tvDailyPayment = findViewById(R.id.tvDailyPayment);
        tvRemainingDays = findViewById(R.id.tvRemainingDays);
        tvCurrentMonthDetails = findViewById(R.id.tvCurrentMonthDetails);
        gridStats = findViewById(R.id.gridStats);
        btnAddLoan = findViewById(R.id.btnAddLoan);
        btnViewLoans = findViewById(R.id.btnViewLoans);
        btnViewMonthlyTotal = findViewById(R.id.btnViewMonthlyTotal);
        btnExport = findViewById(R.id.btnExport);
        btnImport = findViewById(R.id.btnImport);
        btnClear = findViewById(R.id.btnClear);
    }
    
    private void setupListeners() {
        btnAddLoan.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddLoanActivity.class);
            startActivity(intent);
        });
        
        btnViewLoans.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoanListActivity.class);
            startActivity(intent);
        });
        
        btnViewMonthlyTotal.setOnClickListener(v -> {
            Intent intent = new Intent(this, MonthlyTotalActivity.class);
            startActivity(intent);
        });
        
        btnExport.setOnClickListener(v -> exportData());
        btnImport.setOnClickListener(v -> importData());
        btnClear.setOnClickListener(v -> clearData());
    }
    
    private void observeData() {
        LiveData<List<Loan>> loansLive = repository.getAllLoans();
        loansLive.observe(this, new Observer<List<Loan>>() {
            @Override
            public void onChanged(List<Loan> loans) {
                refreshData();
            }
        });
    }
    
    private void refreshData() {
        // 在后台线程执行数据库查询，避免主线程阻塞
        executorService.execute(() -> {
            final List<LoanRepository.LoanWithStatus> result = repository.getLoansWithStatus();
            final List<Loan> dueTodayLoans = repository.getDueTodayLoans();
            runOnUiThread(() -> {
                loansWithStatus = result;
                updateUI();
                if (!dueTodayLoans.isEmpty()) {
                    showReminderDialog(dueTodayLoans);
                }
            });
        });
    }
    
    private void updateUI() {
        if (loansWithStatus.isEmpty()) {
            cardCurrentMonth.setVisibility(View.GONE);
            cardStats.setVisibility(View.GONE);
            cardLoanManage.setVisibility(View.GONE);
            return;
        }
        
        cardCurrentMonth.setVisibility(View.VISIBLE);
        cardStats.setVisibility(View.VISIBLE);
        cardLoanManage.setVisibility(View.VISIBLE);
        
        // 计算统计数据
        int totalCount = loansWithStatus.size();
        double totalPrincipal = 0;
        double totalRemaining = 0;
        double totalPaid = 0;
        int paidOffCount = 0;
        double currentMonthTotal = 0;
        int activeCount = 0;
        StringBuilder detailsBuilder = new StringBuilder();
        
        for (LoanRepository.LoanWithStatus lws : loansWithStatus) {
            totalPrincipal += lws.loan.getPrincipal();
            totalRemaining += lws.status.getRemainingPrincipal();
            totalPaid += lws.status.getTotalPaid();
            
            if (lws.status.isPaidOff()) {
                paidOffCount++;
            } else {
                activeCount++;
                currentMonthTotal += lws.status.getNewMonthlyPayment();
                if (detailsBuilder.length() > 0) {
                    detailsBuilder.append(" | ");
                }
                detailsBuilder.append(lws.loan.getName())
                        .append(": ")
                        .append(NumberFormatUtil.formatCurrency(lws.status.getNewMonthlyPayment()));
            }
        }
        
        // 更新本月应还卡片
        String currentMonth = DateUtil.formatMonthCN(DateUtil.getCurrentMonth());
        tvCurrentDate.setText(currentMonth + " " + getString(R.string.current_month_payment));
        tvCurrentMonthTotal.setText(NumberFormatUtil.formatCurrency(currentMonthTotal));
        tvActiveLoanCount.setText(activeCount + " 笔");
        tvDailyPayment.setText(NumberFormatUtil.formatCurrency(currentMonthTotal / 30));
        tvRemainingDays.setText(DateUtil.getRemainingDaysInMonth() + " 天");
        tvCurrentMonthDetails.setText(detailsBuilder.length() > 0 ? 
                detailsBuilder.toString() : "本月无待还款项");
        
        // 更新统计卡片
        updateStatsGrid(totalCount, totalPrincipal, totalRemaining, totalPaid, paidOffCount);
    }
    
    private void updateStatsGrid(int totalCount, double totalPrincipal, 
                                  double totalRemaining, double totalPaid, int paidOffCount) {
        gridStats.removeAllViews();
        
        addStatCard(gridStats, String.valueOf(totalCount), getString(R.string.total_loans));
        addStatCard(gridStats, NumberFormatUtil.formatCurrency(totalPrincipal), getString(R.string.total_amount));
        addStatCard(gridStats, NumberFormatUtil.formatCurrency(totalRemaining), getString(R.string.remaining_principal));
        addStatCard(gridStats, NumberFormatUtil.formatCurrency(totalPaid), getString(R.string.total_paid));
    }
    
    private void addStatCard(GridLayout grid, String value, String label) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_stat_card, grid, false);
        TextView tvValue = cardView.findViewById(R.id.tvStatValue);
        TextView tvLabel = cardView.findViewById(R.id.tvStatLabel);
        tvValue.setText(value);
        tvLabel.setText(label);
        grid.addView(cardView);
    }
    
    // 此方法已合并到 refreshData() 中，保留空实现以兼容旧代码
    private void checkDueDateReminders() {
        // 提醒逻辑已在 refreshData 的回调中处理
    }
    
    private void showReminderDialog(List<Loan> dueLoans) {
        StringBuilder message = new StringBuilder();
        message.append("今天是 ").append(DateUtil.formatDateCN(DateUtil.getCurrentDate())).append("\n\n");
        message.append("您有 ").append(dueLoans.size()).append(" 笔信用卡账单今日到期：\n\n");
        
        for (Loan loan : dueLoans) {
            LoanStatus status = repository.getLoanStatus(loan.getId());
            message.append(loan.getName())
                    .append("\n本期应还：")
                    .append(NumberFormatUtil.formatCurrency(status != null ? status.getNewMonthlyPayment() : 0))
                    .append("\n\n");
        }
        
        message.append("请及时还款，避免产生逾期费用！");
        
        new AlertDialog.Builder(this)
                .setTitle(R.string.reminder_title)
                .setMessage(message.toString())
                .setPositiveButton(R.string.i_know, null)
                .show();
    }
    
    private void exportData() {
        Intent intent = new Intent(this, DataManagementActivity.class);
        intent.setAction("EXPORT");
        startActivity(intent);
    }
    
    private void importData() {
        Intent intent = new Intent(this, DataManagementActivity.class);
        intent.setAction("IMPORT");
        startActivity(intent);
    }
    
    private void clearData() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_data)
                .setMessage(R.string.clear_confirm)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.clear_data)
                            .setMessage(R.string.clear_confirm_again)
                            .setPositiveButton(R.string.confirm, (d, w) -> {
                                repository.deleteAllPayments();
                                repository.deleteAllLoans();
                                Toast.makeText(this, R.string.clear_success, Toast.LENGTH_SHORT).show();
                                refreshData();
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
