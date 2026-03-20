package com.yuan.test.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuan.common.constant.HttpStatus;
import com.yuan.common.result.R;
import com.yuan.test.constant.RampType;
import com.yuan.test.dto.TestPlanDTO;
import com.yuan.test.entity.TestPlan;
import com.yuan.test.entity.TestScene;
import com.yuan.test.entity.TestTask;
import com.yuan.test.mapper.TestPlanMapper;
import com.yuan.test.mapper.TestSceneMapper;
import com.yuan.test.mapper.TestTaskMapper;
import com.yuan.test.service.TestPlanService;
import com.yuan.test.vo.TestPlanVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TestPlanServiceImpl extends ServiceImpl<TestPlanMapper, TestPlan> implements TestPlanService {

    @Autowired
    TestTaskMapper testTaskMapper;

    @Autowired
    TestSceneMapper testSceneMapper;

    @Override
    public R<List<TestPlanVO>> plans(Long userId) {
        List<TestPlan> plans = this.lambdaQuery()
                .eq(TestPlan::getUserId, userId)
                .orderByDesc(TestPlan::getCreatedAt)
                .list();
        return R.success(buildPlanVOs(plans, userId));
    }

    @Override
    public R<Long> create(TestPlanDTO request, Long userId) {
        TestPlan plan = new TestPlan();
        applyRequest(plan, request, userId);
        plan.setCreatedAt(LocalDateTime.now());
        this.save(plan);
        return R.success(plan.getId());
    }

    @Override
    public R<Void> update(Long id, TestPlanDTO request, Long userId) {
        TestPlan plan = this.getById(id);
        if (plan == null) {
            return R.fail(HttpStatus.NOT_FOUND, "计划不存在");
        }
        if (!plan.getUserId().equals(userId)) {
            return R.fail(HttpStatus.FORBIDDEN, "没有权限操作");
        }
        applyRequest(plan, request, userId);
        this.updateById(plan);
        return R.success();
    }

    @Override
    public R<Void> delete(Long id, Long userId) {
        TestPlan plan = this.getById(id);
        if (plan == null) {
            return R.fail(HttpStatus.NOT_FOUND, "计划不存在");
        }
        if (!plan.getUserId().equals(userId)) {
            return R.fail(HttpStatus.FORBIDDEN, "没有权限操作");
        }
        this.removeById(id);
        return R.success();
    }

    private void applyRequest(TestPlan plan, TestPlanDTO request, Long userId) {
        plan.setUserId(userId);
        plan.setName(request.getName());
        plan.setTargetUrl(request.getTargetUrl());
        plan.setConcurrency(request.getConcurrency());
        plan.setDuration(request.getDuration());
        // rampType 先统一收敛成系统内认识的枚举值，避免大小写或非法字符串污染计划数据。
        plan.setRampType(RampType.fromValue(request.getRampType()).name());
    }

    private List<TestPlanVO> buildPlanVOs(List<TestPlan> plans, Long userId) {
        if (plans.isEmpty()) {
            return List.of();
        }

        List<Long> planIds = plans.stream().map(TestPlan::getId).toList();
        List<TestTask> tasks = testTaskMapper.selectList(new LambdaQueryWrapper<TestTask>()
                .eq(TestTask::getUserId, userId)
                .in(TestTask::getPlanId, planIds)
                .orderByDesc(TestTask::getCreatedAt));

        Map<Long, List<TestTask>> tasksByPlanId = tasks.stream()
                .collect(Collectors.groupingBy(TestTask::getPlanId));

        Map<Long, TestScene> sceneMap = tasks.stream()
                .map(TestTask::getSceneId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        sceneIds -> sceneIds.isEmpty()
                                ? Collections.<Long, TestScene>emptyMap()
                                : testSceneMapper.selectBatchIds(sceneIds).stream()
                                .collect(Collectors.toMap(TestScene::getId, Function.identity()))
                ));

        return plans.stream().map(plan -> {
            TestPlanVO vo = TestPlanVO.fromEntity(plan);
            List<TestTask> planTasks = tasksByPlanId.getOrDefault(plan.getId(), List.of());
            vo.setTaskCount(planTasks.size());
            LinkedHashSet<String> relatedSceneNames = planTasks.stream()
                    .map(TestTask::getSceneId)
                    .map(sceneMap::get)
                    .filter(java.util.Objects::nonNull)
                    .map(TestScene::getName)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            vo.setRelatedSceneNames(List.copyOf(relatedSceneNames));
            return vo;
        }).toList();
    }
}
