package com.finance.loanmanager.ui.data;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.finance.loanmanager.R;
import com.finance.loanmanager.data.AppDatabase;
import com.finance.loanmanager.data.entity.Loan;
import com.finance.loanmanager.data.entity.Payment;
import com.finance.loanmanager.repository.LoanRepository;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataManagementActivity extends AppCompatActivity {
    
    private LoanRepository repository;
    private ExecutorService executorService;
    
    // 使用新的 Activity Result API
    private ActivityResultLauncher<Intent> exportLauncher;
    private ActivityResultLauncher<Intent> importLauncher;
    
    // XLSX 工作表名称
    private static final String SHEET_LOANS = "贷款信息";
    private static final String SHEET_PAYMENTS = "还款记录";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 适配 Android 15/16 Edge-to-Edge 安全区域
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        repository = new LoanRepository(getApplication());
        executorService = Executors.newSingleThreadExecutor();
        
        // 注册 Activity Result Launchers
        exportLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        exportData(uri);
                    } else {
                        finish();
                    }
                } else {
                    finish();
                }
            }
        );
        
        importLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        confirmImport(uri);
                    } else {
                        finish();
                    }
                } else {
                    finish();
                }
            }
        );
        
        String action = getIntent().getAction();
        if ("EXPORT".equals(action)) {
            startExport();
        } else if ("IMPORT".equals(action)) {
            startImport();
        } else {
            finish();
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
    
    private void startExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        // 使用更简单的文件名格式
        String fileName = "loan_data_" + System.currentTimeMillis() + ".xlsx";
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        exportLauncher.launch(intent);
    }
    
    private void startImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        importLauncher.launch(intent);
    }
    
    private void exportData(Uri uri) {
        // 在后台线程执行导出操作
        executorService.execute(() -> {
            OutputStream outputStream = null;
            Workbook workbook = null;
            try {
                List<Loan> loans = repository.getAllLoansSync();
                List<Payment> payments = repository.getAllPaymentsSync();
                
                // 确保列表不为null
                if (loans == null) loans = new ArrayList<>();
                if (payments == null) payments = new ArrayList<>();
                
                // 调试日志：检查数据是否为空
                System.out.println("导出数据调试 - 贷款数量: " + loans.size());
                System.out.println("导出数据调试 - 还款记录数量: " + payments.size());
                
                if (loans.isEmpty() && payments.isEmpty()) {
                    System.err.println("导出数据调试 - 警告：数据库中没有数据");
                }
                
                // 创建 XLSX 工作簿
                System.out.println("导出数据调试 - 开始创建XSSFWorkbook");
                workbook = new XSSFWorkbook();
                System.out.println("导出数据调试 - XSSFWorkbook创建成功");
                
                // 创建贷款信息工作表
                Sheet loanSheet = workbook.createSheet(SHEET_LOANS);
                createLoanSheet(loanSheet, loans);
                
                // 创建还款记录工作表
                Sheet paymentSheet = workbook.createSheet(SHEET_PAYMENTS);
                createPaymentSheet(paymentSheet, payments);
                
                // 写入文件
                outputStream = getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    System.out.println("导出数据调试 - 开始写入文件...");
                    workbook.write(outputStream);
                    outputStream.flush();
                    System.out.println("导出数据调试 - 文件写入成功");
                    runOnUiThread(() -> Toast.makeText(this, R.string.export_success, Toast.LENGTH_SHORT).show());
                } else {
                    System.err.println("导出数据调试 - 无法打开输出流，uri=" + uri);
                    runOnUiThread(() -> Toast.makeText(this, R.string.export_failed + ": 无法创建文件", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                System.err.println("导出数据调试 - 发生异常: " + e.getClass().getName());
                System.err.println("导出数据调试 - 异常消息: " + e.getMessage());
                e.printStackTrace();
                final String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.export_failed) + ": " + errorMsg, Toast.LENGTH_LONG).show());
            } finally {
                // 确保资源正确关闭
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                    if (workbook != null) {
                        workbook.close();
                    }
                } catch (Exception e) {
                    System.err.println("导出数据调试 - 关闭资源时发生异常: " + e.getMessage());
                }
                runOnUiThread(this::finish);
            }
        });
    }
    
    /**
     * 创建贷款信息工作表
     * XLSX 模板结构:
     * 列A: ID | 列B: 贷款名称 | 列C: 贷款类型 | 列D: 还款方式 | 列E: 本金 | 列F: 年利率(%) | 列G: 期限(月) | 列H: 开始日期 | 列I: 信用卡额度 | 列J: 还款日 | 列K: 原始月供
     */
    private void createLoanSheet(Sheet sheet, List<Loan> loans) {
        // 创建标题行样式
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        // 创建标题行
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "贷款名称", "贷款类型", "还款方式", "本金", "年利率(%)", "期限(月)", "开始日期", "信用卡额度", "还款日", "原始月供"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // 填充数据
        int rowNum = 1;
        for (Loan loan : loans) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(loan.getId());
            row.createCell(1).setCellValue(loan.getName() != null ? loan.getName() : "");
            row.createCell(2).setCellValue(loan.getLoanType() != null ? loan.getLoanType() : "");
            row.createCell(3).setCellValue(loan.getRepaymentMethod() != null ? loan.getRepaymentMethod() : "");
            row.createCell(4).setCellValue(loan.getPrincipal());
            row.createCell(5).setCellValue(loan.getAnnualRate());
            row.createCell(6).setCellValue(loan.getMonths());
            row.createCell(7).setCellValue(loan.getStartDate() != null ? loan.getStartDate() : "");
            row.createCell(8).setCellValue(loan.getCreditLimit());
            row.createCell(9).setCellValue(loan.getDueDate());
            row.createCell(10).setCellValue(loan.getOriginalMonthlyPayment());
        }
        
        // 设置固定列宽（Android不支持autoSizeColumn，因为缺少AWT类）
        // 列宽单位: 1/256个字符宽度
        int[] columnWidths = {2500, 6000, 4000, 4000, 4000, 3500, 3000, 4000, 4000, 3000, 4000};
        for (int i = 0; i < headers.length && i < columnWidths.length; i++) {
            sheet.setColumnWidth(i, columnWidths[i]);
        }
        System.out.println("导出数据调试 - 贷款工作表创建完成，数据行数: " + (rowNum - 1));
    }
    
    /**
     * 创建还款记录工作表
     * XLSX 模板结构:
     * 列A: ID | 列B: 贷款ID | 列C: 还款金额 | 列D: 还款日期 | 列E: 备注
     */
    private void createPaymentSheet(Sheet sheet, List<Payment> payments) {
        // 创建标题行样式
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        // 创建标题行
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "贷款ID", "还款金额", "还款日期", "备注"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // 填充数据
        int rowNum = 1;
        for (Payment payment : payments) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(payment.getId());
            row.createCell(1).setCellValue(payment.getLoanId());
            row.createCell(2).setCellValue(payment.getAmount());
            row.createCell(3).setCellValue(payment.getDate() != null ? payment.getDate() : "");
            row.createCell(4).setCellValue(payment.getNote() != null ? payment.getNote() : "");
        }
        
        // 设置固定列宽（Android不支持autoSizeColumn，因为缺少AWT类）
        int[] columnWidths = {2500, 4000, 4000, 4000, 6000};
        for (int i = 0; i < headers.length && i < columnWidths.length; i++) {
            sheet.setColumnWidth(i, columnWidths[i]);
        }
        System.out.println("导出数据调试 - 还款记录工作表创建完成，数据行数: " + (rowNum - 1));
    }
    
    private void confirmImport(Uri uri) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.import_data)
                .setMessage(R.string.import_confirm)
                .setPositiveButton(R.string.confirm, (dialog, which) -> importData(uri))
                .setNegativeButton(R.string.cancel, (dialog, which) -> finish())
                .show();
    }
    
    private void importData(Uri uri) {
        // 在后台线程执行导入操作
        executorService.execute(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream != null) {
                    // 读取 XLSX 文件
                    Workbook workbook = new XSSFWorkbook(inputStream);
                    
                    // 解析贷款信息
                    List<Loan> loans = parseLoanSheet(workbook.getSheet(SHEET_LOANS));
                    
                    // 解析还款记录
                    List<Payment> payments = parsePaymentSheet(workbook.getSheet(SHEET_PAYMENTS));
                    
                    workbook.close();
                    inputStream.close();
                    
                    // 清空现有数据
                    repository.deleteAllPayments();
                    repository.deleteAllLoans();
                    
                    // 导入新数据
                    for (Loan loan : loans) {
                        loan.setId(0); // 重置ID
                        repository.insertLoan(loan);
                    }
                    
                    for (Payment payment : payments) {
                        payment.setId(0); // 重置ID
                        repository.insertPayment(payment);
                    }
                    
                    runOnUiThread(() -> Toast.makeText(this, R.string.import_success, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, R.string.import_failed + ": " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
            runOnUiThread(this::finish);
        });
    }
    
    /**
     * 解析贷款信息工作表
     * XLSX 模板结构:
     * 列A: ID | 列B: 贷款名称 | 列C: 贷款类型 | 列D: 还款方式 | 列E: 本金 | 列F: 年利率(%) | 列G: 期限(月) | 列H: 开始日期 | 列I: 信用卡额度 | 列J: 还款日 | 列K: 原始月供
     */
    private List<Loan> parseLoanSheet(Sheet sheet) {
        List<Loan> loans = new ArrayList<>();
        if (sheet == null) return loans;
        
        Iterator<Row> rowIterator = sheet.iterator();
        // 跳过标题行
        if (rowIterator.hasNext()) {
            rowIterator.next();
        }
        
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Loan loan = new Loan();
            
            // 读取各列数据
            Cell nameCell = row.getCell(1);
            if (nameCell != null) {
                loan.setName(getCellStringValue(nameCell));
            }
            
            Cell loanTypeCell = row.getCell(2);
            if (loanTypeCell != null) {
                loan.setLoanType(getCellStringValue(loanTypeCell));
            }
            
            Cell repaymentMethodCell = row.getCell(3);
            if (repaymentMethodCell != null) {
                loan.setRepaymentMethod(getCellStringValue(repaymentMethodCell));
            }
            
            Cell principalCell = row.getCell(4);
            if (principalCell != null) {
                loan.setPrincipal(getCellNumericValue(principalCell));
            }
            
            Cell annualRateCell = row.getCell(5);
            if (annualRateCell != null) {
                loan.setAnnualRate(getCellNumericValue(annualRateCell));
            }
            
            Cell monthsCell = row.getCell(6);
            if (monthsCell != null) {
                loan.setMonths((int) getCellNumericValue(monthsCell));
            }
            
            Cell startDateCell = row.getCell(7);
            if (startDateCell != null) {
                loan.setStartDate(getCellStringValue(startDateCell));
            }
            
            Cell creditLimitCell = row.getCell(8);
            if (creditLimitCell != null) {
                loan.setCreditLimit(getCellNumericValue(creditLimitCell));
            }
            
            Cell dueDateCell = row.getCell(9);
            if (dueDateCell != null) {
                loan.setDueDate((int) getCellNumericValue(dueDateCell));
            }
            
            Cell originalMonthlyPaymentCell = row.getCell(10);
            if (originalMonthlyPaymentCell != null) {
                loan.setOriginalMonthlyPayment(getCellNumericValue(originalMonthlyPaymentCell));
            }
            
            // 只添加有效的贷款记录（必须有名称）
            if (loan.getName() != null && !loan.getName().trim().isEmpty()) {
                loans.add(loan);
            }
        }
        
        return loans;
    }
    
    /**
     * 解析还款记录工作表
     * XLSX 模板结构:
     * 列A: ID | 列B: 贷款ID | 列C: 还款金额 | 列D: 还款日期 | 列E: 备注
     */
    private List<Payment> parsePaymentSheet(Sheet sheet) {
        List<Payment> payments = new ArrayList<>();
        if (sheet == null) return payments;
        
        Iterator<Row> rowIterator = sheet.iterator();
        // 跳过标题行
        if (rowIterator.hasNext()) {
            rowIterator.next();
        }
        
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Payment payment = new Payment();
            
            // 读取各列数据
            Cell loanIdCell = row.getCell(1);
            if (loanIdCell != null) {
                payment.setLoanId((int) getCellNumericValue(loanIdCell));
            }
            
            Cell amountCell = row.getCell(2);
            if (amountCell != null) {
                payment.setAmount(getCellNumericValue(amountCell));
            }
            
            Cell dateCell = row.getCell(3);
            if (dateCell != null) {
                payment.setDate(getCellStringValue(dateCell));
            }
            
            Cell noteCell = row.getCell(4);
            if (noteCell != null) {
                payment.setNote(getCellStringValue(noteCell));
            }
            
            // 只添加有效的还款记录（必须有贷款ID和金额）
            if (payment.getLoanId() > 0 && payment.getAmount() > 0) {
                payments.add(payment);
            }
        }
        
        return payments;
    }
    
    /**
     * 获取单元格的字符串值
     */
    private String getCellStringValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                // 检查是否为日期格式
                if (isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
    
    /**
     * 检查单元格是否为日期格式
     * 替代 Apache POI 的 DateUtil.isCellDateFormatted
     */
    private boolean isCellDateFormatted(Cell cell) {
        try {
            short dataFormat = cell.getCellStyle().getDataFormat();
            // 常见的日期格式索引
            return dataFormat == 0x0e || dataFormat == 0x0f || dataFormat == 0x10 ||
                   dataFormat == 0x11 || dataFormat == 0x12 || dataFormat == 0x13 ||
                   dataFormat == 0x14 || dataFormat == 0x15 || dataFormat == 0x16 ||
                   dataFormat == 0x2d || dataFormat == 0x2e || dataFormat == 0x2f;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取单元格的数值
     */
    private double getCellNumericValue(Cell cell) {
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    return Double.parseDouble(cell.getStringCellValue());
                } catch (NumberFormatException e) {
                    return 0;
                }
            case FORMULA:
                try {
                    return cell.getNumericCellValue();
                } catch (Exception e) {
                    return 0;
                }
            default:
                return 0;
        }
    }
}
