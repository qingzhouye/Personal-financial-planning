package com.finance.loanmanager.ui.savings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.finance.loanmanager.R;
import com.finance.loanmanager.data.entity.Savings;
import com.finance.loanmanager.databinding.ActivitySavingsHistoryBinding;
import com.finance.loanmanager.repository.SavingsRepository;
import com.finance.loanmanager.ui.BaseActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SavingsHistoryActivity extends BaseActivity {

    private ActivitySavingsHistoryBinding binding;
    private SavingsRepository repository;
    private SavingsAdapter adapter;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySavingsHistoryBinding.inflate(getLayoutInflater());
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
                // 长按删除
                showDeleteConfirmDialog(savings);
            }
        );

        binding.rvSavingsHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSavingsHistory.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        executorService.execute(() -> {
            List<Savings> list = repository.getAllSavingsSync();
            runOnUiThread(() -> {
                if (list != null && !list.isEmpty()) {
                    adapter.updateData(list);
                    binding.rvSavingsHistory.setVisibility(android.view.View.VISIBLE);
                    binding.layoutEmpty.setVisibility(android.view.View.GONE);
                } else {
                    binding.rvSavingsHistory.setVisibility(android.view.View.GONE);
                    binding.layoutEmpty.setVisibility(android.view.View.VISIBLE);
                }
            });
        });
    }

    private void showDeleteConfirmDialog(Savings savings) {
        new AlertDialog.Builder(this)
            .setMessage(R.string.savings_delete_confirm)
            .setPositiveButton(R.string.confirm, (dialog, which) -> {
                repository.deleteSavingsAsync(savings.getId(), () -> {
                    runOnUiThread(() -> {
                        Toast.makeText(this, R.string.savings_delete_success, Toast.LENGTH_SHORT).show();
                        loadData();
                    });
                });
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
