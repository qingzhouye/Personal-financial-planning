package com.finance.loanmanager.ui.savings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.finance.loanmanager.R;
import com.finance.loanmanager.data.entity.Savings;
import com.finance.loanmanager.databinding.ActivitySavingsMainBinding;
import com.finance.loanmanager.repository.SavingsRepository;
import com.finance.loanmanager.ui.BaseActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SavingsMainActivity extends BaseActivity {

    private ActivitySavingsMainBinding binding;
    private SavingsRepository repository;
    private SavingsAdapter adapter;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySavingsMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new SavingsRepository(getApplication());
        executorService = Executors.newSingleThreadExecutor();

        initViews();
        setupListeners();
    }

    private void initViews() {
        adapter = new SavingsAdapter(new ArrayList<>(),
            savings -> {
                // 点击编辑
                Intent intent = new Intent(this, AddEditSavingsActivity.class);
                intent.putExtra(AddEditSavingsActivity.EXTRA_SAVINGS_ID, savings.getId());
                startActivity(intent);
            },
            savings -> {
                // 长按无操作（主页不删除，去历史页删除）
            }
        );

        binding.rvRecentSavings.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRecentSavings.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnAddSavings.setOnClickListener(v -> {
            startActivity(new Intent(this, AddEditSavingsActivity.class));
        });

        binding.tvViewAll.setOnClickListener(v -> {
            startActivity(new Intent(this, SavingsHistoryActivity.class));
        });

        binding.btnViewChart.setOnClickListener(v -> {
            startActivity(new Intent(this, SavingsChartActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        executorService.execute(() -> {
            double totalBalance = repository.getTotalBalance();
            List<Savings> recentList = repository.getRecentSavings(5);

            runOnUiThread(() -> {
                // 更新余额
                binding.tvTotalBalance.setText(String.format(Locale.getDefault(), "¥%.2f", totalBalance));

                // 更新列表
                if (recentList != null && !recentList.isEmpty()) {
                    adapter.updateData(recentList);
                    binding.rvRecentSavings.setVisibility(View.VISIBLE);
                    binding.layoutEmpty.setVisibility(View.GONE);
                } else {
                    binding.rvRecentSavings.setVisibility(View.GONE);
                    binding.layoutEmpty.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
