package com.yuan.api.test.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TestMetricDTO {
    /** 采样时间 */
    private LocalDateTime time;

    /** 当前秒 QPS */
    private BigDecimal qps;

    /** 当前秒 P50 */
    private BigDecimal p50;

    /** 当前秒 P90 */
    private BigDecimal p90;

    /** 当前秒 P99 */
    private BigDecimal p99;

    /** 当前秒错误率 */
    private BigDecimal errorRate;

}
