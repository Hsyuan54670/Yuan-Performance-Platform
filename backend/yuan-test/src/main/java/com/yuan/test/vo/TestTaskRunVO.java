package com.yuan.test.vo;

import com.yuan.test.entity.TestTaskRun;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TestTaskRunVO {
    private Long id;
    private Long taskId;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;

    public static TestTaskRunVO fromEntity(TestTaskRun run) {
        TestTaskRunVO vo = new TestTaskRunVO();
        vo.setId(run.getId());
        vo.setTaskId(run.getTaskId());
        vo.setStatus(run.getStatus());
        vo.setStartTime(run.getStartTime());
        vo.setEndTime(run.getEndTime());
        vo.setCreatedAt(run.getCreatedAt());
        return vo;
    }
}
