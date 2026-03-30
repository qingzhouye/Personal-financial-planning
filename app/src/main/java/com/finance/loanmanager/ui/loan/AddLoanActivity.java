package com.finance.loanmanager.ui.loan;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.finance.loanmanager.R;
import com.finance.loanmanager.data.entity.Loan;
import com.finance.loanmanager.repository.LoanRepository;
import com.finance.loanmanager.util.DateUtil;
import com.finance.loanmanager.util.NumberFormatUtil;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddLoanActivity extends AppCompatActivity {

    private LoanRepository repository;
    private ExecutorService executorService;
    
    private TextInputEditText spinnerLoanType;
    private TextInputEditText etLoanName;
    private TextInputEditText spinnerRepaymentMethod;
    private LinearLayout layoutNormalFields;
    private LinearLayout layoutCreditCardFields;
    private TextInputEditText etPrincipal;
    private TextInputEditText etAnnualRate;
    private TextInputEditText etMonths;
    private TextInputEditText etStartDate;
    private TextInputEditText etCreditLimit;
    private TextInputEditText etCreditCardMonths;
    private TextInputEditText etCreditCardDate;
    private TextInputEditText etDueDate;
    private TextInputEditText etCreditCardRate;
    private Button btnSubmit;
    
    private String selectedLoanType = "normal";
    private String selectedRepaymentMethod = "equal_interest";
    private final String[] loanTypes = {"normal", "credit_card"};
    private final String[] loanTypeNames = {"普通贷款", "信用卡"};
    private final String[] repaymentMethods = {"equal_interest", "equal_principal", "interest_first", "lump_sum"};
    private final String[] repaymentMethodNames = {"等额本息", "等额本金", "先息后本", "利随本清"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_loan);
        
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.add_loan);
        
        repository = new LoanRepository(getApplication());
        executorService = Executors.newSingleThreadExecutor();
        
        initViews();
        setupListeners();
        
        // 设置默认日期 - 确保日期字段不为空
        String currentDate = DateUtil.getCurrentDate();
        if (currentDate == null || currentDate.isEmpty()) {
            currentDate = "2026-01-01"; // 备用默认日期
        }
        if (etStartDate != null) {
            etStartDate.setText(currentDate);
        }
        if (etCreditCardDate != null) {
            etCreditCardDate.setText(currentDate);
        }
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
    
    private void initViews() {
        spinnerLoanType = findViewById(R.id.spinnerLoanType);
        etLoanName = findViewById(R.id.etLoanName);
        spinnerRepaymentMethod = findViewById(R.id.spinnerRepaymentMethod);
        layoutNormalFields = findViewById(R.id.layoutNormalFields);
        layoutCreditCardFields = findViewById(R.id.layoutCreditCardFields);
        etPrincipal = findViewById(R.id.etPrincipal);
        etAnnualRate = findViewById(R.id.etAnnualRate);
        etMonths = findViewById(R.id.etMonths);
        etStartDate = findViewById(R.id.etStartDate);
        etCreditLimit = findViewById(R.id.etCreditLimit);
        etCreditCardMonths = findViewById(R.id.etCreditCardMonths);
        etCreditCardDate = findViewById(R.id.etCreditCardDate);
        etDueDate = findViewById(R.id.etDueDate);
        etCreditCardRate = findViewById(R.id.etCreditCardRate);
        btnSubmit = findViewById(R.id.btnSubmit);
        
        spinnerLoanType.setText(loanTypeNames[0]);
        spinnerRepaymentMethod.setText(repaymentMethodNames[0]);
    }
    
    private void setupListeners() {
        spinnerLoanType.setOnClickListener(v -> showLoanTypeDialog());
        spinnerRepaymentMethod.setOnClickListener(v -> showRepaymentMethodDialog());
        
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etCreditCardDate.setOnClickListener(v -> showDatePicker(etCreditCardDate));
        
        btnSubmit.setOnClickListener(v -> submitLoan());
    }
    
    private void showLoanTypeDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.loan_type)
                .setItems(loanTypeNames, (dialog, which) -> {
                    selectedLoanType = loanTypes[which];
                    spinnerLoanType.setText(loanTypeNames[which]);
                    toggleLoanTypeFields();
                })
                .show();
    }
    
    private void showRepaymentMethodDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.repayment_method)
                .setItems(repaymentMethodNames, (dialog, which) -> {
                    selectedRepaymentMethod = repaymentMethods[which];
                    spinnerRepaymentMethod.setText(repaymentMethodNames[which]);
                })
                .show();
    }
    
    private void showDatePicker(TextInputEditText target) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
            target.setText(date);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }
    
    private void toggleLoanTypeFields() {
        if ("credit_card".equals(selectedLoanType)) {
            layoutNormalFields.setVisibility(View.GONE);
            layoutCreditCardFields.setVisibility(View.VISIBLE);
            selectedRepaymentMethod = "lump_sum";
            spinnerRepaymentMethod.setText(repaymentMethodNames[3]);
        } else {
            layoutNormalFields.setVisibility(View.VISIBLE);
            layoutCreditCardFields.setVisibility(View.GONE);
            selectedRepaymentMethod = "equal_interest";
            spinnerRepaymentMethod.setText(repaymentMethodNames[0]);
        }
    }
    
    private void submitLoan() {
        String name = etLoanName.getText().toString().trim();
        if (name.isEmpty()) {
            etLoanName.setError("请输入名称");
            return;
        }
        
        Loan loan = new Loan();
        loan.setName(name);
        loan.setLoanType(selectedLoanType);
        loan.setRepaymentMethod(selectedRepaymentMethod);
        
        if ("credit_card".equals(selectedLoanType)) {
            double creditLimit = NumberFormatUtil.parseDouble(etCreditLimit.getText().toString());
            int months = NumberFormatUtil.parseInt(etCreditCardMonths.getText().toString());
            double rate = NumberFormatUtil.parseDouble(etCreditCardRate.getText().toString());
            int dueDate = NumberFormatUtil.parseInt(etDueDate.getText().toString());
            String date = etCreditCardDate.getText().toString();
            
            if (creditLimit <= 0) {
                etCreditLimit.setError("请输入有效金额");
                return;
            }
            
            loan.setPrincipal(creditLimit);
            loan.setCreditLimit(creditLimit);
            loan.setMonths(months > 0 ? months : 12);
            loan.setAnnualRate(rate > 0 ? rate : 18);
            loan.setDueDate(dueDate > 0 ? dueDate : 1);
            loan.setStartDate(date.isEmpty() ? DateUtil.getCurrentDate() : date);
        } else {
            double principal = NumberFormatUtil.parseDouble(etPrincipal.getText().toString());
            double rate = NumberFormatUtil.parseDouble(etAnnualRate.getText().toString());
            int months = NumberFormatUtil.parseInt(etMonths.getText().toString());
            String date = etStartDate.getText().toString();
            
            if (principal <= 0) {
                etPrincipal.setError("请输入有效金额");
                return;
            }
            if (rate < 0) {
                etAnnualRate.setError("请输入有效利率");
                return;
            }
            if (months <= 0) {
                etMonths.setError("请输入有效期限");
                return;
            }
            
            loan.setPrincipal(principal);
            loan.setAnnualRate(rate);
            loan.setMonths(months);
            loan.setStartDate(date.isEmpty() ? DateUtil.getCurrentDate() : date);
        }
        
        // 显示确认对话框
        showConfirmDialog(loan);
    }
    
    private void showConfirmDialog(Loan loan) {
        // 加载自定义布局
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirm_loan, null);
        
        // 设置各字段值
        ((android.widget.TextView) dialogView.findViewById(R.id.tvLoanType)).setText(loan.getLoanTypeName());
        ((android.widget.TextView) dialogView.findViewById(R.id.tvLoanName)).setText(loan.getName());
        ((android.widget.TextView) dialogView.findViewById(R.id.tvRepaymentMethod)).setText(loan.getRepaymentMethodName());
        ((android.widget.TextView) dialogView.findViewById(R.id.tvPrincipal)).setText(NumberFormatUtil.formatCurrency(loan.getPrincipal()));
        ((android.widget.TextView) dialogView.findViewById(R.id.tvAnnualRate)).setText(loan.getAnnualRate() + "%");
        ((android.widget.TextView) dialogView.findViewById(R.id.tvMonths)).setText(loan.getMonths() + "个月");
        ((android.widget.TextView) dialogView.findViewById(R.id.tvStartDate)).setText(loan.getStartDate());
        
        // 如果是信用卡，显示还款日
        if (loan.isCreditCard()) {
            LinearLayout layoutDueDate = dialogView.findViewById(R.id.layoutDueDate);
            android.widget.TextView tvDueDate = dialogView.findViewById(R.id.tvDueDate);
            layoutDueDate.setVisibility(View.VISIBLE);
            tvDueDate.setText("每月" + loan.getDueDate() + "号");
        }
        
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_add)
                .setView(dialogView)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    // 在后台线程执行数据库插入
                    executorService.execute(() -> {
                        repository.insertLoan(loan);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
