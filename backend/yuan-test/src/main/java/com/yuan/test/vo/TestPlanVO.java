package com.yuan.test.vo;


import com.yuan.test.entity.TestPlan;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TestPlanVO {
    private Long id;

    private String name;

    private String targetUrl;

    private Integer concurrency;

    private Integer duration;

    private String rampType;

    private LocalDateTime createdAt;

    // 静态方法来将实体类转换为VO
    public static TestPlanVO fromEntity(TestPlan testPlan) {
        TestPlanVO vo = new TestPlanVO();
        vo.setId(testPlan.getId());
        vo.setName(testPlan.getName());
        vo.setTargetUrl(testPlan.getTargetUrl());
        vo.setConcurrency(testPlan.getConcurrency());
        vo.setDuration(testPlan.getDuration());
        vo.setRampType(testPlan.getRampType());
        vo.setCreatedAt(testPlan.getCreatedAt());
        return vo;
    }
}
