package com.yuan.analysis.model;

import lombok.Data;

@Data
public class RuleHit {
    /** 规则编码，便于程序侧识别和去重 */
    private String code;

    /** 问题类型，例如 HIGH_LATENCY / CPU_PRESSURE */
    private String type;

    /** 严重程度，例如 LOW / MEDIUM / HIGH / CRITICAL */
    private String severity;

    /** 规则标题，面向前端和 AI 展示 */
    private String title;

    /** 命中原因，说明为什么触发该规则 */
    private String reason;

    /** 证据摘要，例如 P99=2300ms 持续 12 秒 */
    private String evidence;

    /** 建议优先级，例如 P0 / P1 / P2 */
    private String priority;

    /** 建议标题 */
    private String suggestionTitle;

    /** 建议详情 */
    private String suggestionDetail;
}
