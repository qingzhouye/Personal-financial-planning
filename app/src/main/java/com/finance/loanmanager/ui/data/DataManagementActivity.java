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

    private ActivityResultLauncher<Intent> exportLauncher;
    private ActivityResultLauncher<Intent> importLauncher;

    private static final String SHEET_LOANS = "\u8d37\u6b3e\u4fe1\u606f";
    private static final String SHEET_PAYMENTS = "\u8fd8\u6b3e\u8bb0\u5f55";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        repository = new LoanRepository(getApplication());
        executorService = Executors.newSingleThreadExecutor();

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
        executorService.execute(() -> {
            OutputStream outputStream = null;
            Workbook workbook = null;
            try {
                List<Loan> loans = repository.getAllLoansSync();
                List<Payment> payments = repository.getAllPaymentsSync();

                if (loans == null) loans = new ArrayList<>();
                if (payments == null) payments = new ArrayList<>();

                workbook = new XSSFWorkbook();

                Sheet loanSheet = workbook.createSheet(SHEET_LOANS);
                createLoanSheet(loanSheet, loans);

                Sheet paymentSheet = workbook.createSheet(SHEET_PAYMENTS);
                createPaymentSheet(paymentSheet, payments);

                outputStream = getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    workbook.write(outputStream);
                    outputStream.flush();
                    runOnUiThread(() -> Toast.makeText(this, R.string.export_success, Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(this, getString(R.string.export_failed), Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                final String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.export_failed) + ": " + errorMsg, Toast.LENGTH_LONG).show());
            } finally {
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                    if (workbook != null) {
                        workbook.close();
                    }
                } catch (Exception e) {
                    // ignore close errors
                }
                runOnUiThread(this::finish);
            }
        });
    }

    /**
     * Create loan info sheet.
     * Columns: ID | Name | LoanType | RepaymentMethod | Principal | AnnualRate(%) | Months | StartDate | CreditLimit | DueDate | OriginalMonthlyPayment
     */
    private void createLoanSheet(Sheet sheet, List<Loan> loans) {
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "ID",
            "\u8d37\u6b3e\u540d\u79f0",
            "\u8d37\u6b3e\u7c7b\u578b",
            "\u8fd8\u6b3e\u65b9\u5f0f",
            "\u672c\u91d1",
            "\u5e74\u5229\u7387(%)",
            "\u671f\u9650(\u6708)",
            "\u5f00\u59cb\u65e5\u671f",
            "\u4fe1\u7528\u989d\u5ea6",
            "\u8fd8\u6b3e\u65e5",
            "\u539f\u59cb\u6708\u4f9b"
        };
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

    /**
     * Create payment records sheet.
     * Columns: ID | LoanID | Amount | Date | Note
     */
    private void createPaymentSheet(Sheet sheet, List<Payment> payments) {
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "ID",
            "\u8d37\u6b3eID",
            "\u8fd8\u6b3e\u91d1\u989d",
            "\u8fd8\u6b3e\u65e5\u671f",
            "\u5907\u6ce8"
        };
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

    private void confirmImport(Uri uri) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.import_data)
                .setMessage(R.string.import_confirm)
                .setPositiveButton(R.string.confirm, (dialog, which) -> importData(uri))
                .setNegativeButton(R.string.cancel, (dialog, which) -> finish())
                .show();
    }

    private void importData(Uri uri) {
        executorService.execute(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream != null) {
                    Workbook workbook = new XSSFWorkbook(inputStream);

                    List<Loan> loans = parseLoanSheet(workbook.getSheet(SHEET_LOANS));
                    List<Payment> payments = parsePaymentSheet(workbook.getSheet(SHEET_PAYMENTS));

                    workbook.close();
                    inputStream.close();

                    repository.deleteAllPayments();
                    repository.deleteAllLoans();

                    for (Loan loan : loans) {
                        loan.setId(0);
                        repository.insertLoan(loan);
                    }

                    for (Payment payment : payments) {
                        payment.setId(0);
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
     * Parse loan info sheet.
     * Columns: ID | Name | LoanType | RepaymentMethod | Principal | AnnualRate(%) | Months | StartDate | CreditLimit | DueDate | OriginalMonthlyPayment
     */
    private List<Loan> parseLoanSheet(Sheet sheet) {
        List<Loan> loans = new ArrayList<>();
        if (sheet == null) return loans;

        Iterator<Row> rowIterator = sheet.iterator();
        if (rowIterator.hasNext()) {
            rowIterator.next(); // skip header
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

    /**
     * Parse payment records sheet.
     * Columns: ID | LoanID | Amount | Date | Note
     */
    private List<Payment> parsePaymentSheet(Sheet sheet) {
        List<Payment> payments = new ArrayList<>();
        if (sheet == null) return payments;

        Iterator<Row> rowIterator = sheet.iterator();
        if (rowIterator.hasNext()) {
            rowIterator.next(); // skip header
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

    /**
     * Get string value from a cell.
     */
    private String getCellStringValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
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
     * Check if a cell is date formatted.
     */
    private boolean isCellDateFormatted(Cell cell) {
        try {
            short dataFormat = cell.getCellStyle().getDataFormat();
            return dataFormat == 0x0e || dataFormat == 0x0f || dataFormat == 0x10 ||
                   dataFormat == 0x11 || dataFormat == 0x12 || dataFormat == 0x13 ||
                   dataFormat == 0x14 || dataFormat == 0x15 || dataFormat == 0x16 ||
                   dataFormat == 0x2d || dataFormat == 0x2e || dataFormat == 0x2f;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get numeric value from a cell.
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
