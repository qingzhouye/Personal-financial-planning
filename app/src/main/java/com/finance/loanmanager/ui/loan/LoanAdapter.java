/**
 * ============================================================================
 * 文件名: LoanAdapter.java
 * 模块:   UI层 - 贷款列表适配器
 * 功能:   贷款列表的 RecyclerView 适配器
 * 
 * 主要职责:
 *   1. 将贷款数据绑定到列表项视图
 *   2. 显示贷款名称、剩余本金、月供等信息
 *   3. 处理列表项点击事件
 *   4. 区分信用卡和普通贷款的显示图标
 * 
 * 显示内容:
 *   - 图标: 信用卡显示💳，普通贷款显示🏦
 *   - 名称: 贷款名称（已还清的显示✓）
 *   - 剩余本金: 格式化金额显示
 *   - 月供: 格式化金额显示
 * 
 * @see LoanListActivity 贷款列表页面
 * @see LoanRepository.LoanWithStatus 贷款带状态数据
 * ============================================================================
 */
package com.finance.loanmanager.ui.loan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.finance.loanmanager.R;
import com.finance.loanmanager.repository.LoanRepository;
import com.finance.loanmanager.util.NumberFormatUtil;

import java.util.List;

/**
 * 贷款列表适配器
 * 
 * 用于 RecyclerView 的数据适配器，将贷款数据显示为列表项。
 * 支持点击事件回调，通知外部页面用户的选择操作。
 * 
 * 使用示例:
 *   adapter = new LoanAdapter(loans, loan -> {
 *       // 处理点击事件
 *       openLoanDetail(loan.loan.getId());
 *   });
 */
public class LoanAdapter extends RecyclerView.Adapter<LoanAdapter.ViewHolder> {

    /** 贷款数据列表 */
    private List<LoanRepository.LoanWithStatus> loans;
    
    /** 点击事件监听器 */
    private OnLoanClickListener listener;

    /**
     * 贷款点击事件监听接口
     */
    public interface OnLoanClickListener {
        /**
         * 当贷款项被点击时调用
         * @param loan 被点击的贷款数据
         */
        void onLoanClick(LoanRepository.LoanWithStatus loan);
    }

    /**
     * 构造适配器
     * 
     * @param loans 贷款数据列表
     * @param listener 点击事件监听器
     */
    public LoanAdapter(List<LoanRepository.LoanWithStatus> loans, OnLoanClickListener listener) {
        this.loans = loans;
        this.listener = listener;
    }

    /**
     * 更新数据并刷新列表
     * 
     * @param loans 新的贷款数据列表
     */
    public void updateData(List<LoanRepository.LoanWithStatus> loans) {
        this.loans = loans;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_loan, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LoanRepository.LoanWithStatus item = loans.get(position);
        holder.tvName.setText(item.loan.getName());
        holder.tvRemaining.setText("剩余: " + NumberFormatUtil.formatCurrency(item.status.getRemainingPrincipal()));
        holder.tvMonthlyPayment.setText("月供: " + NumberFormatUtil.formatCurrency(item.status.getNewMonthlyPayment()));
        holder.tvIcon.setText(item.loan.isCreditCard() ? "💳" : "🏦");
        
        if (item.status.isPaidOff()) {
            holder.tvName.append(" ✓");
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLoanClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return loans.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon;
        TextView tvName;
        TextView tvRemaining;
        TextView tvMonthlyPayment;

        ViewHolder(View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvIcon);
            tvName = itemView.findViewById(R.id.tvName);
            tvRemaining = itemView.findViewById(R.id.tvRemaining);
            tvMonthlyPayment = itemView.findViewById(R.id.tvMonthlyPayment);
        }
    }
}
