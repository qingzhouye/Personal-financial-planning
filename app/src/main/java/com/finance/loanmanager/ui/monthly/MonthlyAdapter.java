/**
 * ============================================================================
 * 文件名: MonthlyAdapter.java
 * 模块:   UI层 - 月度还款适配器
 * 功能:   月度还款列表的 RecyclerView 适配器
 * 
 * 主要职责:
 *   1. 将月度还款数据绑定到列表项视图
 *   2. 显示月份、还款总额、各贷款详情
 *   3. 三列布局：日期、总金额、明细
 * 
 * 显示内容:
 *   - 日期: yyyy-MM 格式，固定宽度居中对齐
 *   - 总金额: 格式化货币显示，固定宽度居中对齐
 *   - 明细: 贷款名称和金额在一行显示，左对齐
 * 
 * @see MonthlyTotalActivity 月度还款汇总页面
 * ============================================================================
 */
package com.finance.loanmanager.ui.monthly;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
        
        // 设置日期和总金额
        holder.tvMonth.setText(item.month);
        holder.tvTotal.setText(NumberFormatUtil.formatCurrency(item.total));
        
        // 清空明细列
        holder.llDetails.removeAllViews();
        
        // 合并所有贷款明细（普通贷款/信用卡 + 助学贷款）
        List<MonthlyTotalActivity.LoanDetailItem> allLoans = item.normalLoans;
        if (item.studentLoans != null) {
            allLoans.addAll(item.studentLoans);
        }
        
        // 填充明细列
        if (allLoans != null && !allLoans.isEmpty()) {
            for (MonthlyTotalActivity.LoanDetailItem loan : allLoans) {
                addLoanDetailView(holder.llDetails, loan);
            }
        } else {
            // 明细为空时显示提示
            addEmptyView(holder.llDetails, "无");
        }
    }
    
    /**
     * 添加贷款详情视图到指定容器
     * @param container 目标容器
     * @param loan 贷款详情
     */
    private void addLoanDetailView(LinearLayout container, MonthlyTotalActivity.LoanDetailItem loan) {
        View view = LayoutInflater.from(container.getContext())
                .inflate(R.layout.item_loan_detail, container, false);
        
        TextView tvName = view.findViewById(R.id.tvLoanName);
        TextView tvAmount = view.findViewById(R.id.tvLoanAmount);
        
        tvName.setText(loan.loanName + ":");
        tvAmount.setText(NumberFormatUtil.formatCurrency(loan.amount));
        
        container.addView(view);
    }
    
    /**
     * 添加空状态提示视图
     * @param container 目标容器
     * @param text 提示文本
     */
    private void addEmptyView(LinearLayout container, String text) {
        TextView tv = new TextView(container.getContext());
        tv.setText(text);
        tv.setTextSize(12);
        tv.setTextColor(container.getContext().getResources().getColor(R.color.text_hint));
        tv.setPadding(0, 4, 0, 4);
        container.addView(tv);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMonth;
        TextView tvTotal;
        LinearLayout llDetails;

        ViewHolder(View itemView) {
            super(itemView);
            tvMonth = itemView.findViewById(R.id.tvMonth);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            llDetails = itemView.findViewById(R.id.llDetails);
        }
    }
}
