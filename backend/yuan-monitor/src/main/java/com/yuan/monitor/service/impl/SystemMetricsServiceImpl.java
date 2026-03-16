package com.yuan.monitor.service.impl;

import com.yuan.common.result.R;
import com.yuan.monitor.collector.SystemMetricsCollector;
import com.yuan.monitor.service.AlertEvaluateService;
import com.yuan.monitor.service.SystemMetricsService;
import com.yuan.monitor.vo.SystemMetricVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SystemMetricsServiceImpl implements SystemMetricsService {
    @Autowired
    SystemMetricsCollector systemMetricsCollector;

    @Autowired
    AlertEvaluateService alertEvaluateService;

    @Override
    public R<SystemMetricVO> getSystemMetrics() {
        SystemMetricVO systemMetricVO = systemMetricsCollector.collectCurrentSystemMetrics();
        try {
            alertEvaluateService.evaluateSystemMetric(systemMetricVO);
        } catch (Exception e) {
            log.error("Failed to evaluate system metric alerts", e);
        }
        return R.success(systemMetricVO);
    }
}
