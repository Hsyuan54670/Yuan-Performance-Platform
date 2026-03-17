package com.yuan.monitor.controller;

import com.yuan.common.result.R;
import com.yuan.monitor.service.MetricsService;
import com.yuan.monitor.service.SystemMetricsService;
import com.yuan.monitor.vo.MetricsSummaryVO;
import com.yuan.monitor.vo.RunSystemMetricPointVO;
import com.yuan.monitor.vo.RunSystemMetricSummaryVO;
import com.yuan.monitor.vo.SystemMetricVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping("/system-metrics/runs/{runId}/summary")
    public R<RunSystemMetricSummaryVO> getRunSystemMetricSummary(@RequestHeader("X-User-Id") Long userId,
                                                                 @PathVariable Long runId) {
        return sysMetricsService.getRunSystemMetricSummary(userId, runId);
    }

    @GetMapping("/system-metrics/runs/{runId}/points")
    public R<List<RunSystemMetricPointVO>> listRunSystemMetricPoints(@RequestHeader("X-User-Id") Long userId,
                                                                     @PathVariable Long runId) {
        return sysMetricsService.listRunSystemMetricPoints(userId, runId);
    }
}
