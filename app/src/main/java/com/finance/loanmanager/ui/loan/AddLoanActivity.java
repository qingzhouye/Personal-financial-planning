/**
 * ============================================================================
 * 文件名: AddLoanActivity.java
 * 模块:   UI层 - 贷款添加
 * 功能:   添加新贷款的界面，支持多种贷款类型
 * 
 * 主要职责:
 *   1. 提供贷款信息输入表单
 *   2. 支持三种贷款类型：普通贷款、信用卡、国家助学贷款
 *   3. 根据贷款类型动态切换输入字段
 *   4. 验证输入数据并保存到数据库
 * 
 * 贷款类型说明:
 *   - 普通贷款: 需要本金、利率、期限、开始日期
 *   - 信用卡: 需要额度、期数、还款日、利率
 *   - 国家助学贷款: 需要年利率、年还款额、初始本金余额
 * 
 * 还款方式支持:
 *   - 等额本息: 每月还款额固定（默认）
 *   - 等额本金: 每月本金固定，利息递减
 *   - 先息后本: 前期只还利息，末期还本金
 *   - 利随本清: 到期一次性还本付息
 * 
 * @see Loan 贷款实体
 * @see LoanRepository 数据仓库
 * ============================================================================
 */
package com.finance.loanmanager.ui.loan;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.finance.loanmanager.R;
import com.finance.loanmanager.data.entity.Loan;
import com.finance.loanmanager.repository.LoanRepository;
import com.finance.loanmanager.ui.BaseActivity;
import com.finance.loanmanager.util.DateUtil;
import com.finance.loanmanager.util.NumberFormatUtil;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 添加贷款界面 Activity
 * 
 * 该界面提供表单让用户输入新贷款的信息。根据贷款类型的不同，
 * 界面会动态显示不同的输入字段。
 * 
 * 表单验证:
 *   - 名称不能为空
 *   - 本金/额度必须大于0
 *   - 利率不能为负数
 *   - 期限必须大于0
 */
public class AddLoanActivity extends BaseActivity {

    // ==================== 成员变量 ====================
    
    /** 数据仓库引用 */
    private LoanRepository repository;
    
    /** 后台线程执行器 */
    private ExecutorService executorService;
    
    /** 贷款类型选择输入框 */
    private TextInputEditText spinnerLoanType;
    
    /** 贷款名称输入框 */
    private TextInputEditText etLoanName;
    
    /** 还款方式选择输入框 */
    private TextInputEditText spinnerRepaymentMethod;
    
    /** 基本信息布局 */
    private LinearLayout layoutBasicInfo;
    
    /** 普通贷款字段布局 */
    private LinearLayout layoutNormalFields;
    
    /** 信用卡字段布局 */
    private LinearLayout layoutCreditCardFields;
    
    /** 国家助学贷款字段布局 */
    private LinearLayout layoutStudentLoanFields;
    
    /** 本金输入框 */
    private TextInputEditText etPrincipal;
    
    /** 年利率输入框 */
    private TextInputEditText etAnnualRate;
    
    /** 期限输入框 */
    private TextInputEditText etMonths;
    
    /** 开始日期输入框 */
    private TextInputEditText etStartDate;
    
    /** 信用卡额度输入框 */
    private TextInputEditText etCreditLimit;
    
    /** 信用卡期数输入框 */
    private TextInputEditText etCreditCardMonths;
    
    /** 信用卡开始日期输入框 */
    private TextInputEditText etCreditCardDate;
    
    /** 还款日输入框 */
    private TextInputEditText etDueDate;
    
    /** 信用卡利率输入框 */
    private TextInputEditText etCreditCardRate;
    
    /** 国家助学贷款利率输入框 */
    private TextInputEditText etStudentLoanRate;
    
    /** 年还款额输入框 */
    private TextInputEditText etYearlyPayment;
    
    /** 初始本金余额输入框 */
    private TextInputEditText etFirstYearBalance;
    
    /** 国家助学贷款开始日期输入框 */
    private TextInputEditText etStudentLoanDate;
    
    /** 提交按钮 */
    private Button btnSubmit;
    
    // ==================== 贷款类型和还款方式常量 ====================
    
    /** 当前选择的贷款类型 */
    private String selectedLoanType = "normal";
    
    /** 当前选择的还款方式 */
    private String selectedRepaymentMethod = "equal_interest";
    
    /** 贷款类型代码数组 */
    private final String[] loanTypes = {"normal", "credit_card", "student_loan"};
    
    /** 贷款类型名称数组 */
    private final String[] loanTypeNames = {"普通贷款", "信用卡", "国家助学贷款"};
    
    /** 还款方式代码数组 */
    private final String[] repaymentMethods = {"equal_interest", "equal_principal", "interest_first", "lump_sum"};
    
    /** 还款方式名称数组 */
    private final String[] repaymentMethodNames = {"等额本息", "等额本金", "先息后本", "利随本清"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_loan);
        
        // 适配 Android 15/16 Edge-to-Edge 安全区域
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.add_loan);
        
        repository = new LoanRepository(getApplication());
        executorService = Executors.newSingleThreadExecutor();
        
        initViews();
        setupListeners();
        applyTransparentCardStyle();
        
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
        layoutBasicInfo = findViewById(R.id.layoutBasicInfo);
        layoutNormalFields = findViewById(R.id.layoutNormalFields);
        layoutCreditCardFields = findViewById(R.id.layoutCreditCardFields);
        layoutStudentLoanFields = findViewById(R.id.layoutStudentLoanFields);
        etPrincipal = findViewById(R.id.etPrincipal);
        etAnnualRate = findViewById(R.id.etAnnualRate);
        etMonths = findViewById(R.id.etMonths);
        etStartDate = findViewById(R.id.etStartDate);
        etCreditLimit = findViewById(R.id.etCreditLimit);
        etCreditCardMonths = findViewById(R.id.etCreditCardMonths);
        etCreditCardDate = findViewById(R.id.etCreditCardDate);
        etDueDate = findViewById(R.id.etDueDate);
        etCreditCardRate = findViewById(R.id.etCreditCardRate);
        etStudentLoanRate = findViewById(R.id.etStudentLoanRate);
        etYearlyPayment = findViewById(R.id.etYearlyPayment);
        etFirstYearBalance = findViewById(R.id.etFirstYearBalance);
        etStudentLoanDate = findViewById(R.id.etStudentLoanDate);
        btnSubmit = findViewById(R.id.btnSubmit);
        
        spinnerLoanType.setText(loanTypeNames[0]);
        spinnerRepaymentMethod.setText(repaymentMethodNames[0]);
    }
    
    private void setupListeners() {
        spinnerLoanType.setOnClickListener(v -> showLoanTypeDialog());
        spinnerRepaymentMethod.setOnClickListener(v -> showRepaymentMethodDialog());
        
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etCreditCardDate.setOnClickListener(v -> showDatePicker(etCreditCardDate));
        etStudentLoanDate.setOnClickListener(v -> showDatePicker(etStudentLoanDate));
        
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
            layoutStudentLoanFields.setVisibility(View.GONE);
            selectedRepaymentMethod = "lump_sum";
            spinnerRepaymentMethod.setText(repaymentMethodNames[3]);
        } else if ("student_loan".equals(selectedLoanType)) {
            layoutNormalFields.setVisibility(View.GONE);
            layoutCreditCardFields.setVisibility(View.GONE);
            layoutStudentLoanFields.setVisibility(View.VISIBLE);
            selectedRepaymentMethod = "lump_sum";
            spinnerRepaymentMethod.setText(repaymentMethodNames[3]);
        } else {
            layoutNormalFields.setVisibility(View.VISIBLE);
            layoutCreditCardFields.setVisibility(View.GONE);
            layoutStudentLoanFields.setVisibility(View.GONE);
            selectedRepaymentMethod = "equal_interest";
            spinnerRepaymentMethod.setText(repaymentMethodNames[0]);
        }
    }
    
    /**
     * 当设置了自定义背景时，应用透明卡片样式
     */
    private void applyTransparentCardStyle() {
        boolean hasCustomBg = getBackgroundManager() != null && getBackgroundManager().hasCustomBackground();
        
        // 设置基本信息区域背景
        if (layoutBasicInfo != null) {
            if (hasCustomBg) {
                layoutBasicInfo.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            } else {
                layoutBasicInfo.setBackgroundColor(getResources().getColor(R.color.card_background_alt));
            }
        }
        
        // 设置普通贷款信息区域背景
        if (layoutNormalFields != null) {
            if (hasCustomBg) {
                layoutNormalFields.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            } else {
                layoutNormalFields.setBackgroundColor(getResources().getColor(R.color.card_background_alt));
            }
        }
        
        // 设置信用卡信息区域背景
        if (layoutCreditCardFields != null) {
            if (hasCustomBg) {
                layoutCreditCardFields.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            } else {
                layoutCreditCardFields.setBackgroundColor(getResources().getColor(R.color.card_background_alt));
            }
        }
        
        // 设置国家助学贷款信息区域背景
        if (layoutStudentLoanFields != null) {
            if (hasCustomBg) {
                layoutStudentLoanFields.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            } else {
                layoutStudentLoanFields.setBackgroundColor(getResources().getColor(R.color.card_background_alt));
            }
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
            String rateStr = etCreditCardRate.getText().toString().trim();
            double rate = NumberFormatUtil.parseDouble(rateStr);
            int dueDate = NumberFormatUtil.parseInt(etDueDate.getText().toString());
            String date = etCreditCardDate.getText().toString();
            
            if (creditLimit <= 0) {
                etCreditLimit.setError("请输入有效金额");
                return;
            }
            
            // 如果用户输入了内容但解析失败，显示错误提示
            if (!rateStr.isEmpty() && rate == 0 && !rateStr.equals("0") && !rateStr.equals("0.0") && !rateStr.equals("0.00")) {
                etCreditCardRate.setError("请输入有效的利率值");
                return;
            }
            
            loan.setPrincipal(creditLimit);
            loan.setCreditLimit(creditLimit);
            loan.setMonths(months > 0 ? months : 12);
            // 如果用户没有输入或输入为空，使用默认值18%；否则使用用户输入的值
            loan.setAnnualRate(rateStr.isEmpty() ? 18 : rate);
            loan.setDueDate(dueDate > 0 ? dueDate : 1);
            loan.setStartDate(date.isEmpty() ? DateUtil.getCurrentDate() : date);
        } else if ("student_loan".equals(selectedLoanType)) {
            // 国家助学贷款：固定10年（120个月），固定还款日12月20日
            double rate = NumberFormatUtil.parseDouble(etStudentLoanRate.getText().toString());
            double yearlyPayment = NumberFormatUtil.parseDouble(etYearlyPayment.getText().toString());
            double firstYearBalance = NumberFormatUtil.parseDouble(etFirstYearBalance.getText().toString());
            String date = etStudentLoanDate.getText().toString();
            
            if (rate < 0) {
                etStudentLoanRate.setError("请输入有效利率");
                return;
            }
            if (yearlyPayment <= 0) {
                etYearlyPayment.setError("请输入每年还款金额");
                return;
            }
            if (firstYearBalance < 0) {
                etFirstYearBalance.setError("请输入初始本金余额");
                return;
            }
            
            // 国家助学贷款固定参数
            loan.setAnnualRate(rate);
            loan.setMonths(120); // 10年 = 120个月
            loan.setDueDate(20); // 固定还款日12月20日
            loan.setStartDate(date.isEmpty() ? DateUtil.getCurrentDate() : date);
            loan.setYearlyPayment(yearlyPayment);
            loan.setFirstYearBalance(firstYearBalance);
            // 本金设为初始余额，用于后续计算
            loan.setPrincipal(firstYearBalance);
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
        
        // 如果是国家助学贷款，显示特定信息
        if (loan.isStudentLoan()) {
            LinearLayout layoutDueDate = dialogView.findViewById(R.id.layoutDueDate);
            android.widget.TextView tvDueDate = dialogView.findViewById(R.id.tvDueDate);
            layoutDueDate.setVisibility(View.VISIBLE);
            tvDueDate.setText("每年12月20日");
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
