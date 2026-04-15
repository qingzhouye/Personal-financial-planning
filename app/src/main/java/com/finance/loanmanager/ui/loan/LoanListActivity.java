/**
 * ============================================================================
 * 文件名: LoanListActivity.java
 * 模块:   UI层 - 贷款列表
 * 功能:   显示所有贷款的列表页面
 * 
 * 主要职责:
 *   1. 以列表形式展示所有贷款
 *   2. 点击列表项跳转至贷款详情页
 *   3. 无贷款时显示空状态提示
 * 
 * 列表内容:
 *   - 贷款名称和图标
 *   - 剩余本金
 *   - 月供金额
 *   - 已还清标记
 * 
 * 数据加载:
 *   - 在 onResume 时加载最新数据
 *   - 在后台线程执行数据库查询
 * 
 * @see LoanAdapter 列表适配器
 * @see LoanDetailActivity 贷款详情页面
 * ============================================================================
 */
package com.finance.loanmanager.ui.loan;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.finance.loanmanager.R;
import com.finance.loanmanager.repository.LoanRepository;
import com.finance.loanmanager.ui.BaseActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 贷款列表界面 Activity
 * 
 * 该界面以列表形式展示所有贷款记录，用户可点击查看详情。
 * 继承自 BaseActivity，自动获得主题和背景处理能力。
 */
public class LoanListActivity extends BaseActivity {

    // ==================== 成员变量 ====================
    
    /** 列表视图 */
    private RecyclerView recyclerView;
    
    /** 空状态提示文本 */
    private TextView tvEmpty;
    
    /** 数据仓库引用 */
    private LoanRepository repository;
    
    /** 后台线程执行器 */
    private ExecutorService executorService;
    
    /** 列表适配器 */
    private LoanAdapter adapter;
    
    /** 贷款数据列表 */
    private List<LoanRepository.LoanWithStatus> loans = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loan_list);
        
        // 适配 Android 15/16 Edge-to-Edge 安全区域
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.loan_list);
        
        repository = new LoanRepository(getApplication());
        executorService = Executors.newSingleThreadExecutor();
        
        recyclerView = findViewById(R.id.recyclerView);
        tvEmpty = findViewById(R.id.tvEmpty);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LoanAdapter(loans, loan -> {
            Intent intent = new Intent(this, LoanDetailActivity.class);
            intent.putExtra("loan_id", loan.loan.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
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
            final List<LoanRepository.LoanWithStatus> result = repository.getLoansWithStatus();
            runOnUiThread(() -> {
                loans = result;
                adapter.updateData(loans);
                
                if (loans.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            });
        });
    }
}
