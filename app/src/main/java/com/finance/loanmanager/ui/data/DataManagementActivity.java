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
    
    // 浣跨敤鏂扮殑 Activity Result API
    private ActivityResultLauncher<Intent> exportLauncher;
    private ActivityResultLauncher<Intent> importLauncher;
    
    // XLSX 宸ヤ綔琛ㄥ悕绉?    private static final String SHEET_LOANS = "璐锋淇℃伅";
    private static final String SHEET_PAYMENTS = "杩樻璁板綍";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 閫傞厤 Android 15/16 Edge-to-Edge 瀹夊叏鍖哄煙
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        repository = new LoanRepository(getApplication());
        executorService = Executors.newSingleThreadExecutor();
        
        // 娉ㄥ唽 Activity Result Launchers
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
        // 浣跨敤鏇寸畝鍗曠殑鏂囦欢鍚嶆牸寮?        String fileName = "loan_data_" + System.currentTimeMillis() + ".xlsx";
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
        // 鍦ㄥ悗鍙扮嚎绋嬫墽琛屽鍑烘搷浣?        executorService.execute(() -> {
            OutputStream outputStream = null;
            Workbook workbook = null;
            try {
                List<Loan> loans = repository.getAllLoansSync();
                List<Payment> payments = repository.getAllPaymentsSync();
                
                // 纭繚鍒楄〃涓嶄负null
                if (loans == null) loans = new ArrayList<>();
                if (payments == null) payments = new ArrayList<>();
                
                // 璋冭瘯鏃ュ織锛氭鏌ユ暟鎹槸鍚︿负绌?                System.out.println("瀵煎嚭鏁版嵁璋冭瘯 - 璐锋鏁伴噺: " + loans.size());
                System.out.println("瀵煎嚭鏁版嵁璋冭瘯 - 杩樻璁板綍鏁伴噺: " + payments.size());
                
                if (loans.isEmpty() && payments.isEmpty()) {
                    System.err.println("瀵煎嚭鏁版嵁璋冭瘯 - 璀﹀憡锛氭暟鎹簱涓病鏈夋暟鎹?);
                }
                
                // 鍒涘缓 XLSX 宸ヤ綔绨?                System.out.println("瀵煎嚭鏁版嵁璋冭瘯 - 寮€濮嬪垱寤篨SSFWorkbook");
                workbook = new XSSFWorkbook();
                System.out.println("瀵煎嚭鏁版嵁璋冭瘯 - XSSFWorkbook鍒涘缓鎴愬姛");
                
                // 鍒涘缓璐锋淇℃伅宸ヤ綔琛?                Sheet loanSheet = workbook.createSheet(SHEET_LOANS);
                createLoanSheet(loanSheet, loans);
                
                // 鍒涘缓杩樻璁板綍宸ヤ綔琛?                Sheet paymentSheet = workbook.createSheet(SHEET_PAYMENTS);
                createPaymentSheet(paymentSheet, payments);
                
                // 鍐欏叆鏂囦欢
                outputStream = getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    System.out.println("瀵煎嚭鏁版嵁璋冭瘯 - 寮€濮嬪啓鍏ユ枃浠?..");
                    workbook.write(outputStream);
                    outputStream.flush();
                    System.out.println("瀵煎嚭鏁版嵁璋冭瘯 - 鏂囦欢鍐欏叆鎴愬姛");
                    runOnUiThread(() -> Toast.makeText(this, R.string.export_success, Toast.LENGTH_SHORT).show());
                } else {
                    System.err.println("瀵煎嚭鏁版嵁璋冭瘯 - 鏃犳硶鎵撳紑杈撳嚭娴侊紝uri=" + uri);
                    runOnUiThread(() -> Toast.makeText(this, R.string.export_failed + ": 鏃犳硶鍒涘缓鏂囦欢", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                System.err.println("瀵煎嚭鏁版嵁璋冭瘯 - 鍙戠敓寮傚父: " + e.getClass().getName());
                System.err.println("瀵煎嚭鏁版嵁璋冭瘯 - 寮傚父娑堟伅: " + e.getMessage());
                e.printStackTrace();
                final String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.export_failed) + ": " + errorMsg, Toast.LENGTH_LONG).show());
            } finally {
                // 纭繚璧勬簮姝ｇ‘鍏抽棴
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                    if (workbook != null) {
                        workbook.close();
                    }
                } catch (Exception e) {
                    System.err.println("瀵煎嚭鏁版嵁璋冭瘯 - 鍏抽棴璧勬簮鏃跺彂鐢熷紓甯? " + e.getMessage());
                }
                runOnUiThread(this::finish);
            }
        });
    }
    
    /**
     * 鍒涘缓璐锋淇℃伅宸ヤ綔琛?     * XLSX 妯℃澘缁撴瀯:
     * 鍒桝: ID | 鍒桞: 璐锋鍚嶇О | 鍒桟: 璐锋绫诲瀷 | 鍒桪: 杩樻鏂瑰紡 | 鍒桬: 鏈噾 | 鍒桭: 骞村埄鐜?%) | 鍒桮: 鏈熼檺(鏈? | 鍒桯: 寮€濮嬫棩鏈?| 鍒桰: 淇＄敤鍗￠搴?| 鍒桱: 杩樻鏃?| 鍒桲: 鍘熷鏈堜緵
     */
    private void createLoanSheet(Sheet sheet, List<Loan> loans) {
        // 鍒涘缓鏍囬琛屾牱寮?        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        // 鍒涘缓鏍囬琛?        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "璐锋鍚嶇О", "璐锋绫诲瀷", "杩樻鏂瑰紡", "鏈噾", "骞村埄鐜?%)", "鏈熼檺(鏈?", "寮€濮嬫棩鏈?, "淇＄敤鍗￠搴?, "杩樻鏃?, "鍘熷鏈堜緵"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // 濉厖鏁版嵁
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
        
        // 璁剧疆鍥哄畾鍒楀锛圓ndroid涓嶆敮鎸乤utoSizeColumn锛屽洜涓虹己灏慉WT绫伙級
        // 鍒楀鍗曚綅: 1/256涓瓧绗﹀搴?        int[] columnWidths = {2500, 6000, 4000, 4000, 4000, 3500, 3000, 4000, 4000, 3000, 4000};
        for (int i = 0; i < headers.length && i < columnWidths.length; i++) {
            sheet.setColumnWidth(i, columnWidths[i]);
        }
        System.out.println("瀵煎嚭鏁版嵁璋冭瘯 - 璐锋宸ヤ綔琛ㄥ垱寤哄畬鎴愶紝鏁版嵁琛屾暟: " + (rowNum - 1));
    }
    
    /**
     * 鍒涘缓杩樻璁板綍宸ヤ綔琛?     * XLSX 妯℃澘缁撴瀯:
     * 鍒桝: ID | 鍒桞: 璐锋ID | 鍒桟: 杩樻閲戦 | 鍒桪: 杩樻鏃ユ湡 | 鍒桬: 澶囨敞
     */
    private void createPaymentSheet(Sheet sheet, List<Payment> payments) {
        // 鍒涘缓鏍囬琛屾牱寮?        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        // 鍒涘缓鏍囬琛?        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "璐锋ID", "杩樻閲戦", "杩樻鏃ユ湡", "澶囨敞"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // 濉厖鏁版嵁
        int rowNum = 1;
        for (Payment payment : payments) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(payment.getId());
            row.createCell(1).setCellValue(payment.getLoanId());
            row.createCell(2).setCellValue(payment.getAmount());
            row.createCell(3).setCellValue(payment.getDate() != null ? payment.getDate() : "");
            row.createCell(4).setCellValue(payment.getNote() != null ? payment.getNote() : "");
        }
        
        // 璁剧疆鍥哄畾鍒楀锛圓ndroid涓嶆敮鎸乤utoSizeColumn锛屽洜涓虹己灏慉WT绫伙級
        int[] columnWidths = {2500, 4000, 4000, 4000, 6000};
        for (int i = 0; i < headers.length && i < columnWidths.length; i++) {
            sheet.setColumnWidth(i, columnWidths[i]);
        }
        System.out.println("瀵煎嚭鏁版嵁璋冭瘯 - 杩樻璁板綍宸ヤ綔琛ㄥ垱寤哄畬鎴愶紝鏁版嵁琛屾暟: " + (rowNum - 1));
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
        // 鍦ㄥ悗鍙扮嚎绋嬫墽琛屽鍏ユ搷浣?        executorService.execute(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream != null) {
                    // 璇诲彇 XLSX 鏂囦欢
                    Workbook workbook = new XSSFWorkbook(inputStream);
                    
                    // 瑙ｆ瀽璐锋淇℃伅
                    List<Loan> loans = parseLoanSheet(workbook.getSheet(SHEET_LOANS));
                    
                    // 瑙ｆ瀽杩樻璁板綍
                    List<Payment> payments = parsePaymentSheet(workbook.getSheet(SHEET_PAYMENTS));
                    
                    workbook.close();
                    inputStream.close();
                    
                    // 娓呯┖鐜版湁鏁版嵁
                    repository.deleteAllPayments();
                    repository.deleteAllLoans();
                    
                    // 瀵煎叆鏂版暟鎹?                    for (Loan loan : loans) {
                        loan.setId(0); // 閲嶇疆ID
                        repository.insertLoan(loan);
                    }
                    
                    for (Payment payment : payments) {
                        payment.setId(0); // 閲嶇疆ID
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
     * 瑙ｆ瀽璐锋淇℃伅宸ヤ綔琛?     * XLSX 妯℃澘缁撴瀯:
     * 鍒桝: ID | 鍒桞: 璐锋鍚嶇О | 鍒桟: 璐锋绫诲瀷 | 鍒桪: 杩樻鏂瑰紡 | 鍒桬: 鏈噾 | 鍒桭: 骞村埄鐜?%) | 鍒桮: 鏈熼檺(鏈? | 鍒桯: 寮€濮嬫棩鏈?| 鍒桰: 淇＄敤鍗￠搴?| 鍒桱: 杩樻鏃?| 鍒桲: 鍘熷鏈堜緵
     */
    private List<Loan> parseLoanSheet(Sheet sheet) {
        List<Loan> loans = new ArrayList<>();
        if (sheet == null) return loans;
        
        Iterator<Row> rowIterator = sheet.iterator();
        // 璺宠繃鏍囬琛?        if (rowIterator.hasNext()) {
            rowIterator.next();
        }
        
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Loan loan = new Loan();
            
            // 璇诲彇鍚勫垪鏁版嵁
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
            
            // 鍙坊鍔犳湁鏁堢殑璐锋璁板綍锛堝繀椤绘湁鍚嶇О锛?            if (loan.getName() != null && !loan.getName().trim().isEmpty()) {
                loans.add(loan);
            }
        }
        
        return loans;
    }
    
    /**
     * 瑙ｆ瀽杩樻璁板綍宸ヤ綔琛?     * XLSX 妯℃澘缁撴瀯:
     * 鍒桝: ID | 鍒桞: 璐锋ID | 鍒桟: 杩樻閲戦 | 鍒桪: 杩樻鏃ユ湡 | 鍒桬: 澶囨敞
     */
    private List<Payment> parsePaymentSheet(Sheet sheet) {
        List<Payment> payments = new ArrayList<>();
        if (sheet == null) return payments;
        
        Iterator<Row> rowIterator = sheet.iterator();
        // 璺宠繃鏍囬琛?        if (rowIterator.hasNext()) {
            rowIterator.next();
        }
        
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Payment payment = new Payment();
            
            // 璇诲彇鍚勫垪鏁版嵁
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
            
            // 鍙坊鍔犳湁鏁堢殑杩樻璁板綍锛堝繀椤绘湁璐锋ID鍜岄噾棰濓級
            if (payment.getLoanId() > 0 && payment.getAmount() > 0) {
                payments.add(payment);
            }
        }
        
        return payments;
    }
    
    /**
     * 鑾峰彇鍗曞厓鏍肩殑瀛楃涓插€?     */
    private String getCellStringValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                // 妫€鏌ユ槸鍚︿负鏃ユ湡鏍煎紡
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
     * 妫€鏌ュ崟鍏冩牸鏄惁涓烘棩鏈熸牸寮?     * 鏇夸唬 Apache POI 鐨?DateUtil.isCellDateFormatted
     */
    private boolean isCellDateFormatted(Cell cell) {
        try {
            short dataFormat = cell.getCellStyle().getDataFormat();
            // 甯歌鐨勬棩鏈熸牸寮忕储寮?            return dataFormat == 0x0e || dataFormat == 0x0f || dataFormat == 0x10 ||
                   dataFormat == 0x11 || dataFormat == 0x12 || dataFormat == 0x13 ||
                   dataFormat == 0x14 || dataFormat == 0x15 || dataFormat == 0x16 ||
                   dataFormat == 0x2d || dataFormat == 0x2e || dataFormat == 0x2f;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 鑾峰彇鍗曞厓鏍肩殑鏁板€?     */
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
