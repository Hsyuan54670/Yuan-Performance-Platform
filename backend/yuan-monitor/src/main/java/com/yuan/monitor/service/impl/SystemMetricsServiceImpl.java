package com.yuan.monitor.service.impl;

import com.yuan.common.result.R;
import com.yuan.monitor.collector.SystemMetricsCollector;
import com.yuan.monitor.service.SystemMetricsService;
import com.yuan.monitor.vo.SystemMetricVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SystemMetricsServiceImpl implements SystemMetricsService {


    @Autowired
    SystemMetricsCollector systemMetricsCollector;


    @Override
    public R<SystemMetricVO> getSystemMetrics() {
        SystemMetricVO systemMetricVO = systemMetricsCollector.collectCurrentSystemMetrics();
        return R.success(systemMetricVO);
    }

}
