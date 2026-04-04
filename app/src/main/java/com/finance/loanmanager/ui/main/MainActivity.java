package com.finance.loanmanager.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    private CardView cardAddLoan;
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
    private ImageView btnVersionInfo;
    
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
        cardAddLoan = findViewById(R.id.cardAddLoan);
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
        btnVersionInfo = findViewById(R.id.btnVersionInfo);
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
        
        btnVersionInfo.setOnClickListener(v -> showMainMenu());
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
            cardAddLoan.setVisibility(View.VISIBLE);
            return;
        }
        
        cardCurrentMonth.setVisibility(View.VISIBLE);
        cardStats.setVisibility(View.VISIBLE);
        cardLoanManage.setVisibility(View.GONE); // 贷款管理功能已移到二级菜单
        cardAddLoan.setVisibility(View.GONE);
        
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
        
        // 贷款总数卡片 - 可点击打开贷款管理菜单
        View totalLoansCard = addStatCard(gridStats, String.valueOf(totalCount), getString(R.string.total_loans));
        totalLoansCard.setOnClickListener(v -> showLoanManagementMenu());
        totalLoansCard.setClickable(true);
        // 使用 TypedValue 正确获取属性对应的 drawable
        android.util.TypedValue outValue = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        totalLoansCard.setForeground(getDrawable(outValue.resourceId));
        
        addStatCard(gridStats, NumberFormatUtil.formatCurrency(totalPrincipal), getString(R.string.total_amount));
        addStatCard(gridStats, NumberFormatUtil.formatCurrency(totalRemaining), getString(R.string.remaining_principal));
        addStatCard(gridStats, NumberFormatUtil.formatCurrency(totalPaid), getString(R.string.total_paid));
    }
    
    private View addStatCard(GridLayout grid, String value, String label) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_stat_card, grid, false);
        TextView tvValue = cardView.findViewById(R.id.tvStatValue);
        TextView tvLabel = cardView.findViewById(R.id.tvStatLabel);
        tvValue.setText(value);
        tvLabel.setText(label);
        grid.addView(cardView);
        return cardView;
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
    
    /**
     * 显示主菜单对话框（一级菜单：版本说明、数据管理、添加贷款）
     * 当应用有数据时，显示添加贷款选项
     */
    private void showMainMenu() {
        boolean hasData = !loansWithStatus.isEmpty();
        String[] items = hasData 
                ? new String[]{"版本说明", "数据管理", "添加贷款"}
                : new String[]{"版本说明", "数据管理"};
        
        new AlertDialog.Builder(this)
                .setTitle("菜单")
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        showVersionInfo();
                    } else if (which == 1) {
                        showDataManagementMenu();
                    } else if (which == 2 && hasData) {
                        // 添加贷款
                        Intent intent = new Intent(this, AddLoanActivity.class);
                        startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * 显示数据管理二级菜单对话框
     */
    private void showDataManagementMenu() {
        String[] items = {"导出数据", "导入数据", "清空数据"};
        new AlertDialog.Builder(this)
                .setTitle(R.string.data_management)
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            exportData();
                            break;
                        case 1:
                            importData();
                            break;
                        case 2:
                            clearData();
                            break;
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * 显示贷款管理二级菜单对话框
     */
    private void showLoanManagementMenu() {
        String[] items = {"查看我的贷款", "添加新贷款"};
        new AlertDialog.Builder(this)
                .setTitle(R.string.loan_management)
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // 查看我的贷款
                            Intent viewIntent = new Intent(this, LoanListActivity.class);
                            startActivity(viewIntent);
                            break;
                        case 1:
                            // 添加新贷款
                            Intent addIntent = new Intent(this, AddLoanActivity.class);
                            startActivity(addIntent);
                            break;
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * 显示版本说明对话框
     */
    private void showVersionInfo() {
        String versionContent = readVersionNotesFromAssets();
        new AlertDialog.Builder(this)
                .setTitle(R.string.version_info_title)
                .setMessage(versionContent)
                .setPositiveButton(R.string.i_know, null)
                .show();
    }
    
    /**
     * 从 assets 读取版本说明文件
     */
    private String readVersionNotesFromAssets() {
        StringBuilder content = new StringBuilder();
        try {
            InputStream is = getAssets().open("version_notes.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
            is.close();
        } catch (IOException e) {
            return getString(R.string.version_info_load_failed);
        }
        return content.toString();
    }
}
