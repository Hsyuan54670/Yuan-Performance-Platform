package com.yuan.analysis.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BaselineSnapshot {
    /** 基线运行ID */
    private Long baselineRunId;

    /** 基线 QPS */
    private BigDecimal baselineQps;

    /** 基线 P99 */
    private BigDecimal baselineP99;

    /** 基线错误率 */
    private BigDecimal baselineErrorRate;

    /** 相对基线的 QPS 变化率 */
    private BigDecimal qpsChangeRate;

    /** 相对基线的 P99 变化率 */
    private BigDecimal p99ChangeRate;

    /** 相对基线的错误率变化率 */
    private BigDecimal errorRateChangeRate;
}
