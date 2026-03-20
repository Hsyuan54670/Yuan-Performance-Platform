package com.yuan.test.vo;


import com.yuan.test.entity.TestPlan;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class TestPlanVO {
    private Long id;

    private String name;

    private String targetUrl;

    private Integer concurrency;

    private Integer duration;

    private String rampType;

    private LocalDateTime createdAt;

    private Integer taskCount = 0;

    private List<String> relatedSceneNames = new ArrayList<>();

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
