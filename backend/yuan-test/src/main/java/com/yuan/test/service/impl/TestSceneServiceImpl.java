package com.yuan.test.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuan.common.result.R;
import com.yuan.test.entity.TestScene;
import com.yuan.test.entity.TestSceneStep;
import com.yuan.test.mapper.TestSceneMapper;
import com.yuan.test.mapper.TestSceneStepMapper;
import com.yuan.test.service.TestSceneService;
import com.yuan.test.vo.SceneStepVO;
import com.yuan.test.vo.TestSceneVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestSceneServiceImpl extends ServiceImpl<TestSceneMapper,TestScene> implements TestSceneService {

    @Autowired
    TestSceneStepMapper testSceneStepMapper;

    @Override
    public R<List<TestSceneVO>> scenes(Long userId) {
        List<TestScene> list = this.lambdaQuery()
                .eq(TestScene::getUserId,userId)
                .orderByDesc(TestScene::getCreatedAt)
                .list();

        List<TestSceneVO> sceneVOList = list
                .stream()
                .map(TestSceneVO::fromEntity)
                .toList();

        sceneVOList.forEach(testScene -> {

            LambdaQueryWrapper<TestSceneStep> wrapper = new LambdaQueryWrapper<>();

            wrapper.eq(TestSceneStep::getSceneId,testScene.getId())
                    .orderByAsc(TestSceneStep::getStepOrder);
            List<SceneStepVO> stepVOList = testSceneStepMapper
                    .selectList(wrapper)
                    .stream()
                    .map(SceneStepVO::fromEntity)
                    .toList();

            testScene.setSteps(stepVOList);
        });
        return R.success(sceneVOList);
    }
}
