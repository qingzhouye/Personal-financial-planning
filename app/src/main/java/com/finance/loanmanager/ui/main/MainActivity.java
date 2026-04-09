package com.finance.loanmanager.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
import com.finance.loanmanager.ui.BaseActivity;
import com.finance.loanmanager.ui.data.BackupRestoreDialog;
import com.finance.loanmanager.ui.data.DataManagementActivity;
import com.finance.loanmanager.ui.loan.AddLoanActivity;
import com.finance.loanmanager.ui.monthly.MonthlyTotalActivity;
import com.finance.loanmanager.ui.settings.BackgroundSettingsActivity;
import com.finance.loanmanager.util.BackupManager;
import com.finance.loanmanager.util.DateUtil;
import com.finance.loanmanager.util.NumberFormatUtil;
import com.finance.loanmanager.util.BackgroundManager;
import com.finance.loanmanager.util.ThemeManager;

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
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.widget.ScrollView;

public class MainActivity extends BaseActivity {

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
    private Button btnViewMonthlyTotal;
    private ImageView btnVersionInfo;
    private LinearLayout layoutDailyPayment;
    
    private List<LoanRepository.LoanWithStatus> loansWithStatus = new ArrayList<>();
    private BackupManager backupManager;
    private BackgroundManager backgroundManager;
    private static final String PREFS_NAME = "LoanManagerPrefs";
    private static final String KEY_BACKUP_CHECKED = "backup_checked";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 隐藏 ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_main);
        
        // 设置本月应还卡片背景为当前主题色渐变
        setupCurrentMonthCardBackground();
        
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
            backgroundManager = new BackgroundManager(this);
            executorService = Executors.newSingleThreadExecutor();
            
            // 初始化当前主题标记
            lastKnownTheme = ThemeManager.getSavedTheme(this);
            
            initViews();
            setupListeners();
            observeData();
            
            // 【关键修复】在 onCreate 中立即应用透明卡片样式，确保自定义背景下内容正确显示
            // 这样在数据加载完成前，样式就已经正确设置
            applyTransparentCardStyle();
            
            // 同步加载数据避免概览区域短暂空白
            // 无论是首次启动还是 Activity 重建，都尝试同步加载一次数据
            try {
                loansWithStatus = repository.getLoansWithStatus();
                updateUI();
            } catch (Exception ignored) {
                // 同步加载失败时会由 onResume 中的异步 refreshData 备底
            }
            
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
        
        // 【优先检测主题变化】如果主题已改变，立即 recreate 并跳过后续旧主题渲染
        int currentTheme = ThemeManager.getSavedTheme(this);
        if (lastKnownTheme != -1 && lastKnownTheme != currentTheme) {
            lastKnownTheme = currentTheme;
            recreate();
            return;
        }
        lastKnownTheme = currentTheme;
        
        // 重新设置卡片背景（主题可能已更改）
        setupCurrentMonthCardBackground();
        // 应用自定义背景模式下的透明卡片样式
        applyTransparentCardStyle();
        // 刷新按钮和统计卡片背景
        refreshThemeDependentViews();
        refreshData();
    }
    
    /**
     * 当设置了自定义背景时，应用透明卡片样式
     */
    private void applyTransparentCardStyle() {
        boolean hasCustomBg = backgroundManager != null && backgroundManager.hasCustomBackground();
            
        // 设置本月应还卡片样式
        if (cardCurrentMonth != null) {
            LinearLayout cardContent = (LinearLayout) cardCurrentMonth.getChildAt(0);
            if (cardContent != null) {
                if (hasCustomBg) {
                    cardCurrentMonth.setCardBackgroundColor(getResources().getColor(android.R.color.transparent));
                    cardCurrentMonth.setCardElevation(0);
                    cardContent.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                    applyWhiteTextStyleToCurrentMonthCard();
                } else {
                    cardCurrentMonth.setCardBackgroundColor(getResources().getColor(R.color.card_background));
                    cardCurrentMonth.setCardElevation(getResources().getDimension(R.dimen.card_elevation));
                    setupCurrentMonthCardBackground();
                    applyDefaultTextStyleToCurrentMonthCard();
                }
            }
        }
        
        // 设置每日应还区域背景色
        if (layoutDailyPayment != null) {
            if (hasCustomBg) {
                // 自定义背景模式：更深的半透明灰色背景，提高白字对比度
                layoutDailyPayment.setBackgroundColor(0x80000000); // 50% 透明度的黑色
            } else {
                // 普通模式：恢复半透明白色
                layoutDailyPayment.setBackgroundColor(getResources().getColor(R.color.semi_transparent_white));
            }
        }
            
        // 设置统计概览卡片样式
        if (cardStats != null) {
            if (hasCustomBg) {
                cardStats.setCardBackgroundColor(getResources().getColor(android.R.color.transparent));
                cardStats.setCardElevation(0);
                // 设置标题为白色加粗
                TextView titleView = findViewById(R.id.tvLoanOverviewTitle);
                if (titleView != null) {
                    titleView.setTextColor(getResources().getColor(R.color.text_white));
                    titleView.setTypeface(null, Typeface.BOLD);
                }
                // 自定义背景模式："查看每月总还款金额"按钮改为透明背景+白色边框
                if (btnViewMonthlyTotal != null) {
                    btnViewMonthlyTotal.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        getResources().getColor(android.R.color.transparent)));
                    // 添加白色边框
                    GradientDrawable drawable = new GradientDrawable();
                    drawable.setColor(getResources().getColor(android.R.color.transparent));
                    drawable.setStroke(3, getResources().getColor(R.color.text_white));
                    drawable.setCornerRadius(50f);
                    btnViewMonthlyTotal.setBackground(drawable);
                    btnViewMonthlyTotal.setTextColor(getResources().getColor(R.color.text_white));
                }
            } else {
                cardStats.setCardBackgroundColor(getResources().getColor(R.color.card_background));
                cardStats.setCardElevation(getResources().getDimension(R.dimen.card_elevation));
                // 恢夌标题默认样式
                TextView titleView = findViewById(R.id.tvLoanOverviewTitle);
                if (titleView != null) {
                    titleView.setTextColor(getResources().getColor(R.color.primary_dark));
                    titleView.setTypeface(null, Typeface.BOLD);
                }
                // 恢夌按钮默认样式
                if (btnViewMonthlyTotal != null) {
                    android.util.TypedValue typedValue = new android.util.TypedValue();
                    getTheme().resolveAttribute(android.R.attr.colorPrimary, typedValue, true);
                    int primaryColor = typedValue.data;
                    btnViewMonthlyTotal.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primaryColor));
                    btnViewMonthlyTotal.setBackgroundResource(R.drawable.gradient_primary);
                    btnViewMonthlyTotal.setTextColor(getResources().getColor(R.color.text_white));
                }
            }
            
            // 刷新统计网格中的卡片样式（当从背景设置页面返回时需要重新创建统计卡片）
            if (!loansWithStatus.isEmpty()) {
                refreshStatsGrid();
            }
        }
            
        // 设置添加新贷款卡片样式（无数据时显示）
        if (cardAddLoan != null) {
            if (hasCustomBg) {
                cardAddLoan.setCardBackgroundColor(getResources().getColor(android.R.color.transparent));
                cardAddLoan.setCardElevation(0);
                // 设置标题文字为白色加粗
                TextView addLoanTitle = cardAddLoan.findViewWithTag("add_loan_title");
                // 直接遍历子view处理
                LinearLayout addLoanContent = (LinearLayout) cardAddLoan.getChildAt(0);
                if (addLoanContent != null) {
                    for (int i = 0; i < addLoanContent.getChildCount(); i++) {
                        View child = addLoanContent.getChildAt(i);
                        if (child instanceof TextView) {
                            ((TextView) child).setTextColor(getResources().getColor(R.color.text_white));
                            ((TextView) child).setTypeface(null, Typeface.BOLD);
                        }
                    }
                }
            } else {
                cardAddLoan.setCardBackgroundColor(getResources().getColor(R.color.card_background));
                cardAddLoan.setCardElevation(getResources().getDimension(R.dimen.card_elevation));
                // 恢夌标题文字默认样式
                LinearLayout addLoanContent = (LinearLayout) cardAddLoan.getChildAt(0);
                if (addLoanContent != null) {
                    for (int i = 0; i < addLoanContent.getChildCount(); i++) {
                        View child = addLoanContent.getChildAt(i);
                        if (child instanceof TextView) {
                            ((TextView) child).setTextColor(getResources().getColor(R.color.primary_dark));
                            ((TextView) child).setTypeface(null, Typeface.BOLD);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 为本月应还卡片应用白色加粗文字样式（自定义背景模式）
     */
    private void applyWhiteTextStyleToCurrentMonthCard() {
        int whiteColor = getResources().getColor(R.color.text_white);
        
        if (tvCurrentDate != null) {
            tvCurrentDate.setTextColor(whiteColor);
            tvCurrentDate.setTypeface(null, Typeface.BOLD);
        }
        if (tvCurrentMonthTotal != null) {
            tvCurrentMonthTotal.setTextColor(whiteColor);
            tvCurrentMonthTotal.setTypeface(null, Typeface.BOLD);
        }
        if (tvActiveLoanCount != null) {
            tvActiveLoanCount.setTextColor(whiteColor);
            tvActiveLoanCount.setTypeface(null, Typeface.BOLD);
        }
        if (tvDailyPayment != null) {
            tvDailyPayment.setTextColor(whiteColor);
            tvDailyPayment.setTypeface(null, Typeface.BOLD);
        }
        if (tvRemainingDays != null) {
            tvRemainingDays.setTextColor(whiteColor);
            tvRemainingDays.setTypeface(null, Typeface.BOLD);
        }
        if (tvCurrentMonthDetails != null) {
            tvCurrentMonthDetails.setTextColor(whiteColor);
        }
    }
    
    /**
     * 恢复本月应还卡片的默认文字样式
     */
    private void applyDefaultTextStyleToCurrentMonthCard() {
        int semiTransparentWhite = getResources().getColor(R.color.semi_transparent_white);
        int whiteColor = getResources().getColor(R.color.text_white);
        
        if (tvCurrentDate != null) {
            tvCurrentDate.setTextColor(semiTransparentWhite);
            tvCurrentDate.setTypeface(null, Typeface.NORMAL);
        }
        if (tvCurrentMonthTotal != null) {
            tvCurrentMonthTotal.setTextColor(whiteColor);
            tvCurrentMonthTotal.setTypeface(null, Typeface.BOLD);
        }
        if (tvActiveLoanCount != null) {
            tvActiveLoanCount.setTextColor(whiteColor);
            tvActiveLoanCount.setTypeface(null, Typeface.BOLD);
        }
        if (tvDailyPayment != null) {
            tvDailyPayment.setTextColor(whiteColor);
            tvDailyPayment.setTypeface(null, Typeface.BOLD);
        }
        if (tvRemainingDays != null) {
            tvRemainingDays.setTextColor(whiteColor);
            tvRemainingDays.setTypeface(null, Typeface.BOLD);
        }
        if (tvCurrentMonthDetails != null) {
            tvCurrentMonthDetails.setTextColor(semiTransparentWhite);
        }
    }
    
    /**
     * 刷新依赖主题颜色的视图
     */
    private void refreshThemeDependentViews() {
        // 获取当前主题的 colorPrimary 颜色
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.colorPrimary, typedValue, true);
        int primaryColor = typedValue.data;
        
        // 如果没有自定义背景，才刷新按钮为主题色
        boolean hasCustomBg = backgroundManager != null && backgroundManager.hasCustomBackground();
        
        // 刷新"查看每月总还款金额"按钮背景
        if (btnViewMonthlyTotal != null && !hasCustomBg) {
            btnViewMonthlyTotal.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primaryColor));
            btnViewMonthlyTotal.setBackgroundResource(R.drawable.gradient_primary);
            btnViewMonthlyTotal.setTextColor(getResources().getColor(R.color.text_white));
        }
        
        // 刷新添加贷款按钮背景
        if (btnAddLoan != null) {
            btnAddLoan.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primaryColor));
        }
    }
    
    private int lastKnownTheme = -1;
    
    /**
     * 设置本月应还卡片背景为当前主题色的渐变
     */
    private void setupCurrentMonthCardBackground() {
        CardView cardCurrentMonth = findViewById(R.id.cardCurrentMonth);
        if (cardCurrentMonth != null) {
            int themeIndex = ThemeManager.getSavedTheme(this);
            
            // 获取主题对应的渐变颜色数组
            int[] gradientColors = getThemeGradientColors(themeIndex);
            
            // 创建渐变背景
            GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                gradientColors
            );
            gradient.setCornerRadius(24f); // 12dp * 2 for better quality
            
            // 设置卡片内容背景
            LinearLayout cardContent = (LinearLayout) cardCurrentMonth.getChildAt(0);
            if (cardContent != null) {
                cardContent.setBackground(gradient);
            }
        }
    }
    
    /**
     * 获取主题对应的渐变颜色数组
     */
    private int[] getThemeGradientColors(int themeIndex) {
        switch (themeIndex) {
            case ThemeManager.THEME_BLUE:
                return new int[] { 0xFF42A5F5, 0xFF64B5F6, 0xFF90CAF9 }; // 深蓝渐变（柔和）
            case ThemeManager.THEME_ORANGE:
                return new int[] { 0xFFFFA726, 0xFFFFB74D, 0xFFFFCC80 }; // 橙色渐变（柔和）
            case ThemeManager.THEME_PURPLE:
                return new int[] { 0xFFAB47BC, 0xFFBA68C8, 0xFFCE93D8 }; // 紫色渐变（柔和）
            case ThemeManager.THEME_GREEN:
                return new int[] { 0xFF66BB6A, 0xFF81C784, 0xFFA5D6A7 }; // 绿色渐变（柔和）
            case ThemeManager.THEME_ROSE:
                return new int[] { 0xFFEC407A, 0xFFF06292, 0xFFF48FB1 }; // 玫瑰渐变（柔和）
            case ThemeManager.THEME_CYAN:
            default:
                return new int[] { 0xFF4DD0E1, 0xFF63D7E5, 0xFF80DEEA }; // 青色渐变（柔和）
        }
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
        btnViewMonthlyTotal = findViewById(R.id.btnViewMonthlyTotal);
        btnVersionInfo = findViewById(R.id.btnVersionInfo);
        layoutDailyPayment = findViewById(R.id.layoutDailyPayment);
    }
    
    private void setupListeners() {
        btnAddLoan.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddLoanActivity.class);
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
    
    /**
     * 刷新统计网格（用于主题或背景切换后重新创建统计卡片）
     */
    private void refreshStatsGrid() {
        // 计算统计数据
        int totalCount = loansWithStatus.size();
        double totalPrincipal = 0;
        double totalRemaining = 0;
        double totalPaid = 0;
        int paidOffCount = 0;
        
        for (LoanRepository.LoanWithStatus lws : loansWithStatus) {
            totalPrincipal += lws.loan.getPrincipal();
            totalRemaining += lws.status.getRemainingPrincipal();
            totalPaid += lws.status.getTotalPaid();
            
            if (lws.status.isPaidOff()) {
                paidOffCount++;
            }
        }
        
        updateStatsGrid(totalCount, totalPrincipal, totalRemaining, totalPaid, paidOffCount);
    }
    
    private View addStatCard(GridLayout grid, String value, String label) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_stat_card, grid, false);
        TextView tvValue = cardView.findViewById(R.id.tvStatValue);
        TextView tvLabel = cardView.findViewById(R.id.tvStatLabel);
        tvValue.setText(value);
        tvLabel.setText(label);
        
        // 动态设置卡片背景
        if (cardView instanceof androidx.cardview.widget.CardView) {
            androidx.cardview.widget.CardView card = (androidx.cardview.widget.CardView) cardView;
            
            // 检查是否有自定义背景
            boolean hasCustomBg = backgroundManager != null && backgroundManager.hasCustomBackground();
            
            if (hasCustomBg) {
                // 自定义背景模式：透明背景 + 白色边框
                card.setCardBackgroundColor(getResources().getColor(android.R.color.transparent));
                card.setCardElevation(0);
                
                // 添加半透明边框
                GradientDrawable borderDrawable = new GradientDrawable();
                borderDrawable.setColor(getResources().getColor(android.R.color.transparent));
                borderDrawable.setStroke(2, getResources().getColor(R.color.semi_transparent_white));
                borderDrawable.setCornerRadius(16f);
                card.setBackground(borderDrawable);
                
                // 设置文字为白色加粗
                tvValue.setTextColor(getResources().getColor(R.color.text_white));
                tvValue.setTypeface(null, Typeface.BOLD);
                tvLabel.setTextColor(getResources().getColor(R.color.text_white));
            } else {
                // 普通模式：主题色渐变背景
                int themeIndex = ThemeManager.getSavedTheme(this);
                int[] gradientColors = getThemeGradientColors(themeIndex);
                
                GradientDrawable gradient = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    gradientColors
                );
                gradient.setCornerRadius(16f);
                card.setBackground(gradient);
                
                // 恢复默认文字样式
                tvValue.setTextColor(getResources().getColor(R.color.text_white));
                tvValue.setTypeface(null, Typeface.BOLD);
                tvLabel.setTextColor(getResources().getColor(R.color.semi_transparent_white));
            }
        }
        
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
     * 显示主菜单对话框（一级菜单：版本说明、数据管理、个性化设置、添加贷款）
     * 使用自定义卡片样式布局，带渐变标题头部
     */
    private void showMainMenu() {
        boolean hasData = !loansWithStatus.isEmpty();

        // 加载自定义菜单视图
        View menuView = LayoutInflater.from(this).inflate(R.layout.dialog_main_menu, null);

        // 动态设置渐变头部背景（跟随当前主题色）
        View menuHeader = menuView.findViewById(R.id.menuHeader);
        int themeIndex = ThemeManager.getSavedTheme(this);
        int primaryColor = ThemeManager.getThemePrimaryColor(themeIndex);
        int darkColor = ThemeManager.getThemeDarkColor(themeIndex);
        GradientDrawable headerBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{primaryColor, darkColor});
        float cornerRadius = dp2px(20);
        headerBg.setCornerRadii(new float[]{
                cornerRadius, cornerRadius, cornerRadius, cornerRadius,
                0, 0, 0, 0
        });
        menuHeader.setBackground(headerBg);

        // 条件显示添加贷款菜单项
        View dividerAddLoan = menuView.findViewById(R.id.dividerAddLoan);
        LinearLayout menuItemAddLoan = menuView.findViewById(R.id.menuItemAddLoan);
        if (hasData) {
            dividerAddLoan.setVisibility(View.VISIBLE);
            menuItemAddLoan.setVisibility(View.VISIBLE);
        }

        // 创建透明背景的对话框
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(menuView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // 设置各菜单项点击事件
        menuView.findViewById(R.id.menuItemVersion).setOnClickListener(v -> {
            dialog.dismiss();
            showVersionInfo();
        });
        menuView.findViewById(R.id.menuItemData).setOnClickListener(v -> {
            dialog.dismiss();
            showDataManagementMenu();
        });
        menuView.findViewById(R.id.menuItemSettings).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, BackgroundSettingsActivity.class);
            startActivity(intent);
        });
        if (hasData) {
            menuItemAddLoan.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(this, AddLoanActivity.class);
                startActivity(intent);
            });
        }
        menuView.findViewById(R.id.btnMenuClose).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * 显示数据管理二级菜单对话框（自定义卡片样式）
     */
    private void showDataManagementMenu() {
        // 加载自定义菜单视图
        View dataMenuView = LayoutInflater.from(this).inflate(R.layout.dialog_data_menu, null);

        // 动态设置渐变头部背景
        View dataMenuHeader = dataMenuView.findViewById(R.id.dataMenuHeader);
        int themeIndex = ThemeManager.getSavedTheme(this);
        int primaryColor = ThemeManager.getThemePrimaryColor(themeIndex);
        int darkColor = ThemeManager.getThemeDarkColor(themeIndex);
        GradientDrawable headerBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{primaryColor, darkColor});
        float cornerRadius = dp2px(20);
        headerBg.setCornerRadii(new float[]{
                cornerRadius, cornerRadius, cornerRadius, cornerRadius,
                0, 0, 0, 0
        });
        dataMenuHeader.setBackground(headerBg);

        // 创建透明背景对话框
        AlertDialog dataDialog = new AlertDialog.Builder(this)
                .setView(dataMenuView)
                .create();
        if (dataDialog.getWindow() != null) {
            dataDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // 设置各菜单项点击事件
        dataMenuView.findViewById(R.id.dataMenuItemExport).setOnClickListener(v -> {
            dataDialog.dismiss();
            exportData();
        });
        dataMenuView.findViewById(R.id.dataMenuItemImport).setOnClickListener(v -> {
            dataDialog.dismiss();
            importData();
        });
        dataMenuView.findViewById(R.id.dataMenuItemClear).setOnClickListener(v -> {
            dataDialog.dismiss();
            clearData();
        });
        dataMenuView.findViewById(R.id.btnDataMenuClose).setOnClickListener(v -> dataDialog.dismiss());

        dataDialog.show();
    }

    /**
     * 显示贷款管理对话框，使用卡片样式展示贷款列表（带渐变头部）
     */
    private void showLoanManagementMenu() {
        // 获取当前主题颜色
        int themeIndex = ThemeManager.getSavedTheme(this);
        int primaryColor = ThemeManager.getThemePrimaryColor(themeIndex);
        int darkColor = ThemeManager.getThemeDarkColor(themeIndex);
        int[] gradientColors = getThemeGradientColors(themeIndex);

        if (loansWithStatus.isEmpty()) {
            // 暂无贷款 - 显示样式化提示
            android.widget.FrameLayout emptyHeader = new android.widget.FrameLayout(this);
            LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp2px(80));
            emptyHeader.setLayoutParams(headerLp);
            GradientDrawable emptyHeaderBg = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR, new int[]{primaryColor, darkColor});
            float r = dp2px(20);
            emptyHeaderBg.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
            emptyHeader.setBackground(emptyHeaderBg);

            LinearLayout titleLayout = new LinearLayout(this);
            titleLayout.setOrientation(LinearLayout.VERTICAL);
            titleLayout.setGravity(android.view.Gravity.CENTER);
            android.widget.FrameLayout.LayoutParams tlp = new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT);
            tlp.gravity = android.view.Gravity.CENTER;
            titleLayout.setLayoutParams(tlp);

            TextView tvTitle = new TextView(this);
            tvTitle.setText(getString(R.string.loan_management));
            tvTitle.setTextColor(0xFFFFFFFF);
            tvTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
            tvTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            tvTitle.setGravity(android.view.Gravity.CENTER);
            titleLayout.addView(tvTitle);

            TextView tvSub = new TextView(this);
            tvSub.setText("暂无贷款记录");
            tvSub.setTextColor(0xCCFFFFFF);
            tvSub.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11);
            tvSub.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            subLp.topMargin = dp2px(3);
            tvSub.setLayoutParams(subLp);
            titleLayout.addView(tvSub);
            emptyHeader.addView(titleLayout);

            com.google.android.material.button.MaterialButton closeBtn =
                    new com.google.android.material.button.MaterialButton(
                            this, null, com.google.android.material.R.attr.borderlessButtonStyle);
            closeBtn.setText("关  闭");
            closeBtn.setTextColor(primaryColor);

            LinearLayout btnContainer = new LinearLayout(this);
            btnContainer.setOrientation(LinearLayout.HORIZONTAL);
            btnContainer.setGravity(android.view.Gravity.CENTER);
            btnContainer.setPadding(0, dp2px(4), 0, dp2px(16));
            btnContainer.addView(closeBtn);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            android.graphics.drawable.GradientDrawable cardBg = new android.graphics.drawable.GradientDrawable();
            cardBg.setColor(0xFFFFFFFF);
            cardBg.setCornerRadius(dp2px(20));
            card.setBackground(cardBg);
            card.addView(emptyHeader);
            card.addView(btnContainer);

            LinearLayout wrapper = new LinearLayout(this);
            wrapper.setOrientation(LinearLayout.VERTICAL);
            wrapper.setPadding(dp2px(20), dp2px(12), dp2px(20), dp2px(12));
            wrapper.addView(card);

            AlertDialog emptyDialog = new AlertDialog.Builder(this)
                    .setView(wrapper).create();
            if (emptyDialog.getWindow() != null) {
                emptyDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
            closeBtn.setOnClickListener(v -> emptyDialog.dismiss());
            emptyDialog.show();
            return;
        }

        // ── 有数据：构建卡片列表 ──
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp2px(12), dp2px(8), dp2px(12), dp2px(8));
        scrollView.addView(container);

        // 为每个贷款创建卡片
        for (int i = 0; i < loansWithStatus.size(); i++) {
            final LoanRepository.LoanWithStatus lws = loansWithStatus.get(i);
            View cardView = LayoutInflater.from(this).inflate(R.layout.dialog_loan_card_item, container, false);

            // 设置卡片内容背景为当前主题渐变
            LinearLayout cardContent = cardView.findViewById(R.id.cardContent);
            GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, gradientColors
            );
            gradient.setCornerRadius(dp2px(12));
            cardContent.setBackground(gradient);

            // 填充数据
            TextView tvIcon = cardView.findViewById(R.id.tvLoanIcon);
            TextView tvName = cardView.findViewById(R.id.tvLoanName);
            TextView tvMethod = cardView.findViewById(R.id.tvRepaymentMethod);
            TextView tvStatus = cardView.findViewById(R.id.tvLoanStatus);
            TextView tvMonthly = cardView.findViewById(R.id.tvMonthlyPayment);
            TextView tvRemaining = cardView.findViewById(R.id.tvRemainingPrincipal);

            tvIcon.setText(lws.loan.isCreditCard() ? "💳" : "🏦");
            tvName.setText(lws.loan.getName());
            tvMethod.setText(lws.loan.getRepaymentMethodName());
            tvMonthly.setText(NumberFormatUtil.formatCurrency(lws.status.getNewMonthlyPayment()));
            tvRemaining.setText(NumberFormatUtil.formatCurrency(lws.status.getRemainingPrincipal()));

            // 已还清标签
            if (lws.status.isPaidOff()) {
                tvStatus.setVisibility(View.VISIBLE);
                tvStatus.setText("已还清");
                GradientDrawable paidGradient = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[] { 0xFF66BB6A, 0xFF81C784, 0xFFA5D6A7 }
                );
                paidGradient.setCornerRadius(dp2px(12));
                cardContent.setBackground(paidGradient);
            }

            // 点击跳转贷款详情
            cardView.setOnClickListener(v -> {
                Intent intent = new Intent(this, com.finance.loanmanager.ui.loan.LoanDetailActivity.class);
                intent.putExtra("loan_id", lws.loan.getId());
                startActivity(intent);
            });

            container.addView(cardView);
        }

        // ── 构建带渐变头部的外层包装 ──
        // 渐变头部
        android.widget.FrameLayout headerView = new android.widget.FrameLayout(this);
        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp2px(80));
        headerView.setLayoutParams(headerLp);
        GradientDrawable headerBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[]{primaryColor, darkColor});
        float cornerRadius = dp2px(20);
        headerBg.setCornerRadii(new float[]{cornerRadius, cornerRadius, cornerRadius, cornerRadius, 0, 0, 0, 0});
        headerView.setBackground(headerBg);

        LinearLayout headerContent = new LinearLayout(this);
        headerContent.setOrientation(LinearLayout.VERTICAL);
        headerContent.setGravity(android.view.Gravity.CENTER);
        android.widget.FrameLayout.LayoutParams hcLp = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT);
        hcLp.gravity = android.view.Gravity.CENTER;
        headerContent.setLayoutParams(hcLp);

        TextView tvHeaderTitle = new TextView(this);
        tvHeaderTitle.setText(getString(R.string.loan_management));
        tvHeaderTitle.setTextColor(0xFFFFFFFF);
        tvHeaderTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
        tvHeaderTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvHeaderTitle.setGravity(android.view.Gravity.CENTER);
        tvHeaderTitle.setLetterSpacing(0.08f);
        headerContent.addView(tvHeaderTitle);

        TextView tvHeaderSub = new TextView(this);
        tvHeaderSub.setText("点击贷款卡片查看详情");
        tvHeaderSub.setTextColor(0xCCFFFFFF);
        tvHeaderSub.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11);
        tvHeaderSub.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp2px(3);
        tvHeaderSub.setLayoutParams(subLp);
        headerContent.addView(tvHeaderSub);
        headerView.addView(headerContent);

        // 细分割线
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(0xFFF0F0F0);

        // 关闭按钮
        com.google.android.material.button.MaterialButton closeBtn =
                new com.google.android.material.button.MaterialButton(
                        this, null, com.google.android.material.R.attr.borderlessButtonStyle);
        closeBtn.setText("关  闭");
        closeBtn.setTextColor(primaryColor);

        LinearLayout btnContainer = new LinearLayout(this);
        btnContainer.setOrientation(LinearLayout.HORIZONTAL);
        btnContainer.setGravity(android.view.Gravity.CENTER);
        btnContainer.setPadding(0, dp2px(4), 0, dp2px(16));
        btnContainer.addView(closeBtn);

        // 白色圆角卡片容器
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        android.graphics.drawable.GradientDrawable cardBg = new android.graphics.drawable.GradientDrawable();
        cardBg.setColor(0xFFFFFFFF);
        cardBg.setCornerRadius(dp2px(20));
        card.setBackground(cardBg);
        card.setElevation(dp2px(6));
        card.addView(headerView);
        card.addView(divider);
        card.addView(scrollView);
        card.addView(btnContainer);

        // 外层边距包装
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPadding(dp2px(20), dp2px(12), dp2px(20), dp2px(12));
        wrapper.addView(card);

        // 显示对话框
        AlertDialog loanDialog = new AlertDialog.Builder(this)
                .setView(wrapper)
                .create();
        if (loanDialog.getWindow() != null) {
            loanDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        closeBtn.setOnClickListener(v -> loanDialog.dismiss());
        loanDialog.show();
    }
    
    /**
     * dp 转 px
     */
    private int dp2px(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
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
