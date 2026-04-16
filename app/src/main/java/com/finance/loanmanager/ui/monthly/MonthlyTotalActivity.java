/**
 * ============================================================================
 * 文件名: MonthlyTotalActivity.java
 * 模块:   UI层 - 月度还款汇总
 * 功能:   显示每月还款总额和各贷款详情的页面
 * 
 * 主要职责:
 *   1. 计算并展示每月的还款总额
 *   2. 按月份汇总所有活跃贷款的还款
 *   3. 显示每笔贷款在各月的还款金额
 * 
 * 界面元素:
 *   - 当前月份: 最近一个需要还款的月份
 *   - 当前月总额: 该月的总还款金额
 *   - 月度列表: 按时间顺序显示每月还款详情
 * 
 * 计算逻辑:
 *   1. 获取所有活跃贷款的还款计划
 *   2. 按月份汇总还款金额
 *   3. 按时间排序显示
 * 
 * @see MonthlyAdapter 月度列表适配器
 * @see LoanCalculator 还款计划计算
 * ============================================================================
 */
package com.finance.loanmanager.ui.monthly;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.finance.loanmanager.R;
import com.finance.loanmanager.data.entity.Loan;
import com.finance.loanmanager.repository.LoanRepository;
import com.finance.loanmanager.service.LoanCalculator;
import com.finance.loanmanager.ui.BaseActivity;
import com.finance.loanmanager.util.NumberFormatUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 月度还款汇总界面 Activity
 * 
 * 该界面展示所有活跃贷款按月份汇总的还款计划，
 * 帮助用户了解未来各月的还款压力。
 */
public class MonthlyTotalActivity extends BaseActivity {

    // ==================== 成员变量 ====================
    
    /** 数据仓库引用 */
    private LoanRepository repository;
    
    /** 后台线程执行器 */
    private ExecutorService executorService;
    
    /** 列表视图 */
    private RecyclerView recyclerView;
    
    /** 当前月份文本 */
    private TextView tvCurrentMonth;
    
    /** 当前月总额文本 */
    private TextView tvCurrentTotal;
    
    /** 列表适配器 */
    private MonthlyAdapter adapter;
    
    /** 月度数据列表 */
    private List<MonthlyItem> monthlyItems = new ArrayList<>();

    /**
     * 月度贷款详情项
     * 包含贷款名称和应还金额
     */
    public static class LoanDetailItem {
        public final String loanName;
        public final double amount;
        
        public LoanDetailItem(String loanName, double amount) {
            this.loanName = loanName;
            this.amount = amount;
        }
    }

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
                    
                    // 根据贷款类型分类
                    LoanDetailItem loanDetail = new LoanDetailItem(
                        lws.loan.getName(), 
                        item.payment
                    );
                    
                    if (lws.loan.isStudentLoan()) {
                        data.studentLoans.add(loanDetail);
                    } else {
                        data.normalLoans.add(loanDetail);
                    }
                }
            }
            
            List<String> sortedMonths = new ArrayList<>(monthlyData.keySet());
            Collections.sort(sortedMonths);
            
            final List<MonthlyItem> newMonthlyItems = new ArrayList<>();
            for (String month : sortedMonths) {
                MonthlyData data = monthlyData.get(month);
                newMonthlyItems.add(new MonthlyItem(month, data.total, 
                    data.normalLoans, data.studentLoans));
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
        List<LoanDetailItem> normalLoans = new ArrayList<>();  // 普通贷款和信用卡
        List<LoanDetailItem> studentLoans = new ArrayList<>(); // 国家助学贷款
    }
    
    /**
     * 月度数据项
     * 包含月份、总额、普通贷款/信用卡列表、助学贷款列表
     */
    public static class MonthlyItem {
        public final String month;
        public final double total;
        public final List<LoanDetailItem> normalLoans;  // 普通贷款和信用卡
        public final List<LoanDetailItem> studentLoans; // 国家助学贷款
        
        public MonthlyItem(String month, double total, 
                          List<LoanDetailItem> normalLoans, 
                          List<LoanDetailItem> studentLoans) {
            this.month = month;
            this.total = total;
            this.normalLoans = normalLoans;
            this.studentLoans = studentLoans;
        }
    }
}
