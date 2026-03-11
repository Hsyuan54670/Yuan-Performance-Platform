package com.yuan.test.vo;

import com.yuan.test.entity.TestTask;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TestTaskVO {
    private Long id;
    private String planName;
    private String sceneName;
    private String status;
    private LocalDateTime startTime;
    private Integer duration;
    private BigDecimal qps;
    private BigDecimal p99;
    private BigDecimal errorRate;

    public static TestTaskVO fromEntity(TestTask testTask) {
        TestTaskVO vo = new TestTaskVO();
        vo.setId(testTask.getId());
        vo.setPlanName(testTask.getPlanName());
        vo.setSceneName(testTask.getSceneName());
        vo.setStatus(testTask.getStatus());
        vo.setStartTime(testTask.getStartTime());
        vo.setDuration(testTask.getDuration());
        vo.setQps(testTask.getQps());
        vo.setP99(testTask.getP99());
        vo.setErrorRate(testTask.getErrorRate());
        return vo;
    }
}
