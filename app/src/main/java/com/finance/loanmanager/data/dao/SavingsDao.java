package com.finance.loanmanager.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.finance.loanmanager.data.entity.Savings;

import java.util.List;

@Dao
public interface SavingsDao {

    @Query("SELECT * FROM savings ORDER BY date DESC, createdAt DESC")
    LiveData<List<Savings>> getAllSavingsLive();

    @Query("SELECT * FROM savings ORDER BY date DESC, createdAt DESC")
    List<Savings> getAllSavings();

    @Query("SELECT * FROM savings ORDER BY date DESC, createdAt DESC LIMIT :limit")
    List<Savings> getRecentSavings(int limit);

    @Query("SELECT * FROM savings WHERE id = :id")
    Savings getSavingsById(int id);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM savings")
    double getTotalBalance();

    @Query("SELECT COALESCE(SUM(amount), 0) FROM savings WHERE amount > 0")
    double getTotalDeposit();

    @Query("SELECT COALESCE(SUM(amount), 0) FROM savings WHERE amount < 0")
    double getTotalWithdraw();

    @Query("SELECT * FROM savings ORDER BY date ASC, createdAt ASC")
    List<Savings> getAllSavingsAsc();

    @Query("SELECT COUNT(*) FROM savings")
    int getSavingsCount();

    @Insert
    long insertSavings(Savings savings);

    @Insert
    void insertAllSavings(List<Savings> savingsList);

    @Update
    void updateSavings(Savings savings);

    @Delete
    void deleteSavings(Savings savings);

    @Query("DELETE FROM savings WHERE id = :id")
    void deleteSavingsById(int id);

    @Query("DELETE FROM savings")
    void deleteAllSavings();
}
