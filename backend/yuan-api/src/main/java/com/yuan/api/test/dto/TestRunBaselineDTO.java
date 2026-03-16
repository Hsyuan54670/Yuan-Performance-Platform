package com.yuan.api.test.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TestRunBaselineDTO {
    /** 基线运行ID */
    private Long baselineRunId;

    /** 基线平均QPS */
    private BigDecimal avgQps;

    /** 基线P99 */
    private BigDecimal p99;

    /** 基线错误率 */
    private BigDecimal errorRate;
}
