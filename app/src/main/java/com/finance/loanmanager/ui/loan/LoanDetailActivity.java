package com.finance.loanmanager.ui.loan;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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

public class LoanDetailActivity extends BaseActivity {

    private int loanId;
    private LoanRepository repository;
    private ExecutorService executorService;
    private Loan loan;
    private LoanStatus status;
    
    private TextView tvTitle;
    private TextView tvSubtitle;
    private TextView tvPrincipal;
    private TextView tvRate;
    private TextView tvMonths;
    private TextView tvStartDate;
    private TextView tvTotalPaid;
    private TextView tvRemaining;
    private TextView tvMonthlyPayment;
    private TextView tvProgress;
    private TextView tvProgressPercent;
    private TextView tvRemainingPercent;
    private Button btnViewSchedule;
    private Button btnMakePayment;
    private Button btnDelete;
    private LinearLayout layoutPayment;
    private TextInputEditText etPaymentAmount;
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
