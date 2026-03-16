package com.yuan.analysis.collector;

import com.yuan.analysis.model.AnalysisSnapshot;

public interface AnalysisDataCollector {
    /**
     * 按一次运行收集分析快照。
     *
     * @param userId 当前用户ID
     * @param taskId 当前任务ID
     * @param runId 当前运行ID
     * @param finalStatus 本次运行最终状态
     * @return 面向规则引擎和 AI 的分析快照
     */
    AnalysisSnapshot collectSnapshot(Long userId, Long taskId, Long runId, String finalStatus);
}
