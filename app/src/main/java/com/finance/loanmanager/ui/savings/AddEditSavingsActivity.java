package com.finance.loanmanager.ui.savings;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Toast;

import com.finance.loanmanager.R;
import com.finance.loanmanager.data.entity.Savings;
import com.finance.loanmanager.databinding.ActivityAddEditSavingsBinding;
import com.finance.loanmanager.repository.SavingsRepository;
import com.finance.loanmanager.ui.BaseActivity;

import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddEditSavingsActivity extends BaseActivity {

    public static final String EXTRA_SAVINGS_ID = "extra_savings_id";

    private ActivityAddEditSavingsBinding binding;
    private SavingsRepository repository;
    private ExecutorService executorService;

    private boolean isDeposit = true; // 默认存入
    private int editId = -1; // -1 表示新增
    private Savings editSavings = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddEditSavingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new SavingsRepository(getApplication());
        executorService = Executors.newSingleThreadExecutor();

        editId = getIntent().getIntExtra(EXTRA_SAVINGS_ID, -1);

        initViews();
        setupListeners();

        if (editId != -1) {
            loadEditData();
        }
    }

    private void initViews() {
        // 设置默认日期为今天
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        binding.etDate.setText(sdf.format(Calendar.getInstance().getTime()));

        // 编辑模式标题
        if (editId != -1) {
            binding.tvTitle.setText(R.string.savings_edit_record);
        }

        updateTypeButtons();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        // 类型切换
        binding.btnDeposit.setOnClickListener(v -> {
            isDeposit = true;
            updateTypeButtons();
        });

        binding.btnWithdraw.setOnClickListener(v -> {
            isDeposit = false;
            updateTypeButtons();
        });

        // 日期选择
        binding.etDate.setOnClickListener(v -> showDatePicker());

        // 常用标签
        for (int i = 0; i < binding.chipGroup.getChildCount(); i++) {
            if (binding.chipGroup.getChildAt(i) instanceof Chip) {
                Chip chip = (Chip) binding.chipGroup.getChildAt(i);
                chip.setOnClickListener(v -> {
                    binding.etNote.setText(chip.getText());
                    binding.etNote.setSelection(chip.getText().length());
                });
            }
        }

        // 保存
        binding.btnSave.setOnClickListener(v -> saveSavings());
    }

    private void updateTypeButtons() {
        if (isDeposit) {
            binding.btnDeposit.setStrokeWidth(0);
            binding.btnWithdraw.setStrokeWidth(2);
            // 交换样式：存入为实心，支出为轮廓
            binding.btnDeposit.setBackgroundColor(getResources().getColor(R.color.success, getTheme()));
            binding.btnDeposit.setTextColor(getResources().getColor(R.color.text_white, getTheme()));
            binding.btnWithdraw.setBackgroundColor(getResources().getColor(android.R.color.transparent, getTheme()));
            binding.btnWithdraw.setTextColor(getResources().getColor(R.color.danger, getTheme()));
        } else {
            binding.btnDeposit.setStrokeWidth(2);
            binding.btnWithdraw.setStrokeWidth(0);
            binding.btnWithdraw.setBackgroundColor(getResources().getColor(R.color.danger, getTheme()));
            binding.btnWithdraw.setTextColor(getResources().getColor(R.color.text_white, getTheme()));
            binding.btnDeposit.setBackgroundColor(getResources().getColor(android.R.color.transparent, getTheme()));
            binding.btnDeposit.setTextColor(getResources().getColor(R.color.success, getTheme()));
        }
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        // 尝试解析当前显示的日期
        try {
            String current = binding.etDate.getText().toString();
            if (!current.isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                cal.setTime(sdf.parse(current));
            }
        } catch (Exception ignored) {}

        DatePickerDialog dialog = new DatePickerDialog(this,
            (view, year, month, dayOfMonth) -> {
                String date = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, dayOfMonth);
                binding.etDate.setText(date);
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void loadEditData() {
        executorService.execute(() -> {
            editSavings = repository.getSavingsById(editId);
            if (editSavings != null) {
                runOnUiThread(() -> {
                    isDeposit = editSavings.isDeposit();
                    updateTypeButtons();
                    binding.etAmount.setText(String.valueOf(Math.abs(editSavings.getAmount())));
                    binding.etDate.setText(editSavings.getDate());
                    binding.etNote.setText(editSavings.getNote());
                });
            }
        });
    }

    private void saveSavings() {
        String amountStr = binding.etAmount.getText().toString().trim();
        String date = binding.etDate.getText().toString().trim();
        String note = binding.etNote.getText().toString().trim();

        if (amountStr.isEmpty()) {
            binding.etAmount.setError(getString(R.string.error_invalid_amount));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            binding.etAmount.setError(getString(R.string.error_invalid_amount));
            return;
        }

        if (amount <= 0) {
            binding.etAmount.setError(getString(R.string.error_invalid_amount));
            return;
        }

        if (date.isEmpty()) {
            return;
        }

        // 支出取负
        if (!isDeposit) {
            amount = -amount;
        }

        if (editId != -1 && editSavings != null) {
            // 编辑模式
            editSavings.setAmount(amount);
            editSavings.setDate(date);
            editSavings.setNote(note);
            repository.updateSavingsAsync(editSavings, () -> {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.savings_save_success, Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        } else {
            // 新增模式
            Savings savings = new Savings(amount, date, note);
            repository.insertSavingsAsync(savings, id -> {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.savings_save_success, Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
