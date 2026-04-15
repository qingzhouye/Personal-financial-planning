/**
 * ============================================================================
 * 文件名: LoanDetailActivity.java
 * 模块:   UI层 - 贷款详情
 * 功能:   展示单个贷款的详细信息和操作功能
 * 
 * 主要职责:
 *   1. 显示贷款基本信息（本金、利率、期限等）
 *   2. 显示还款进度（已还金额、剩余本金）
 *   3. 支持手动还款操作
 *   4. 提供还款计划查看入口
 *   5. 支持编辑贷款名称和删除贷款
 * 
 * 界面元素:
 *   - 贷款标题（点击可编辑名称）
 *   - 基本信息卡片：本金、利率、期限、开始日期
 *   - 还款进度卡片：已还金额、剩余本金、月供、进度百分比
 *   - 操作按钮：查看还款计划、还款、删除贷款
 * 
 * 数据刷新:
 *   - 在 onResume 时重新加载数据，确保信息最新
 *   - 还款成功后自动刷新界面
 * 
 * @see Loan 贷款实体
 * @see LoanStatus 贷款状态
 * @see PaymentScheduleActivity 还款计划页面
 * ============================================================================
 */
package com.finance.loanmanager.ui.loan;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.finance.loanmanager.R;
import com.finance.loanmanager.data.entity.Loan;
import com.finance.loanmanager.data.entity.LoanStatus;
import com.finance.loanmanager.data.entity.Payment;
import com.finance.loanmanager.repository.LoanRepository;
import com.finance.loanmanager.ui.BaseActivity;
import com.finance.loanmanager.ui.schedule.PaymentScheduleActivity;
import com.finance.loanmanager.util.DateUtil;
import com.finance.loanmanager.util.NumberFormatUtil;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 贷款详情界面 Activity
 * 
 * 该界面展示单个贷款的完整信息，并提供还款和删除功能。
 * 通过 Intent 传递的 loan_id 参数来确定要显示的贷款。
 * 
 * 主要功能:
 *   - 点击标题可编辑贷款名称
 *   - 支持手动记录还款
 *   - 可查看完整还款计划表
 *   - 可删除该贷款记录
 */
public class LoanDetailActivity extends BaseActivity {

    // ==================== 成员变量 ====================
    
    /** 当前贷款ID */
    private int loanId;
    
    /** 数据仓库引用 */
    private LoanRepository repository;
    
    /** 后台线程执行器 */
    private ExecutorService executorService;
    
    /** 当前贷款数据 */
    private Loan loan;
    
    /** 当前贷款状态 */
    private LoanStatus status;
    
    /** 贷款标题文本 */
    private TextView tvTitle;
    
    /** 副标题文本（还款方式、状态） */
    private TextView tvSubtitle;
    
    /** 本金文本 */
    private TextView tvPrincipal;
    
    /** 利率文本 */
    private TextView tvRate;
    
    /** 期限文本 */
    private TextView tvMonths;
    
    /** 开始日期文本 */
    private TextView tvStartDate;
    
    /** 已还金额文本 */
    private TextView tvTotalPaid;
    
    /** 剩余本金文本 */
    private TextView tvRemaining;
    
    /** 月供文本 */
    private TextView tvMonthlyPayment;
    
    /** 进度文本 */
    private TextView tvProgress;
    
    /** 已还百分比文本 */
    private TextView tvProgressPercent;
    
    /** 剩余百分比文本 */
    private TextView tvRemainingPercent;
    
    /** 查看还款计划按钮 */
    private Button btnViewSchedule;
    
    /** 还款按钮 */
    private Button btnMakePayment;
    
    /** 删除按钮 */
    private Button btnDelete;
    
    /** 还款输入布局 */
    private LinearLayout layoutPayment;
    
    /** 还款金额输入框 */
    private TextInputEditText etPaymentAmount;
    
    /** 确认还款按钮 */
    private Button btnConfirmPayment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loan_detail);
        
        // 适配 Android 15/16 Edge-to-Edge 安全区域
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.loan_detail);
        
        loanId = getIntent().getIntExtra("loan_id", -1);
        if (loanId == -1) {
            finish();
            return;
        }
        
        repository = new LoanRepository(getApplication());
        executorService = Executors.newSingleThreadExecutor();
        initViews();
        setupListeners();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvPrincipal = findViewById(R.id.tvPrincipal);
        tvRate = findViewById(R.id.tvRate);
        tvMonths = findViewById(R.id.tvMonths);
        tvStartDate = findViewById(R.id.tvStartDate);
        tvTotalPaid = findViewById(R.id.tvTotalPaid);
        tvRemaining = findViewById(R.id.tvRemaining);
        tvMonthlyPayment = findViewById(R.id.tvMonthlyPayment);
        tvProgress = findViewById(R.id.tvProgress);
        tvProgressPercent = findViewById(R.id.tvProgressPercent);
        tvRemainingPercent = findViewById(R.id.tvRemainingPercent);
        btnViewSchedule = findViewById(R.id.btnViewSchedule);
        btnMakePayment = findViewById(R.id.btnMakePayment);
        btnDelete = findViewById(R.id.btnDelete);
        layoutPayment = findViewById(R.id.layoutPayment);
        etPaymentAmount = findViewById(R.id.etPaymentAmount);
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment);
    }
    
    private void setupListeners() {
        // 点击标题编辑贷款名称
        tvTitle.setOnClickListener(v -> showEditNameDialog());
        
        btnViewSchedule.setOnClickListener(v -> {
            Intent intent = new Intent(this, PaymentScheduleActivity.class);
            intent.putExtra("loan_id", loanId);
            startActivity(intent);
        });
        
        btnMakePayment.setOnClickListener(v -> {
            layoutPayment.setVisibility(layoutPayment.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });
        
        btnConfirmPayment.setOnClickListener(v -> makePayment());
        
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.delete_loan)
                    .setMessage(R.string.delete_confirm)
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        repository.deleteLoanById(loanId);
                        Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }
    
    /**
     * 显示编辑贷款名称对话框
     */
    private void showEditNameDialog() {
        if (loan == null) return;
        
        EditText editText = new EditText(this);
        editText.setText(loan.getName());
        editText.setSelection(loan.getName().length());
        editText.setSingleLine(true);
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginStart((int) (16 * getResources().getDisplayMetrics().density));
        lp.setMarginEnd((int) (16 * getResources().getDisplayMetrics().density));
        editText.setLayoutParams(lp);
        
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, (int) (16 * getResources().getDisplayMetrics().density), 0, 0);
        container.addView(editText);
        
        new AlertDialog.Builder(this)
                .setTitle("编辑贷款名称")
                .setView(container)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    String newName = editText.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        updateLoanName(newName);
                    } else {
                        Toast.makeText(this, "名称不能为空", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    /**
     * 更新贷款名称
     */
    private void updateLoanName(String newName) {
        if (loan == null) return;
        
        loan.setName(newName);
        executorService.execute(() -> {
            repository.updateLoan(loan);
            runOnUiThread(() -> {
                tvTitle.setText((loan.isCreditCard() ? "💳 " : "🏦 ") + loan.getName());
                Toast.makeText(this, "名称已更新", Toast.LENGTH_SHORT).show();
            });
        });
    }
    
    private void loadData() {
        // 在后台线程执行数据库查询
        executorService.execute(() -> {
            final Loan loanData = repository.getLoanById(loanId);
            final LoanStatus statusData = repository.getLoanStatus(loanId);
            
            runOnUiThread(() -> {
                if (loanData == null || statusData == null) {
                    finish();
                    return;
                }
                
                loan = loanData;
                status = statusData;
                
                tvTitle.setText((loan.isCreditCard() ? "💳 " : "🏦 ") + loan.getName());
                tvSubtitle.setText(loan.getRepaymentMethodName() + (status.isPaidOff() ? " | 已还清" : ""));
                tvPrincipal.setText(NumberFormatUtil.formatCurrency(loan.getPrincipal()));
                tvRate.setText(loan.getAnnualRate() + "%");
                tvMonths.setText(loan.getMonths() + "个月");
                tvStartDate.setText(loan.getStartDate());
                tvTotalPaid.setText(NumberFormatUtil.formatCurrency(status.getTotalPaid()));
                tvRemaining.setText(NumberFormatUtil.formatCurrency(status.getRemainingPrincipal()));
                tvMonthlyPayment.setText(NumberFormatUtil.formatCurrency(status.getNewMonthlyPayment()));
                tvProgress.setText(NumberFormatUtil.formatPercent(status.getProgressPercentage(loan.getPrincipal())));
                
                double paidPct = status.getProgressPercentage(loan.getPrincipal());
                double remainPct = status.getRemainingPercentage(loan.getPrincipal());
                tvProgressPercent.setText(String.format("%.1f%%", paidPct));
                tvRemainingPercent.setText(String.format("%.1f%%", remainPct));
                
                if (status.isPaidOff()) {
                    btnMakePayment.setVisibility(View.GONE);
                }
            });
        });
    }
    
    private void makePayment() {
        String amountStr = etPaymentAmount.getText().toString().trim();
        double amount = NumberFormatUtil.parseDouble(amountStr);
        
        if (amount <= 0) {
            Toast.makeText(this, R.string.error_invalid_amount, Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (status == null || amount > status.getRemainingPrincipal()) {
            Toast.makeText(this, 
                    String.format(getString(R.string.error_payment_exceeds), 
                            status != null ? status.getRemainingPrincipal() : 0),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 在后台线程执行数据库插入
        executorService.execute(() -> {
            Payment payment = new Payment(loanId, amount, DateUtil.getCurrentDate());
            repository.insertPayment(payment);
            
            runOnUiThread(() -> {
                Toast.makeText(this, R.string.payment_success, Toast.LENGTH_SHORT).show();
                etPaymentAmount.setText("");
                layoutPayment.setVisibility(View.GONE);
                loadData();
            });
        });
    }
}
