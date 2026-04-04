package com.finance.loanmanager.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/**
 * 还款记录实体类
 * 对应数据库中的还款记录表
 */
@Entity(
    tableName = "payments",
    foreignKeys = @ForeignKey(
        entity = Loan.class,
        parentColumns = "id",
        childColumns = "loanId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("loanId")}
)
public class Payment {
    
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private int loanId;
    
    // 还款金额
    private double amount;
    
    // 还款日期(格式: yyyy-MM-dd)
    @NonNull
    private String date;
    
    // 备注
    private String note;
    
    public Payment() {
    }
    
    @Ignore
    public Payment(int loanId, double amount, @NonNull String date) {
        this.loanId = loanId;
        this.amount = amount;
        this.date = date;
    }
    
    @Ignore
    public Payment(int loanId, double amount, @NonNull String date, String note) {
        this.loanId = loanId;
        this.amount = amount;
        this.date = date;
        this.note = note;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getLoanId() {
        return loanId;
    }
    
    public void setLoanId(int loanId) {
        this.loanId = loanId;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    @NonNull
    public String getDate() {
        return date;
    }
    
    public void setDate(@NonNull String date) {
        this.date = date;
    }
    
    public String getNote() {
        return note;
    }
    
    public void setNote(String note) {
        this.note = note;
    }
}
