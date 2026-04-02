package com.finance.loanmanager.ui.monthly;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MonthlyTotalActivity extends AppCompatActivity {

    private LoanRepository repository;
    private ExecutorService executorService;
    private RecyclerView recyclerView;
    private TextView tvCurrentMonth;
    private TextView tvCurrentTotal;
    private MonthlyAdapter adapter;
    private List<MonthlyItem> monthlyItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_total);
        
        // 适配 Android 15/16 Edge-to-Edge 安全区域
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.monthly_total);
        
        repository = new LoanRepository(getApplication());
        executorService = Executors.newSingleThreadExecutor();
        
        recyclerView = findViewById(R.id.recyclerView);
        tvCurrentMonth = findViewById(R.id.tvCurrentMonth);
        tvCurrentTotal = findViewById(R.id.tvCurrentTotal);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MonthlyAdapter(monthlyItems);
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
            List<LoanRepository.LoanWithStatus> loans = repository.getLoansWithStatus();
            List<LoanRepository.LoanWithStatus> activeLoans = new ArrayList<>();
            
            for (LoanRepository.LoanWithStatus lws : loans) {
                if (!lws.status.isPaidOff()) {
                    activeLoans.add(lws);
                }
            }
            
            if (activeLoans.isEmpty()) {
                runOnUiThread(() -> Toast.makeText(this, R.string.no_active_loans, Toast.LENGTH_SHORT).show());
                return;
            }
            
            Map<String, MonthlyData> monthlyData = new HashMap<>();
            
            for (LoanRepository.LoanWithStatus lws : activeLoans) {
                List<LoanCalculator.PaymentScheduleItem> schedule = 
                        repository.getPaymentSchedule(lws.loan.getId());
                
                for (LoanCalculator.PaymentScheduleItem item : schedule) {
                    String month = item.date.substring(0, 7);
                    
                    MonthlyData data = monthlyData.get(month);
                    if (data == null) {
                        data = new MonthlyData();
                        monthlyData.put(month, data);
                    }
                    
                    data.total += item.payment;
                    data.loans.add(lws.loan.getName() + ": " + NumberFormatUtil.formatCurrency(item.payment));
                }
            }
            
            List<String> sortedMonths = new ArrayList<>(monthlyData.keySet());
            Collections.sort(sortedMonths);
            
            final List<MonthlyItem> newMonthlyItems = new ArrayList<>();
            for (String month : sortedMonths) {
                MonthlyData data = monthlyData.get(month);
                newMonthlyItems.add(new MonthlyItem(month, data.total, data.loans));
            }
            
            final String firstMonth = sortedMonths.isEmpty() ? null : sortedMonths.get(0);
            final MonthlyData firstData = firstMonth != null ? monthlyData.get(firstMonth) : null;
            
            runOnUiThread(() -> {
                monthlyItems.clear();
                monthlyItems.addAll(newMonthlyItems);
                adapter.updateData(monthlyItems);
                
                if (firstMonth != null && firstData != null) {
                    tvCurrentMonth.setText(firstMonth);
                    tvCurrentTotal.setText(NumberFormatUtil.formatCurrency(firstData.total));
                }
            });
        });
    }
    
    private static class MonthlyData {
        double total = 0;
        List<String> loans = new ArrayList<>();
    }
    
    public static class MonthlyItem {
        public final String month;
        public final double total;
        public final List<String> loans;
        
        public MonthlyItem(String month, double total, List<String> loans) {
            this.month = month;
            this.total = total;
            this.loans = loans;
        }
    }
}
