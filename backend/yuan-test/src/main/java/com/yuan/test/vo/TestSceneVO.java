package com.yuan.test.vo;

import com.yuan.test.entity.TestScene;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class TestSceneVO {

    private Long id;
    private String name;
    private LocalDateTime createdAt;
    private Integer taskCount = 0;
    private List<String> relatedPlanNames = new ArrayList<>();
    private List<SceneStepVO> steps = new ArrayList<>();

    public static TestSceneVO fromEntity(TestScene scene) {
        TestSceneVO vo = new TestSceneVO();
        vo.setId(scene.getId());
        vo.setName(scene.getName());
        vo.setCreatedAt(scene.getCreatedAt());
        return vo;
    }
}
