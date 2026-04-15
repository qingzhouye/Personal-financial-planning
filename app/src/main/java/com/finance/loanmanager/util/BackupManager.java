/**
 * ============================================================================
 * 文件名: BackupManager.java
 * 模块:   工具类层 (util)
 * 功能:   数据备份与恢复管理器，负责贷款数据的导出和导入
 * 
 * 主要职责:
 *   1. 将贷款数据导出为 Excel 文件 (XLSX格式)
 *   2. 从 Excel 文件恢复贷款数据
 *   3. 自动备份功能（应用启动时静默执行）
 *   4. 手动备份功能（带时间戳的备份文件）
 * 
 * 备份文件格式:
 *   - 使用 Apache POI 库生成 XLSX 文件
 *   - 包含两个工作表：贷款信息、还款记录
 *   - 文件存储在公共 Downloads 目录下
 * 
 * 使用场景:
 *   - 应用启动时自动备份当前数据
 *   - 用户手动创建备份文件
 *   - 用户恢复之前备份的数据
 *   - 数据迁移和跨设备同步
 * 
 * 依赖库:
 *   - Apache POI (XSSFWorkbook) 用于 Excel 文件操作
 * 
 * @see LoanRepository 数据仓库
 * @see BackupInfo 备份文件信息
 * ============================================================================
 */
package com.finance.loanmanager.util;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.finance.loanmanager.data.AppDatabase;
import com.finance.loanmanager.data.entity.Loan;
import com.finance.loanmanager.data.entity.Payment;
import com.finance.loanmanager.repository.LoanRepository;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 数据备份与恢复管理器
 * 
 * 该类负责管理贷款数据的备份和恢复功能。使用 Excel (XLSX) 格式存储数据，
 * 便于用户查看和在其他设备上恢复数据。
 * 
 * 线程安全:
 *   - 所有备份和恢复操作都在后台线程执行
 *   - 使用单线程执行器确保操作顺序
 *   - 通过回调接口通知操作结果
 * 
 * 文件存储位置:
 *   - 自动备份: /Downloads/LoanManager/backup_auto.xlsx
 *   - 手动备份: /Downloads/LoanManager/loan_backup_yyyyMMdd_HHmmss.xlsx
 * 
 * 使用示例:
 *   BackupManager manager = new BackupManager(context);
 *   manager.performAutoBackup(new BackupManager.BackupCallback() {
 *       public void onSuccess(String message) { }
 *       public void onError(String error) { }
 *   });
 */
public class BackupManager {
    
    // ==================== 常量定义 ====================
    
    /** 备份目录名称 */
    private static final String BACKUP_DIR = "LoanManager";
    
    /** 自动备份文件名 */
    private static final String AUTO_BACKUP_FILE = "backup_auto.xlsx";
    
    /** 手动备份文件名前缀 */
    private static final String BACKUP_PREFIX = "loan_backup_";
    
    /** 贷款信息工作表名称 */
    private static final String SHEET_LOANS = "贷款信息";
    
    /** 还款记录工作表名称 */
    private static final String SHEET_PAYMENTS = "还款记录";
    
    /** 最小有效备份文件大小（字节） */
    private static final long MIN_VALID_BACKUP_SIZE = 100;
    
    // ==================== 成员变量 ====================
    
    /** 应用上下文 */
    private final Context context;
    
    /** 数据仓库引用 */
    private final LoanRepository repository;
    
    /** 后台线程执行器 */
    private final ExecutorService executorService;
    
    // ==================== 构造方法 ====================
    
    /**
     * 构造备份管理器
     * 
     * @param context 上下文对象
     */
    public BackupManager(Context context) {
        this.context = context.getApplicationContext();
        this.repository = new LoanRepository((Application) this.context);
        this.executorService = Executors.newSingleThreadExecutor();
    }
    
    /**
     * 获取自动备份文件路径
     */
    public File getAutoBackupFile() {
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File backupDir = new File(downloadDir, BACKUP_DIR);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        return new File(backupDir, AUTO_BACKUP_FILE);
    }
    
    /**
     * 检查是否存在有效的自动备份文件
     * 验证文件存在且大小合理，并检查文件头是否为有效的XLSX格式
     */
    public boolean hasAutoBackup() {
        File backupFile = getAutoBackupFile();
        if (!backupFile.exists() || backupFile.length() < MIN_VALID_BACKUP_SIZE) {
            return false;
        }
        // 额外验证：尝试读取文件头确认是有效的XLSX
        return isValidXlsxFile(backupFile);
    }
    
    /**
     * 验证文件是否为有效的XLSX文件
     */
    private boolean isValidXlsxFile(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            // XLSX文件是ZIP格式，前4个字节应该是 ZIP magic number: 50 4B 03 04
            byte[] header = new byte[4];
            int read = fis.read(header);
            if (read < 4) {
                return false;
            }
            // ZIP文件魔数: 0x50 0x4B 0x03 0x04
            return header[0] == 0x50 && header[1] == 0x4B && 
                   header[2] == 0x03 && header[3] == 0x04;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取备份文件信息
     */
    public BackupInfo getBackupInfo() {
        File backupFile = getAutoBackupFile();
        if (!backupFile.exists()) {
            return null;
        }
        return new BackupInfo(
            backupFile.getName(),
            backupFile.length(),
            new Date(backupFile.lastModified())
        );
    }
    
    /**
     * 执行自动备份（静默模式，无UI提示）
     */
    public void performAutoBackup(BackupCallback callback) {
        executorService.execute(() -> {
            try {
                List<Loan> loans = repository.getAllLoansSync();
                List<Payment> payments = repository.getAllPaymentsSync();
                
                if (loans == null) loans = new ArrayList<>();
                if (payments == null) payments = new ArrayList<>();
                
                // 创建备份文件
                File backupFile = getAutoBackupFile();
                
                try (Workbook workbook = new XSSFWorkbook();
                     FileOutputStream fos = new FileOutputStream(backupFile)) {
                    
                    // 创建贷款信息工作表
                    Sheet loanSheet = workbook.createSheet(SHEET_LOANS);
                    createLoanSheet(loanSheet, loans);
                    
                    // 创建还款记录工作表
                    Sheet paymentSheet = workbook.createSheet(SHEET_PAYMENTS);
                    createPaymentSheet(paymentSheet, payments);
                    
                    workbook.write(fos);
                }
                
                if (callback != null) {
                    callback.onSuccess(backupFile.getAbsolutePath());
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }
    
    /**
     * 从自动备份恢复数据
     */
    public void restoreFromAutoBackup(BackupCallback callback) {
        executorService.execute(() -> {
            try {
                File backupFile = getAutoBackupFile();
                if (!backupFile.exists()) {
                    if (callback != null) {
                        callback.onError("备份文件不存在");
                    }
                    return;
                }
                
                try (InputStream inputStream = new FileInputStream(backupFile);
                     Workbook workbook = new XSSFWorkbook(inputStream)) {
                    
                    // 解析贷款信息
                    List<Loan> loans = parseLoanSheet(workbook.getSheet(SHEET_LOANS));
                    
                    // 解析还款记录
                    List<Payment> payments = parsePaymentSheet(workbook.getSheet(SHEET_PAYMENTS));
                    
                    // 清空现有数据
                    repository.deleteAllPayments();
                    repository.deleteAllLoans();
                    
                    // 导入新数据
                    for (Loan loan : loans) {
                        loan.setId(0);
                        repository.insertLoan(loan);
                    }
                    
                    for (Payment payment : payments) {
                        payment.setId(0);
                        repository.insertPayment(payment);
                    }
                    
                    if (callback != null) {
                        callback.onSuccess("恢复成功");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }
    
    /**
     * 创建带时间戳的手动备份
     */
    public void createTimestampedBackup(BackupCallback callback) {
        executorService.execute(() -> {
            try {
                List<Loan> loans = repository.getAllLoansSync();
                List<Payment> payments = repository.getAllPaymentsSync();
                
                if (loans == null) loans = new ArrayList<>();
                if (payments == null) payments = new ArrayList<>();
                
                // 创建带时间戳的文件名
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
                String fileName = BACKUP_PREFIX + sdf.format(new Date()) + ".xlsx";
                
                File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File backupDir = new File(downloadDir, BACKUP_DIR);
                if (!backupDir.exists()) {
                    backupDir.mkdirs();
                }
                File backupFile = new File(backupDir, fileName);
                
                try (Workbook workbook = new XSSFWorkbook();
                     FileOutputStream fos = new FileOutputStream(backupFile)) {
                    
                    Sheet loanSheet = workbook.createSheet(SHEET_LOANS);
                    createLoanSheet(loanSheet, loans);
                    
                    Sheet paymentSheet = workbook.createSheet(SHEET_PAYMENTS);
                    createPaymentSheet(paymentSheet, payments);
                    
                    workbook.write(fos);
                }
                
                if (callback != null) {
                    callback.onSuccess(backupFile.getAbsolutePath());
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }
    
    /**
     * 删除自动备份文件
     */
    public boolean deleteAutoBackup() {
        File backupFile = getAutoBackupFile();
        if (backupFile.exists()) {
            return backupFile.delete();
        }
        return false;
    }
    
    /**
     * 获取备份文件分享URI
     */
    public Uri getBackupFileUri() {
        File backupFile = getAutoBackupFile();
        if (backupFile.exists()) {
            return FileProvider.getUriForFile(context, 
                context.getPackageName() + ".fileprovider", 
                backupFile);
        }
        return null;
    }
    
    // ========== 私有辅助方法 ==========
    
    private void createLoanSheet(Sheet sheet, List<Loan> loans) {
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "贷款名称", "贷款类型", "还款方式", "本金", "年利率(%)", "期限(月)", "开始日期", "信用卡额度", "还款日", "原始月供"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
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
        
        int[] columnWidths = {2500, 6000, 4000, 4000, 4000, 3500, 3000, 4000, 4000, 3000, 4000};
        for (int i = 0; i < headers.length && i < columnWidths.length; i++) {
            sheet.setColumnWidth(i, columnWidths[i]);
        }
    }
    
    private void createPaymentSheet(Sheet sheet, List<Payment> payments) {
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "贷款ID", "还款金额", "还款日期", "备注"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        int rowNum = 1;
        for (Payment payment : payments) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(payment.getId());
            row.createCell(1).setCellValue(payment.getLoanId());
            row.createCell(2).setCellValue(payment.getAmount());
            row.createCell(3).setCellValue(payment.getDate() != null ? payment.getDate() : "");
            row.createCell(4).setCellValue(payment.getNote() != null ? payment.getNote() : "");
        }
        
        int[] columnWidths = {2500, 4000, 4000, 4000, 6000};
        for (int i = 0; i < headers.length && i < columnWidths.length; i++) {
            sheet.setColumnWidth(i, columnWidths[i]);
        }
    }
    
    private List<Loan> parseLoanSheet(Sheet sheet) {
        List<Loan> loans = new ArrayList<>();
        if (sheet == null) return loans;
        
        Iterator<Row> rowIterator = sheet.iterator();
        if (rowIterator.hasNext()) {
            rowIterator.next();
        }
        
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Loan loan = new Loan();
            
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
            
            if (loan.getName() != null && !loan.getName().trim().isEmpty()) {
                loans.add(loan);
            }
        }
        
        return loans;
    }
    
    private List<Payment> parsePaymentSheet(Sheet sheet) {
        List<Payment> payments = new ArrayList<>();
        if (sheet == null) return payments;
        
        Iterator<Row> rowIterator = sheet.iterator();
        if (rowIterator.hasNext()) {
            rowIterator.next();
        }
        
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Payment payment = new Payment();
            
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
            
            if (payment.getLoanId() > 0 && payment.getAmount() > 0) {
                payments.add(payment);
            }
        }
        
        return payments;
    }
    
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
    
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    /**
     * 备份信息类
     */
    public static class BackupInfo {
        public final String fileName;
        public final long fileSize;
        public final Date lastModified;
        
        public BackupInfo(String fileName, long fileSize, Date lastModified) {
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.lastModified = lastModified;
        }
        
        public String getFormattedSize() {
            if (fileSize < 1024) {
                return fileSize + " B";
            } else if (fileSize < 1024 * 1024) {
                return String.format(Locale.getDefault(), "%.2f KB", fileSize / 1024.0);
            } else {
                return String.format(Locale.getDefault(), "%.2f MB", fileSize / (1024.0 * 1024.0));
            }
        }
        
        public String getFormattedDate() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            return sdf.format(lastModified);
        }
    }
    
    /**
     * 备份回调接口
     */
    public interface BackupCallback {
        void onSuccess(String message);
        void onError(String error);
    }
}
