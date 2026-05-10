package com.finance.loanmanager.ui.savings;

import android.graphics.Color;
import android.os.Bundle;

import com.finance.loanmanager.R;
import com.finance.loanmanager.data.entity.Savings;
import com.finance.loanmanager.databinding.ActivitySavingsChartBinding;
import com.finance.loanmanager.repository.SavingsRepository;
import com.finance.loanmanager.ui.BaseActivity;
import com.finance.loanmanager.util.ThemeManager;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SavingsChartActivity extends BaseActivity {

    private ActivitySavingsChartBinding binding;
    private SavingsRepository repository;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySavingsChartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new SavingsRepository(getApplication());
        executorService = Executors.newSingleThreadExecutor();

        binding.btnBack.setOnClickListener(v -> finish());

        loadChartData();
    }

    private void loadChartData() {
        executorService.execute(() -> {
            List<Savings> savingsList = repository.getAllSavingsForChart();
            double totalBalance = repository.getTotalBalance();
            double totalDeposit = repository.getTotalDeposit();
            double totalWithdraw = repository.getTotalWithdraw();

            runOnUiThread(() -> {
                // 更新摘要
                binding.tvTotalBalance.setText(String.format(Locale.getDefault(), "¥%.2f", totalBalance));
                binding.tvTotalDeposit.setText(String.format(Locale.getDefault(), "¥%.2f", totalDeposit));
                binding.tvTotalWithdraw.setText(String.format(Locale.getDefault(), "¥%.2f", Math.abs(totalWithdraw)));

                if (savingsList == null || savingsList.isEmpty()) {
                    binding.lineChart.setVisibility(android.view.View.GONE);
                    binding.layoutEmpty.setVisibility(android.view.View.VISIBLE);
                    return;
                }

                binding.lineChart.setVisibility(android.view.View.VISIBLE);
                binding.layoutEmpty.setVisibility(android.view.View.GONE);

                setupChart(savingsList);
            });
        });
    }

    private void setupChart(List<Savings> savingsList) {
        LineChart chart = binding.lineChart;

        // 计算累计余额
        List<Entry> entries = new ArrayList<>();
        List<String> dateLabels = new ArrayList<>();
        double cumulativeBalance = 0;

        for (int i = 0; i < savingsList.size(); i++) {
            Savings s = savingsList.get(i);
            cumulativeBalance += s.getAmount();
            entries.add(new Entry(i, (float) cumulativeBalance));

            // 日期标签取 MM/dd 部分
            String date = s.getDate();
            if (date.length() >= 10) {
                dateLabels.add(date.substring(5)); // MM-dd
            } else {
                dateLabels.add(date);
            }
        }

        // 获取主题色
        int themeIndex = ThemeManager.getSavedTheme(this);
        int themeColor = ThemeManager.getThemePrimaryColor(themeIndex);

        LineDataSet dataSet = new LineDataSet(entries, "储蓄余额");
        dataSet.setColor(themeColor);
        dataSet.setCircleColor(themeColor);
        dataSet.setCircleRadius(3f);
        dataSet.setLineWidth(2f);
        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(themeColor);
        dataSet.setFillAlpha(50);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        // X轴配置
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dateLabels));
        xAxis.setLabelRotationAngle(-45);
        xAxis.setTextSize(10f);

        // 如果数据点过多，控制显示标签数量
        if (dateLabels.size() > 10) {
            xAxis.setLabelCount(10, false);
        }

        // Y轴配置
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextSize(10f);
        chart.getAxisRight().setEnabled(false);

        // 图表整体配置
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setExtraBottomOffset(16f);

        chart.animateX(800);
        chart.invalidate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
