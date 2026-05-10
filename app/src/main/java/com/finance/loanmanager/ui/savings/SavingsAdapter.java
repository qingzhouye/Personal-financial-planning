package com.finance.loanmanager.ui.savings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.finance.loanmanager.R;
import com.finance.loanmanager.data.entity.Savings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SavingsAdapter extends RecyclerView.Adapter<SavingsAdapter.ViewHolder> {

    private List<Savings> savingsList;
    private final OnItemClickListener clickListener;
    private final OnItemLongClickListener longClickListener;

    public interface OnItemClickListener {
        void onClick(Savings savings);
    }

    public interface OnItemLongClickListener {
        void onLongClick(Savings savings);
    }

    public SavingsAdapter(List<Savings> savingsList, OnItemClickListener clickListener, OnItemLongClickListener longClickListener) {
        this.savingsList = savingsList != null ? savingsList : new ArrayList<>();
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    public void updateData(List<Savings> newList) {
        this.savingsList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_savings, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Savings savings = savingsList.get(position);
        holder.bind(savings);
    }

    @Override
    public int getItemCount() {
        return savingsList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvIcon;
        private final TextView tvNote;
        private final TextView tvDate;
        private final TextView tvAmount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvIcon);
            tvNote = itemView.findViewById(R.id.tvNote);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }

        void bind(Savings savings) {
            // 图标
            if (savings.isDeposit()) {
                tvIcon.setText("\uD83D\uDCB0");
            } else {
                tvIcon.setText("\uD83D\uDCB8");
            }

            // 备注
            String note = savings.getNote();
            tvNote.setText(note != null && !note.isEmpty() ? note : (savings.isDeposit() ? "存入" : "支出"));

            // 日期
            tvDate.setText(savings.getDate());

            // 金额
            double amount = savings.getAmount();
            if (amount >= 0) {
                tvAmount.setText(String.format(Locale.getDefault(), "+¥%.2f", amount));
                tvAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.success));
            } else {
                tvAmount.setText(String.format(Locale.getDefault(), "-¥%.2f", Math.abs(amount)));
                tvAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.danger));
            }

            // 点击事件
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onClick(savings);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onLongClick(savings);
                }
                return true;
            });
        }
    }
}
