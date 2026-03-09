package com.yuan.test.vo;

import com.yuan.test.entity.TestSceneStep;
import lombok.Data;

@Data
public class SceneStepVO {
    private Long id;
    private String name;
    private String method;
    private String path;
    private Integer weight;

    public static SceneStepVO fromEntity(TestSceneStep step) {
        SceneStepVO vo = new SceneStepVO();
        vo.setId(step.getId());
        vo.setName(step.getName());
        vo.setMethod(step.getMethod());
        vo.setPath(step.getPath());
        vo.setWeight(step.getWeight());
        return vo;
    }
}
