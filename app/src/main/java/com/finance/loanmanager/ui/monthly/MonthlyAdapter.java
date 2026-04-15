/**
 * ============================================================================
 * 文件名: MonthlyAdapter.java
 * 模块:   UI层 - 月度还款适配器
 * 功能:   月度还款列表的 RecyclerView 适配器
 * 
 * 主要职责:
 *   1. 将月度还款数据绑定到列表项视图
 *   2. 显示月份、还款总额、各贷款详情
 * 
 * 显示内容:
 *   - 月份: yyyy-MM 格式
 *   - 还款总额: 格式化货币显示
 *   - 贷款详情: 每笔贷款的还款金额列表
 * 
 * @see MonthlyTotalActivity 月度还款汇总页面
 * ============================================================================
 */
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

/**
 * 月度还款适配器
 * 
 * 用于 RecyclerView 的数据适配器，将月度还款数据显示为列表项。
 * 每个列表项显示一个月的还款汇总信息。
 */
public class MonthlyAdapter extends RecyclerView.Adapter<MonthlyAdapter.ViewHolder> {

    /** 月度数据列表 */
    private List<MonthlyTotalActivity.MonthlyItem> items;

    /**
     * 构造适配器
     * @param items 月度数据列表
     */
    public MonthlyAdapter(List<MonthlyTotalActivity.MonthlyItem> items) {
        this.items = items;
    }

    /**
     * 更新数据并刷新列表
     * @param items 新的月度数据列表
     */
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
