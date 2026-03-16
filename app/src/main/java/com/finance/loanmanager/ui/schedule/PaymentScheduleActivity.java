package com.finance.loanmanager.ui.schedule;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.finance.loanmanager.R;
import com.finance.loanmanager.data.entity.Loan;
import com.finance.loanmanager.repository.LoanRepository;
import com.finance.loanmanager.service.LoanCalculator;
import com.finance.loanmanager.util.NumberFormatUtil;

import java.util.ArrayList;
import java.util.List;

public class PaymentScheduleActivity extends AppCompatActivity {

    private int loanId;
    private LoanRepository repository;
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
        
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.payment_schedule);
        
        loanId = getIntent().getIntExtra("loan_id", -1);
        if (loanId == -1) {
            finish();
            return;
        }
        
        repository = new LoanRepository(getApplication());
        
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
    
    private void loadData() {
        Loan loan = repository.getLoanById(loanId);
        if (loan == null) {
            finish();
            return;
        }
        
        schedule = repository.getPaymentSchedule(loanId);
        adapter.updateData(schedule);
        
        tvLoanInfo.setText((loan.isCreditCard() ? "💳 " : "🏦 ") + loan.getName() 
                + "\n还款方式：" + loan.getRepaymentMethodName());
        
        double totalPaid = repository.getTotalPaidByLoanId(loanId);
        double remaining = loan.getPrincipal() - totalPaid;
        
        tvRemaining.setText(NumberFormatUtil.formatCurrency(remaining));
        tvPaid.setText(NumberFormatUtil.formatCurrency(totalPaid));
        tvPeriods.setText(schedule.size() + "期");
    }
}
