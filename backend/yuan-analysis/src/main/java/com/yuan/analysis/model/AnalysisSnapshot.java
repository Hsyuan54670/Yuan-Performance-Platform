package com.yuan.analysis.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AnalysisSnapshot {
    /** 用户ID */
    private Long userId;

    /** 任务ID */
    private Long taskId;

    /** 运行ID */
    private Long runId;

    /** 最终状态，例如 SUCCESS / FAILED / STOPPED */
    private String finalStatus;

    /** 运行上下文信息，例如场景、并发、持续时间 */
    private RunContext context = new RunContext();

    /** 本次运行的指标摘要 */
    private MetricSummary summary = new MetricSummary();

    /** 从原始数据中提取出的分析特征 */
    private MetricFeature feature = new MetricFeature();

    /** 历史基线快照，用于做回归对比 */
    private BaselineSnapshot baseline = new BaselineSnapshot();

    /** 当前命中的规则结果，既可用于结构化报告，也可作为 AI 输入 */
    private List<RuleHit> ruleHits = new ArrayList<>();

    /** 秒级时序数据容器，MVP 阶段允许为空，后续可按需加载 */
    private MetricSeriesBundle series = new MetricSeriesBundle();

    /** 告警证据数量，作为辅助信息保留 */
    private Integer alertEvidenceCount;

    /** 告警证据列表，作为辅助信息保留 */
    private List<AlertEvidenceItem> alertEvidence = new ArrayList<>();
}
