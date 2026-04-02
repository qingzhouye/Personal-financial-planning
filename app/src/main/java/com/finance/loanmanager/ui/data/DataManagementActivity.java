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
            try {
                // 重新获取数据库实例，确保连接正常
                System.out.println("导出数据调试 - 开始获取数据");
                
                List<Loan> loans = repository.getAllLoansSync();
                List<Payment> payments = repository.getAllPaymentsSync();
                
                // 确保列表不为null
                if (loans == null) loans = new ArrayList<>();
                if (payments == null) payments = new ArrayList<>();
                
                // 调试日志：检查数据是否为空
                System.out.println("导出数据调试 - 贷款数量: " + loans.size());
                System.out.println("导出数据调试 - 还款记录数量: " + payments.size());
                
                // 打印第一条贷款数据用于调试
                if (!loans.isEmpty()) {
                    Loan firstLoan = loans.get(0);
                    System.out.println("导出数据调试 - 第一条贷款: ID=" + firstLoan.getId() + ", 名称=" + firstLoan.getName());
                }
                
                // 创建 XLSX 工作簿
                System.out.println("导出数据调试 - 开始创建XSSFWorkbook");
                Workbook workbook = new XSSFWorkbook();
                System.out.println("导出数据调试 - XSSFWorkbook创建成功");
                
                // 创建贷款信息工作表
                Sheet loanSheet = workbook.createSheet(SHEET_LOANS);
                createLoanSheet(loanSheet, loans);
                
                // 创建还款记录工作表
                Sheet paymentSheet = workbook.createSheet(SHEET_PAYMENTS);
                createPaymentSheet(paymentSheet, payments);
                
                // 写入文件
                System.out.println("导出数据调试 - 开始写入文件，uri=" + uri);
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    System.out.println("导出数据调试 - 输出流打开成功");
                    
                    // 写入工作簿到输出流
                    workbook.write(outputStream);
                    System.out.println("导出数据调试 - workbook.write() 完成");
                    
                    outputStream.flush();
                    System.out.println("导出数据调试 - flush() 完成");
                    
                    outputStream.close();
                    System.out.println("导出数据调试 - 输出流关闭");
                    
                    workbook.close();
                    System.out.println("导出数据调试 - 工作簿关闭");
                    
                    System.out.println("导出数据调试 - 文件写入成功");
                    runOnUiThread(() -> Toast.makeText(this, R.string.export_success, Toast.LENGTH_SHORT).show());
                } else {
                    System.err.println("导出数据调试 - 无法打开输出流，uri=" + uri);
                    runOnUiThread(() -> Toast.makeText(this, R.string.export_failed + ": 无法创建文件", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                final String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.export_failed) + ": " + errorMsg, Toast.LENGTH_LONG).show());
            }
            runOnUiThread(this::finish);
        });
    }
    
    /**
     * 创建贷款信息工作表
     * XLSX 模板结构:
     * 列A: ID | 列B: 贷款名称 | 列C: 贷款类型 | 列D: 还款方式 | 列E: 本金 | 列F: 年利率(%) | 列G: 期限(月) | 列H: 开始日期 | 列I: 信用卡额度 | 列J: 还款日 | 列K: 原始月供
     */
    private void createLoanSheet(Sheet sheet, List<Loan> loans) {
        System.out.println("createLoanSheet - 开始创建工作表，数据条数: " + (loans != null ? loans.size() : 0));
        
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
        System.out.println("createLoanSheet - 标题行创建完成");
        
        // 填充数据
        int rowNum = 1;
        if (loans != null) {
            for (Loan loan : loans) {
                Row row = sheet.createRow(rowNum++);
                
                // ID - 整数
                Cell idCell = row.createCell(0);
                idCell.setCellValue(loan.getId());
                
                // 贷款名称 - 字符串
                Cell nameCell = row.createCell(1);
                nameCell.setCellValue(loan.getName() != null ? loan.getName() : "");
                
                // 贷款类型 - 字符串
                Cell typeCell = row.createCell(2);
                typeCell.setCellValue(loan.getLoanType() != null ? loan.getLoanType() : "");
                
                // 还款方式 - 字符串
                Cell methodCell = row.createCell(3);
                methodCell.setCellValue(loan.getRepaymentMethod() != null ? loan.getRepaymentMethod() : "");
                
                // 本金 - 数值
                Cell principalCell = row.createCell(4);
                principalCell.setCellValue(loan.getPrincipal());
                
                // 年利率 - 数值
                Cell rateCell = row.createCell(5);
                rateCell.setCellValue(loan.getAnnualRate());
                
                // 期限 - 整数
                Cell monthsCell = row.createCell(6);
                monthsCell.setCellValue(loan.getMonths());
                
                // 开始日期 - 字符串
                Cell dateCell = row.createCell(7);
                dateCell.setCellValue(loan.getStartDate() != null ? loan.getStartDate() : "");
                
                // 信用卡额度 - 数值
                Cell limitCell = row.createCell(8);
                limitCell.setCellValue(loan.getCreditLimit());
                
                // 还款日 - 整数
                Cell dueCell = row.createCell(9);
                dueCell.setCellValue(loan.getDueDate());
                
                // 原始月供 - 数值
                Cell monthlyCell = row.createCell(10);
                monthlyCell.setCellValue(loan.getOriginalMonthlyPayment());
            }
        }
        System.out.println("createLoanSheet - 数据填充完成，共 " + (rowNum - 1) + " 行数据");
        
        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        System.out.println("createLoanSheet - 工作表创建完成");
    }
    
    /**
     * 创建还款记录工作表
     * XLSX 模板结构:
     * 列A: ID | 列B: 贷款ID | 列C: 还款金额 | 列D: 还款日期 | 列E: 备注
     */
    private void createPaymentSheet(Sheet sheet, List<Payment> payments) {
        System.out.println("createPaymentSheet - 开始创建工作表，数据条数: " + (payments != null ? payments.size() : 0));
        
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
        System.out.println("createPaymentSheet - 标题行创建完成");
        
        // 填充数据
        int rowNum = 1;
        if (payments != null) {
            for (Payment payment : payments) {
                Row row = sheet.createRow(rowNum++);
                
                // ID - 整数
                Cell idCell = row.createCell(0);
                idCell.setCellValue(payment.getId());
                
                // 贷款ID - 整数
                Cell loanIdCell = row.createCell(1);
                loanIdCell.setCellValue(payment.getLoanId());
                
                // 还款金额 - 数值
                Cell amountCell = row.createCell(2);
                amountCell.setCellValue(payment.getAmount());
                
                // 还款日期 - 字符串
                Cell dateCell = row.createCell(3);
                dateCell.setCellValue(payment.getDate() != null ? payment.getDate() : "");
                
                // 备注 - 字符串
                Cell noteCell = row.createCell(4);
                noteCell.setCellValue(payment.getNote() != null ? payment.getNote() : "");
            }
        }
        System.out.println("createPaymentSheet - 数据填充完成，共 " + (rowNum - 1) + " 行数据");
        
        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        System.out.println("createPaymentSheet - 工作表创建完成");
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
                if (DateUtil.isCellDateFormatted(cell)) {
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
