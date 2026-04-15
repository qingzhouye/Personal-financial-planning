/**
 * ============================================================================
 * 文件名: ScheduleAdapter.java
 * 模块:   UI层 - 还款计划适配器
 * 功能:   还款计划列表的 RecyclerView 适配器
 * 
 * 主要职责:
 *   1. 将还款计划数据绑定到列表项视图
 *   2. 显示每期的还款明细
 * 
 * 显示内容:
 *   - 期数: 第几期
 *   - 还款日期: yyyy-MM-dd 格式
 *   - 还款金额: 格式化货币显示
 *   - 本金: 当期偿还的本金
 *   - 利息: 当期偿还的利息
 *   - 剩余本金: 还款后的剩余金额
 * 
 * @see PaymentScheduleActivity 还款计划页面
 * ============================================================================
 */
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

/**
 * 还款计划适配器
 * 
 * 用于 RecyclerView 的数据适配器，将还款计划数据显示为列表项。
 * 每个列表项显示一期的还款明细。
 */
public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    /** 还款计划数据列表 */
    private List<LoanCalculator.PaymentScheduleItem> schedule;

    /**
     * 构造适配器
     * @param schedule 还款计划数据
     */
    public ScheduleAdapter(List<LoanCalculator.PaymentScheduleItem> schedule) {
        this.schedule = schedule;
    }

    /**
     * 更新数据并刷新列表
     * @param schedule 新的还款计划数据
     */
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
