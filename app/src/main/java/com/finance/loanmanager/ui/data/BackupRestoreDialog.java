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
import com.finance.loanmanager.util.BackupManager;

/**
 * 备份恢复对话框
 * 应用启动时检测到有备份文件时显示
 */
public class BackupRestoreDialog extends DialogFragment {
    
    public interface RestoreListener {
        void onRestoreComplete(boolean success);
        void onDismiss();
    }
    
    private RestoreListener listener;
    private BackupManager backupManager;
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
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        
        // 使用自定义布局
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_backup_restore, null);
        
        // 设置备份信息
        TextView tvBackupInfo = view.findViewById(R.id.tv_backup_info);
        TextView tvBackupDate = view.findViewById(R.id.tv_backup_date);
        TextView tvBackupSize = view.findViewById(R.id.tv_backup_size);
        
        if (backupInfo != null) {
            tvBackupInfo.setText(getString(R.string.backup_found_info, backupInfo.fileName));
            tvBackupDate.setText(getString(R.string.backup_date, backupInfo.getFormattedDate()));
            tvBackupSize.setText(getString(R.string.backup_size, backupInfo.getFormattedSize()));
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
