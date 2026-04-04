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
        
        // 閫傞厤 Android 15/16 Edge-to-Edge 瀹夊叏鍖哄煙
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
            
            // 妫€鏌ユ槸鍚﹂渶瑕佹樉绀哄浠芥仮澶嶅璇濇
            checkAndShowBackupRestoreDialog();
        } catch (Exception e) {
            Toast.makeText(this, "搴旂敤鍒濆鍖栧け璐? " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }
    
    /**
     * 妫€鏌ュ苟鏄剧ず澶囦唤鎭㈠瀵硅瘽妗?     */
    private void checkAndShowBackupRestoreDialog() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean alreadyChecked = prefs.getBoolean(KEY_BACKUP_CHECKED, false);
        
        // 濡傛灉宸茬粡妫€鏌ヨ繃锛屼笉鍐嶆樉绀?        if (alreadyChecked) {
            return;
        }
        
        // 妫€鏌ユ槸鍚﹀瓨鍦ㄥ浠芥枃浠?        if (backupManager.hasAutoBackup()) {
            // 寤惰繜涓€鐐规樉绀猴紝绛塙I瀹屽叏鍔犺浇
            findViewById(android.R.id.content).postDelayed(() -> {
                showBackupRestoreDialog();
            }, 500);
        }
        
        // 鏍囪宸叉鏌ワ紙鏃犺鏄惁鏈夊浠介兘鏍囪锛岄伩鍏嶅弽澶嶆鏌ワ級
        prefs.edit().putBoolean(KEY_BACKUP_CHECKED, true).apply();
    }
    
    /**
     * 鏄剧ず澶囦唤鎭㈠瀵硅瘽妗?     */
    private void showBackupRestoreDialog() {
        BackupRestoreDialog dialog = new BackupRestoreDialog();
        dialog.setRestoreListener(new BackupRestoreDialog.RestoreListener() {
            @Override
            public void onRestoreComplete(boolean success) {
                if (success) {
                    // 鎭㈠鎴愬姛鍚庡埛鏂版暟鎹?                    refreshData();
                }
            }
            
            @Override
            public void onDismiss() {
                // 瀵硅瘽妗嗗叧闂悗鐨勫鐞?            }
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
        // 鍦ㄥ悗鍙扮嚎绋嬫墽琛屾暟鎹簱鏌ヨ锛岄伩鍏嶄富绾跨▼闃诲
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
        cardLoanManage.setVisibility(View.GONE); // 璐锋绠＄悊鍔熻兘宸茬Щ鍒颁簩绾ц彍鍗?        cardAddLoan.setVisibility(View.GONE);
        
        // 璁＄畻缁熻鏁版嵁
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
        
        // 鏇存柊鏈湀搴旇繕鍗＄墖
        String currentMonth = DateUtil.formatMonthCN(DateUtil.getCurrentMonth());
        tvCurrentDate.setText(currentMonth + " " + getString(R.string.current_month_payment));
        tvCurrentMonthTotal.setText(NumberFormatUtil.formatCurrency(currentMonthTotal));
        tvActiveLoanCount.setText(activeCount + " 绗?);
        tvDailyPayment.setText(NumberFormatUtil.formatCurrency(currentMonthTotal / 30));
        tvRemainingDays.setText(DateUtil.getRemainingDaysInMonth() + " 澶?);
        tvCurrentMonthDetails.setText(detailsBuilder.length() > 0 ? 
                detailsBuilder.toString() : "鏈湀鏃犲緟杩樻椤?);
        
        // 鏇存柊缁熻鍗＄墖
        updateStatsGrid(totalCount, totalPrincipal, totalRemaining, totalPaid, paidOffCount);
    }
    
    private void updateStatsGrid(int totalCount, double totalPrincipal, 
                                  double totalRemaining, double totalPaid, int paidOffCount) {
        gridStats.removeAllViews();
        
        // 璐锋鎬绘暟鍗＄墖 - 鍙偣鍑绘墦寮€璐锋绠＄悊鑿滃崟
        View totalLoansCard = addStatCard(gridStats, String.valueOf(totalCount), getString(R.string.total_loans));
        totalLoansCard.setOnClickListener(v -> showLoanManagementMenu());
        totalLoansCard.setClickable(true);
        // 浣跨敤 TypedValue 姝ｇ‘鑾峰彇灞炴€у搴旂殑 drawable
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
    
    // 姝ゆ柟娉曞凡鍚堝苟鍒?refreshData() 涓紝淇濈暀绌哄疄鐜颁互鍏煎鏃т唬鐮?    private void checkDueDateReminders() {
        // 鎻愰啋閫昏緫宸插湪 refreshData 鐨勫洖璋冧腑澶勭悊
    }
    
    private void showReminderDialog(List<Loan> dueLoans) {
        StringBuilder message = new StringBuilder();
        message.append("浠婂ぉ鏄?").append(DateUtil.formatDateCN(DateUtil.getCurrentDate())).append("\n\n");
        message.append("鎮ㄦ湁 ").append(dueLoans.size()).append(" 绗斾俊鐢ㄥ崱璐﹀崟浠婃棩鍒版湡锛歕n\n");
        
        for (Loan loan : dueLoans) {
            LoanStatus status = repository.getLoanStatus(loan.getId());
            message.append(loan.getName())
                    .append("\n鏈湡搴旇繕锛?)
                    .append(NumberFormatUtil.formatCurrency(status != null ? status.getNewMonthlyPayment() : 0))
                    .append("\n\n");
        }
        
        message.append("璇峰強鏃惰繕娆撅紝閬垮厤浜х敓閫炬湡璐圭敤锛?);
        
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
     * 鏄剧ず涓昏彍鍗曞璇濇锛堜竴绾ц彍鍗曪細鐗堟湰璇存槑銆佹暟鎹鐞嗐€佹坊鍔犺捶娆撅級
     * 褰撳簲鐢ㄦ湁鏁版嵁鏃讹紝鏄剧ず娣诲姞璐锋閫夐」
     */
    private void showMainMenu() {
        boolean hasData = !loansWithStatus.isEmpty();
        String[] items = hasData 
                ? new String[]{"鐗堟湰璇存槑", "鏁版嵁绠＄悊", "娣诲姞璐锋"}
                : new String[]{"鐗堟湰璇存槑", "鏁版嵁绠＄悊"};
        
        new AlertDialog.Builder(this)
                .setTitle("鑿滃崟")
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        showVersionInfo();
                    } else if (which == 1) {
                        showDataManagementMenu();
                    } else if (which == 2 && hasData) {
                        // 娣诲姞璐锋
                        Intent intent = new Intent(this, AddLoanActivity.class);
                        startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * 鏄剧ず鏁版嵁绠＄悊浜岀骇鑿滃崟瀵硅瘽妗?     */
    private void showDataManagementMenu() {
        String[] items = {"瀵煎嚭鏁版嵁", "瀵煎叆鏁版嵁", "娓呯┖鏁版嵁"};
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
     * 鏄剧ず璐锋绠＄悊浜岀骇鑿滃崟瀵硅瘽妗?     */
    private void showLoanManagementMenu() {
        String[] items = {"鏌ョ湅鎴戠殑璐锋", "娣诲姞鏂拌捶娆?};
        new AlertDialog.Builder(this)
                .setTitle(R.string.loan_management)
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // 鏌ョ湅鎴戠殑璐锋
                            Intent viewIntent = new Intent(this, LoanListActivity.class);
                            startActivity(viewIntent);
                            break;
                        case 1:
                            // 娣诲姞鏂拌捶娆?                            Intent addIntent = new Intent(this, AddLoanActivity.class);
                            startActivity(addIntent);
                            break;
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * 鏄剧ず鐗堟湰璇存槑瀵硅瘽妗?     */
    private void showVersionInfo() {
        String versionContent = readVersionNotesFromAssets();
        new AlertDialog.Builder(this)
                .setTitle(R.string.version_info_title)
                .setMessage(versionContent)
                .setPositiveButton(R.string.i_know, null)
                .show();
    }
    
    /**
     * 浠?assets 璇诲彇鐗堟湰璇存槑鏂囦欢
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
