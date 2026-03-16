package com.finance.loanmanager.ui.monthly;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.finance.loanmanager.R;
import com.finance.loanmanager.util.NumberFormatUtil;

import java.util.List;

public class MonthlyAdapter extends RecyclerView.Adapter<MonthlyAdapter.ViewHolder> {

    private List<MonthlyTotalActivity.MonthlyItem> items;

    public MonthlyAdapter(List<MonthlyTotalActivity.MonthlyItem> items) {
        this.items = items;
    }

    public void updateData(List<MonthlyTotalActivity.MonthlyItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_monthly, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MonthlyTotalActivity.MonthlyItem item = items.get(position);
        holder.tvMonth.setText(item.month);
        holder.tvTotal.setText(NumberFormatUtil.formatCurrency(item.total));
        
        StringBuilder loansText = new StringBuilder();
        for (String loan : item.loans) {
            if (loansText.length() > 0) {
                loansText.append("\n");
            }
            loansText.append(loan);
        }
        holder.tvLoans.setText(loansText.toString());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMonth;
        TextView tvTotal;
        TextView tvLoans;

        ViewHolder(View itemView) {
            super(itemView);
            tvMonth = itemView.findViewById(R.id.tvMonth);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            tvLoans = itemView.findViewById(R.id.tvLoans);
        }
    }
}
