package com.yuan.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.common.result.R;
import com.yuan.monitor.entity.MonitorMetricsRecord;
import com.yuan.monitor.entity.TestStatusRecord;
import com.yuan.monitor.mapper.MonitorMetricsRecordMapper;
import com.yuan.monitor.mapper.TestStatusRecordMapper;
import com.yuan.monitor.service.MetricsService;
import com.yuan.monitor.vo.MetricsSummaryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MetricsServiceImpl implements MetricsService {
    @Autowired
    TestStatusRecordMapper tsrMapper;

    @Autowired
    MonitorMetricsRecordMapper mmrMapper;

    @Override
    public R<MetricsSummaryVO> getMetricsSummary() {
        LambdaQueryWrapper<MonitorMetricsRecord> metricWrapper = new LambdaQueryWrapper<>();
        metricWrapper
                .orderByDesc(MonitorMetricsRecord::getCreatedAt)
                .last("LIMIT 1");
        MonitorMetricsRecord latestMetric = mmrMapper.selectOne(metricWrapper);

        if(latestMetric == null){
            return R.success(new MetricsSummaryVO());
        }

        LambdaQueryWrapper<TestStatusRecord> statusWrapper = new LambdaQueryWrapper<>();
        statusWrapper.eq(TestStatusRecord::getRunId, latestMetric.getRunId())
                .orderByDesc(TestStatusRecord::getCreatedAt)
                .last("LIMIT 1");
        TestStatusRecord latestStatus = tsrMapper.selectOne(statusWrapper);

        MetricsSummaryVO metricsSummaryVO = new MetricsSummaryVO();
        metricsSummaryVO.setRunId(latestMetric.getRunId());
        metricsSummaryVO.setTaskId(latestMetric.getTaskId());
        metricsSummaryVO.setQps(latestMetric.getQps());
        metricsSummaryVO.setP99(latestMetric.getP99());
        metricsSummaryVO.setErrorRate(latestMetric.getErrorRate());
        metricsSummaryVO.setTimestamp(latestMetric.getCreatedAt());

        if(latestStatus == null){
            metricsSummaryVO.setStatus("UNKNOWN");
            return R.success(metricsSummaryVO);
        }

        metricsSummaryVO.setStatus(latestStatus.getStatus());
        return R.success(metricsSummaryVO);
    }
}
