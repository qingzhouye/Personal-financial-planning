package com.finance.loanmanager.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "savings")
public class Savings {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private double amount; // 正数=存入，负数=支出

    @NonNull
    private String date; // yyyy-MM-dd

    private String note; // 备注

    private long createdAt; // 创建时间戳(ms)

    public Savings() {
        this.date = "";
        this.createdAt = System.currentTimeMillis();
    }

    @Ignore
    public Savings(double amount, @NonNull String date, String note) {
        this.amount = amount;
        this.date = date;
        this.note = note;
        this.createdAt = System.currentTimeMillis();
    }

    @Ignore
    public Savings(double amount, @NonNull String date, String note, long createdAt) {
        this.amount = amount;
        this.date = date;
        this.note = note;
        this.createdAt = createdAt;
    }

    // Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isDeposit() {
        return amount >= 0;
    }
}
