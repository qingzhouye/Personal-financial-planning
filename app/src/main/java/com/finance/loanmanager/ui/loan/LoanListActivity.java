package com.finance.loanmanager.ui.loan;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.finance.loanmanager.R;
import com.finance.loanmanager.repository.LoanRepository;

import java.util.ArrayList;
import java.util.List;

public class LoanListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private LoanRepository repository;
    private LoanAdapter adapter;
    private List<LoanRepository.LoanWithStatus> loans = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loan_list);
        
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.loan_list);
        
        repository = new LoanRepository(getApplication());
        
        recyclerView = findViewById(R.id.recyclerView);
        tvEmpty = findViewById(R.id.tvEmpty);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LoanAdapter(loans, loan -> {
            Intent intent = new Intent(this, LoanDetailActivity.class);
            intent.putExtra("loan_id", loan.loan.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void loadData() {
        loans = repository.getLoansWithStatus();
        adapter.updateData(loans);
        
        if (loans.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}
