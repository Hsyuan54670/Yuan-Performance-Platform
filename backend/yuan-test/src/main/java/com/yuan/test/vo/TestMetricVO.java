package com.yuan.test.vo;

import com.yuan.test.entity.TestMetricSecond;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TestMetricVO {

    private LocalDateTime time;

    private BigDecimal qps;

    private BigDecimal p50;

    private BigDecimal p90;

    private BigDecimal p99;

    private BigDecimal errorRate;

    public static TestMetricVO fromEntity(TestMetricSecond entity) {
        TestMetricVO vo = new TestMetricVO();
        vo.setTime(entity.getTs());
        vo.setQps(entity.getQps());
        vo.setP50(entity.getP50());
        vo.setP90(entity.getP90());
        vo.setP99(entity.getP99());
        vo.setErrorRate(entity.getErrorRate());
        return vo;
    }
}
