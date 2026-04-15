/**
 * ============================================================================
 * 文件名: BackupRestoreDialog.java
 * 模块:   UI层 - 数据管理
 * 功能:   备份恢复对话框，在应用启动时检测并提示恢复备份
 * 
 * 主要职责:
 *   1. 检测是否存在有效的备份文件
 *   2. 显示备份文件信息（文件名、日期、大小）
 *   3. 提供恢复、忽略、删除备份三个操作选项
 *   4. 检测当前数据库是否有数据，提示恢复风险
 * 
 * 使用场景:
 *   - 应用首次启动时检测到有备份文件
 *   - 用户可能需要在设备更换后恢复数据
 * 
 * @see BackupManager 备份管理器
 * ============================================================================
 */
package com.finance.loanmanager.ui.data;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.finance.loanmanager.R;
import com.finance.loanmanager.repository.LoanRepository;
import com.finance.loanmanager.util.BackupManager;

/**
 * 备份恢复对话框
 * 
 * DialogFragment 实现，在检测到存在备份文件时显示。
 * 提供三个操作选项：
 *   - 恢复：从备份文件恢复数据
 *   - 忽略：关闭对话框，保留备份文件
 *   - 删除：删除备份文件
 */
public class BackupRestoreDialog extends DialogFragment {
    
    /**
     * 恢复操作监听接口
     */
    public interface RestoreListener {
        /**
         * 恢复完成时调用
         * @param success 是否成功
         */
        void onRestoreComplete(boolean success);
        
        /**
         * 对话框关闭时调用
         */
        void onDismiss();
    }
    
    /** 恢复监听器 */
    private RestoreListener listener;
    
    /** 备份管理器 */
    private BackupManager backupManager;
    
    /** 备份文件信息 */
    private BackupManager.BackupInfo backupInfo;
    
    public void setRestoreListener(RestoreListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = requireContext();
        backupManager = new BackupManager(context);
        backupInfo = backupManager.getBackupInfo();
        
        // 检查当前数据库是否有数据
        LoanRepository repository = new LoanRepository(requireActivity().getApplication());
        int currentLoanCount = repository.getAllLoansSync().size();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        
        // 使用自定义布局
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_backup_restore, null);
        
        // 设置备份信息
        TextView tvBackupInfo = view.findViewById(R.id.tv_backup_info);
        TextView tvBackupDate = view.findViewById(R.id.tv_backup_date);
        TextView tvBackupSize = view.findViewById(R.id.tv_backup_size);
        TextView tvCurrentData = view.findViewById(R.id.tv_current_data);
        
        if (backupInfo != null) {
            tvBackupInfo.setText(getString(R.string.backup_found_info, backupInfo.fileName));
            tvBackupDate.setText(getString(R.string.backup_date, backupInfo.getFormattedDate()));
            tvBackupSize.setText(getString(R.string.backup_size, backupInfo.getFormattedSize()));
        }
        
        // 显示当前数据状态
        if (tvCurrentData != null) {
            if (currentLoanCount > 0) {
                tvCurrentData.setText(getString(R.string.current_data_exists, currentLoanCount));
                tvCurrentData.setTextColor(context.getResources().getColor(android.R.color.holo_orange_dark, null));
            } else {
                tvCurrentData.setText(R.string.current_data_empty);
                tvCurrentData.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark, null));
            }
        }
        
        // 设置按钮点击事件
        Button btnRestore = view.findViewById(R.id.btn_restore);
        Button btnIgnore = view.findViewById(R.id.btn_ignore);
        Button btnDelete = view.findViewById(R.id.btn_delete_backup);
        
        btnRestore.setOnClickListener(v -> performRestore());
        btnIgnore.setOnClickListener(v -> dismiss());
        btnDelete.setOnClickListener(v -> showDeleteConfirmDialog());
        
        builder.setView(view);
        builder.setCancelable(false); // 不允许点击外部关闭
        
        return builder.create();
    }
    
    private void performRestore() {
        Context context = getContext();
        if (context == null) return;
        
        // 显示进度提示
        Toast.makeText(context, R.string.restoring_backup, Toast.LENGTH_SHORT).show();
        
        backupManager.restoreFromAutoBackup(new BackupManager.BackupCallback() {
            @Override
            public void onSuccess(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(context, R.string.restore_success, Toast.LENGTH_LONG).show();
                        if (listener != null) {
                            listener.onRestoreComplete(true);
                        }
                        dismiss();
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(context, getString(R.string.restore_failed) + ": " + error, Toast.LENGTH_LONG).show();
                        if (listener != null) {
                            listener.onRestoreComplete(false);
                        }
                    });
                }
            }
        });
    }
    
    private void showDeleteConfirmDialog() {
        Context context = getContext();
        if (context == null) return;
        
        new AlertDialog.Builder(context)
            .setTitle(R.string.delete_backup_confirm_title)
            .setMessage(R.string.delete_backup_confirm_message)
            .setPositiveButton(R.string.confirm, (dialog, which) -> {
                boolean deleted = backupManager.deleteAutoBackup();
                if (deleted) {
                    Toast.makeText(context, R.string.backup_deleted, Toast.LENGTH_SHORT).show();
                }
                dismiss();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (backupManager != null) {
            backupManager.shutdown();
        }
        if (listener != null) {
            listener.onDismiss();
        }
    }
}
