/**
 * ============================================================================
 * 文件名: PaymentScheduleActivity.java
 * 模块:   UI层 - 还款计划
 * 功能:   显示单个贷款的完整还款计划表
 * 
 * 主要职责:
 *   1. 展示贷款的还款计划列表
 *   2. 显示每期的还款金额、本金、利息、剩余本金
 *   3. 显示贷款汇总信息（已还、剩余、总期数）
 * 
 * 列表内容:
 *   - 期数: 第几期还款
 *   - 还款日期: 该期的还款日期
 *   - 还款金额: 当期总还款额
 *   - 本金: 当期偿还的本金
 *   - 利息: 当期偿还的利息
 *   - 剩余本金: 还款后的剩余本金
 * 
 * @see ScheduleAdapter 还款计划适配器
 * @see LoanCalculator.PaymentScheduleItem 还款计划项
 * ============================================================================
 */
package com.finance.loanmanager.ui.schedule;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 还款计划界面 Activity
 * 
 * 该界面展示单个贷款的完整还款计划表，帮助用户了解
 * 每期的还款明细和整体还款进度。
 */
public class PaymentScheduleActivity extends BaseActivity {

    // ==================== 成员变量 ====================
    
    /** 当前贷款ID */
    private int loanId;
    
    /** 数据仓库引用 */
    private LoanRepository repository;
    
    /** 后台线程执行器 */
    private ExecutorService executorService;
    
    /** 列表视图 */
    private RecyclerView recyclerView;
    
    /** 贷款信息文本 */
    private TextView tvLoanInfo;
    
    /** 剩余本金文本 */
    private TextView tvRemaining;
    
    /** 已还金额文本 */
    private TextView tvPaid;
    
    /** 期数文本 */
    private TextView tvPeriods;
    
    /** 列表适配器 */
    private ScheduleAdapter adapter;
    
    /** 还款计划数据 */
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
