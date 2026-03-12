package com.yuan.monitor.service;

import com.yuan.common.result.R;
import com.yuan.monitor.vo.MetricsSummaryVO;
import com.yuan.monitor.vo.SystemMetricVO;

public interface SystemMetricsService {
    /**
     * 获取系统指标数据
     *
     * @return 系统指标数据对象
     */
    R<SystemMetricVO> getSystemMetrics();


}


