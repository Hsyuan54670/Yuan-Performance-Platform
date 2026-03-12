package com.yuan.monitor.controller;

import com.yuan.common.result.R;
import com.yuan.monitor.service.MetricsService;
import com.yuan.monitor.service.SystemMetricsService;
import com.yuan.monitor.vo.MetricsSummaryVO;
import com.yuan.monitor.vo.SystemMetricVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/monitor")
public class MetricsController {
    @Autowired
    private SystemMetricsService sysMetricsService;
    @Autowired
    private MetricsService metricsService;

    @GetMapping("/system-metrics")
    public R<SystemMetricVO> getSystemMetrics() {
        return sysMetricsService.getSystemMetrics();
    }

    @GetMapping("/metrics-summary")
    public R<MetricsSummaryVO> getMetricsSummary() {
        return metricsService.getMetricsSummary();
    }

}