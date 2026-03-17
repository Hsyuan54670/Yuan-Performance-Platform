package com.yuan.monitor.service;

import com.yuan.common.result.R;
import com.yuan.monitor.vo.RunSystemMetricPointVO;
import com.yuan.monitor.vo.RunSystemMetricSummaryVO;
import com.yuan.monitor.vo.SystemMetricVO;

import java.util.List;

public interface SystemMetricsService {
    /**
     * 获取系统指标数据
     *
     * @return 系统指标数据对象
     */
    R<SystemMetricVO> getSystemMetrics();

    /**
     * 按运行维度返回系统资源摘要，供 analysis 构建运行快照使用。
     *
     * @param userId 当前用户ID
     * @param runId  压测运行ID
     * @return CPU / Memory 摘要数据
     */
    R<RunSystemMetricSummaryVO> getRunSystemMetricSummary(Long userId, Long runId);

    /**
     * 按运行维度返回秒级系统资源原始点，供 analysis 将资源侧时序并入统一快照。
     *
     * @param userId 当前用户ID
     * @param runId  压测运行ID
     * @return 秒级 CPU / Memory 原始点
     */
    R<List<RunSystemMetricPointVO>> listRunSystemMetricPoints(Long userId, Long runId);
}
