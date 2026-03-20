package com.yuan.test.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuan.common.constant.HttpStatus;
import com.yuan.common.result.R;
import com.yuan.test.dto.TestSceneDTO;
import com.yuan.test.dto.TestSceneStepDTO;
import com.yuan.test.entity.TestPlan;
import com.yuan.test.entity.TestScene;
import com.yuan.test.entity.TestSceneStep;
import com.yuan.test.entity.TestTask;
import com.yuan.test.mapper.TestPlanMapper;
import com.yuan.test.mapper.TestSceneMapper;
import com.yuan.test.mapper.TestSceneStepMapper;
import com.yuan.test.mapper.TestTaskMapper;
import com.yuan.test.service.TestSceneService;
import com.yuan.test.vo.SceneStepVO;
import com.yuan.test.vo.TestSceneVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TestSceneServiceImpl extends ServiceImpl<TestSceneMapper, TestScene> implements TestSceneService {

    @Autowired
    TestSceneStepMapper testSceneStepMapper;

    @Autowired
    TestTaskMapper testTaskMapper;

    @Autowired
    TestPlanMapper testPlanMapper;

    @Override
    public R<List<TestSceneVO>> scenes(Long userId) {
        List<TestScene> scenes = this.lambdaQuery()
                .eq(TestScene::getUserId, userId)
                .orderByDesc(TestScene::getCreatedAt)
                .list();
        return R.success(buildSceneVOs(scenes, userId));
    }

    @Override
    @Transactional
    public R<Long> create(TestSceneDTO request, Long userId) {
        TestScene scene = new TestScene();
        scene.setUserId(userId);
        scene.setName(request.getName());
        scene.setCreatedAt(LocalDateTime.now());
        this.save(scene);
        saveSteps(scene.getId(), request.getSteps());
        return R.success(scene.getId());
    }

    @Override
    @Transactional
    public R<Void> update(Long id, TestSceneDTO request, Long userId) {
        TestScene scene = this.getById(id);
        if (scene == null) {
            return R.fail(HttpStatus.NOT_FOUND, "场景不存在");
        }
        if (!scene.getUserId().equals(userId)) {
            return R.fail(HttpStatus.FORBIDDEN, "没有权限操作");
        }

        scene.setName(request.getName());
        this.updateById(scene);

        testSceneStepMapper.delete(new LambdaQueryWrapper<TestSceneStep>()
                .eq(TestSceneStep::getSceneId, id));
        // 场景步骤整体由前端按当前顺序提交，更新时重建一遍最稳妥。
        saveSteps(id, request.getSteps());
        return R.success();
    }

    @Override
    @Transactional
    public R<Void> delete(Long id, Long userId) {
        TestScene scene = this.getById(id);
        if (scene == null) {
            return R.fail(HttpStatus.NOT_FOUND, "场景不存在");
        }
        if (!scene.getUserId().equals(userId)) {
            return R.fail(HttpStatus.FORBIDDEN, "没有权限操作");
        }

        testSceneStepMapper.delete(new LambdaQueryWrapper<TestSceneStep>()
                .eq(TestSceneStep::getSceneId, id));
        this.removeById(id);
        return R.success();
    }

    private void saveSteps(Long sceneId, List<TestSceneStepDTO> steps) {
        LocalDateTime now = LocalDateTime.now();
        for (int index = 0; index < steps.size(); index++) {
            TestSceneStepDTO step = steps.get(index);
            TestSceneStep entity = new TestSceneStep();
            entity.setSceneId(sceneId);
            entity.setStepOrder(index + 1);
            entity.setName(step.getName());
            entity.setMethod(step.getMethod());
            entity.setPath(step.getPath());
            entity.setWeight(step.getWeight());
            entity.setCreatedAt(now);
            testSceneStepMapper.insert(entity);
        }
    }

    private List<TestSceneVO> buildSceneVOs(List<TestScene> scenes, Long userId) {
        if (scenes.isEmpty()) {
            return List.of();
        }

        List<Long> sceneIds = scenes.stream().map(TestScene::getId).toList();

        Map<Long, List<SceneStepVO>> stepsBySceneId = testSceneStepMapper.selectList(new LambdaQueryWrapper<TestSceneStep>()
                        .in(TestSceneStep::getSceneId, sceneIds)
                        .orderByAsc(TestSceneStep::getSceneId)
                        .orderByAsc(TestSceneStep::getStepOrder))
                .stream()
                .collect(Collectors.groupingBy(
                        TestSceneStep::getSceneId,
                        Collectors.mapping(SceneStepVO::fromEntity, Collectors.toList())
                ));

        List<TestTask> tasks = testTaskMapper.selectList(new LambdaQueryWrapper<TestTask>()
                .eq(TestTask::getUserId, userId)
                .in(TestTask::getSceneId, sceneIds)
                .orderByDesc(TestTask::getCreatedAt));

        Map<Long, List<TestTask>> tasksBySceneId = tasks.stream()
                .collect(Collectors.groupingBy(TestTask::getSceneId));

        Set<Long> planIds = tasks.stream()
                .map(TestTask::getPlanId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, TestPlan> planMap = planIds.isEmpty()
                ? Collections.emptyMap()
                : testPlanMapper.selectBatchIds(planIds).stream()
                .collect(Collectors.toMap(TestPlan::getId, Function.identity()));

        return scenes.stream().map(scene -> {
            TestSceneVO vo = TestSceneVO.fromEntity(scene);
            vo.setSteps(stepsBySceneId.getOrDefault(scene.getId(), List.of()));
            List<TestTask> sceneTasks = tasksBySceneId.getOrDefault(scene.getId(), List.of());
            vo.setTaskCount(sceneTasks.size());
            LinkedHashSet<String> relatedPlanNames = sceneTasks.stream()
                    .map(TestTask::getPlanId)
                    .map(planMap::get)
                    .filter(java.util.Objects::nonNull)
                    .map(TestPlan::getName)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            vo.setRelatedPlanNames(List.copyOf(relatedPlanNames));
            return vo;
        }).toList();
    }
}
