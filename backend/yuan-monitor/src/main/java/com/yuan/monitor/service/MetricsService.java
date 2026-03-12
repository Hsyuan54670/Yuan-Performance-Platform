package com.yuan.monitor.service;

import com.yuan.common.result.R;
import com.yuan.monitor.vo.MetricsSummaryVO;


public interface MetricsService {


    R<MetricsSummaryVO> getMetricsSummary();
}