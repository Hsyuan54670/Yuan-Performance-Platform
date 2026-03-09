package com.yuan.test.vo;

import com.yuan.test.entity.TestScene;
import lombok.Data;

import java.util.List;

@Data
public class TestSceneVO {

    private Long id;
    private String name;
    private List<SceneStepVO> steps;

    public static TestSceneVO fromEntity(TestScene scene) {
        TestSceneVO vo = new TestSceneVO();
        vo.setId(scene.getId());
        vo.setName(scene.getName());
        return vo;
    }
}
