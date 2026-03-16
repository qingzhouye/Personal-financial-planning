package com.finance.loanmanager.ui.schedule;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.finance.loanmanager.R;
import com.finance.loanmanager.service.LoanCalculator;
import com.finance.loanmanager.util.NumberFormatUtil;

import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    private List<LoanCalculator.PaymentScheduleItem> schedule;

    public ScheduleAdapter(List<LoanCalculator.PaymentScheduleItem> schedule) {
        this.schedule = schedule;
    }

    public void updateData(List<LoanCalculator.PaymentScheduleItem> schedule) {
        this.schedule = schedule;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LoanCalculator.PaymentScheduleItem item = schedule.get(position);
        holder.tvMonth.setText(String.valueOf(item.month));
        holder.tvDate.setText(item.date);
        holder.tvPayment.setText(NumberFormatUtil.formatCurrency(item.payment));
        holder.tvPrincipal.setText(NumberFormatUtil.formatCurrency(item.principal));
        holder.tvInterest.setText(NumberFormatUtil.formatCurrency(item.interest));
        holder.tvRemaining.setText(NumberFormatUtil.formatCurrency(item.remainingBalance));
    }

    @Override
    public int getItemCount() {
        return schedule.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMonth;
        TextView tvDate;
        TextView tvPayment;
        TextView tvPrincipal;
        TextView tvInterest;
        TextView tvRemaining;

        ViewHolder(View itemView) {
            super(itemView);
            tvMonth = itemView.findViewById(R.id.tvMonth);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvPayment = itemView.findViewById(R.id.tvPayment);
            tvPrincipal = itemView.findViewById(R.id.tvPrincipal);
            tvInterest = itemView.findViewById(R.id.tvInterest);
            tvRemaining = itemView.findViewById(R.id.tvRemaining);
        }
    }
}
