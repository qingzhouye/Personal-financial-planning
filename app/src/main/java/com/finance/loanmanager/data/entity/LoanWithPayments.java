package com.finance.loanmanager.data.entity;

import androidx.room.Embedded;
import androidx.room.Relation;
import java.util.List;

/**
 * 贷款及其还款记录的关系类
 * 用于Room的一对多查询
 */
public class LoanWithPayments {
    
    @Embedded
    private Loan loan;
    
    @Relation(
        parentColumn = "id",
        entityColumn = "loanId"
    )
    private List<Payment> payments;
    
    public Loan getLoan() {
        return loan;
    }
    
    public void setLoan(Loan loan) {
        this.loan = loan;
    }
    
    public List<Payment> getPayments() {
        return payments;
    }
    
    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }
    
    /**
     * 计算该贷款的总还款金额
     */
    public double getTotalPaidAmount() {
        if (payments == null || payments.isEmpty()) {
            return 0;
        }
        double total = 0;
        for (Payment payment : payments) {
            total += payment.getAmount();
        }
        return total;
    }
}
