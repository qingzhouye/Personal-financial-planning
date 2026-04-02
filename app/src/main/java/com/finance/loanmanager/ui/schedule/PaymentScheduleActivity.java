package com.finance.loanmanager.ui.schedule;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.finance.loanmanager.R;
import com.finance.loanmanager.data.entity.Loan;
import com.finance.loanmanager.repository.LoanRepository;
import com.finance.loanmanager.service.LoanCalculator;
import com.finance.loanmanager.util.NumberFormatUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PaymentScheduleActivity extends AppCompatActivity {

    private int loanId;
    private LoanRepository repository;
    private ExecutorService executorService;
    private RecyclerView recyclerView;
    private TextView tvLoanInfo;
    private TextView tvRemaining;
    private TextView tvPaid;
    private TextView tvPeriods;
    private ScheduleAdapter adapter;
    private List<LoanCalculator.PaymentScheduleItem> schedule = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_schedule);
        
        // 适配 Android 15/Edge 安全区域
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.payment_schedule);
        
        loanId = getIntent().getIntExtra("loan_id", -1);
        if (loanId == -1) {
            finish();
            return;
        }
        
        repository = new LoanRepository(getApplication());
        executorService = Executors.newSingleThreadExecutor();
        
        recyclerView = findViewById(R.id.recyclerView);
        tvLoanInfo = findViewById(R.id.tvLoanInfo);
        tvRemaining = findViewById(R.id.tvRemaining);
        tvPaid = findViewById(R.id.tvPaid);
        tvPeriods = findViewById(R.id.tvPeriods);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ScheduleAdapter(schedule);
        recyclerView.setAdapter(adapter);
        
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
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    private void loadData() {
        // 在后台线程执行数据库查询
        executorService.execute(() -> {
            final Loan loan = repository.getLoanById(loanId);
            if (loan == null) {
                runOnUiThread(this::finish);
                return;
            }
            
            final List<LoanCalculator.PaymentScheduleItem> newSchedule = repository.getPaymentSchedule(loanId);
            final double totalPaid = repository.getTotalPaidByLoanId(loanId);
            final double remaining = loan.getPrincipal() - totalPaid;
            
            runOnUiThread(() -> {
                schedule = newSchedule;
                adapter.updateData(schedule);
                
                tvLoanInfo.setText((loan.isCreditCard() ? "💳 " : "🏦 ") + loan.getName() 
                        + "\n还款方式：" + loan.getRepaymentMethodName());
                
                tvRemaining.setText(NumberFormatUtil.formatCurrency(remaining));
                tvPaid.setText(NumberFormatUtil.formatCurrency(totalPaid));
                tvPeriods.setText(schedule.size() + "期");
            });
        });
    }
}
