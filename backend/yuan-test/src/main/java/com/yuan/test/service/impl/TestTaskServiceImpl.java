package com.yuan.test.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuan.common.constant.HttpStatus;
import com.yuan.common.result.R;
import com.yuan.test.collector.TaskMetricAggregator;
import com.yuan.test.collector.TaskResultCollector;
import com.yuan.test.dto.TestTaskCreateDTO;
import com.yuan.test.entity.TestMetricSecond;
import com.yuan.test.entity.TestPlan;
import com.yuan.test.entity.TestScene;
import com.yuan.test.entity.TestSceneStep;
import com.yuan.test.entity.TestTask;
import com.yuan.test.entity.TestTaskRun;
import com.yuan.test.jmeter.JmeterExecutionManager;
import com.yuan.test.jmeter.JmeterTestPlanBuilder;
import com.yuan.test.mapper.TestMetricSecondMapper;
import com.yuan.test.mapper.TestPlanMapper;
import com.yuan.test.mapper.TestSceneMapper;
import com.yuan.test.mapper.TestSceneStepMapper;
import com.yuan.test.mapper.TestTaskMapper;
import com.yuan.test.mapper.TestTaskRunMapper;
import com.yuan.test.mq.TestCompletedProducer;
import com.yuan.test.mq.TestStatusProducer;
import com.yuan.test.service.TestTaskService;
import com.yuan.test.vo.TestMetricVO;
import com.yuan.test.vo.TestTaskVO;
import org.apache.jorphan.collections.ListedHashTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TestTaskServiceImpl extends ServiceImpl<TestTaskMapper, TestTask> implements TestTaskService {
    private final TestStatusProducer tsProducer;
    private final TestTaskRunMapper ttrMapper;
    private final TaskMetricAggregator taskMetricAggregator;
    private final TestSceneStepMapper tssMapper;
    private final TestSceneMapper tsMapper;
    private final TestPlanMapper tpMapper;
    private final TestMetricSecondMapper tsmMapper;
    private final JmeterTestPlanBuilder jmeterTestPlanBuilder;
    private final JmeterExecutionManager jmeterExecutionManager;

    @Autowired
    private TestCompletedProducer tcProducer;

    public TestTaskServiceImpl(TestStatusProducer tsProducer, TestSceneMapper tsMapper, TestPlanMapper tpMapper, TestMetricSecondMapper tsmMapper, JmeterTestPlanBuilder jmeterTestPlanBuilder, JmeterExecutionManager jmeterExecutionManager, TestSceneStepMapper testSceneStepMapper, TaskMetricAggregator taskMetricAggregator, TestTaskRunMapper testTaskRunMapper) {
        this.tsProducer = tsProducer;
        this.tsMapper = tsMapper;
        this.tpMapper = tpMapper;
        this.tsmMapper = tsmMapper;
        this.jmeterTestPlanBuilder = jmeterTestPlanBuilder;
        this.jmeterExecutionManager = jmeterExecutionManager;
        this.tssMapper = testSceneStepMapper;
        this.taskMetricAggregator = taskMetricAggregator;
        this.ttrMapper = testTaskRunMapper;
    }

    @Override
    public R<List<TestTaskVO>> tasks(Long userId) {
        List<TestTask> list = this.lambdaQuery()
                .eq(TestTask::getUserId, userId)
                .orderByDesc(TestTask::getCreatedAt)
                .list();

        list.forEach(task -> {
            var scene = tsMapper.selectById(task.getSceneId());
            var plan = tpMapper.selectById(task.getPlanId());
            if (scene != null) {
                task.setSceneName(scene.getName());
            } else {
                task.setSceneName("场景已删除");
            }
            if (plan != null) {
                task.setPlanName(plan.getName());
            } else {
                task.setPlanName("计划已删除");
            }
        });

        List<TestTaskVO> taskVOList = list.stream().map(TestTaskVO::fromEntity).toList();
        return R.success(taskVOList);
    }

    @Override
    public R<Void> start(Long id, Long userId) {
        TestTask testTask = this.getById(id);
        if (testTask == null) {
            return R.fail(HttpStatus.NOT_FOUND, "任务不存在");
        }
        if (!testTask.getUserId().equals(userId)) {
            return R.fail(HttpStatus.FORBIDDEN, "没有权限操作");
        }
        if ("RUNNING".equals(testTask.getStatus())) {
            return R.fail(HttpStatus.CONFLICT, "任务正在运行中");
        }

        TestPlan plan = tpMapper.selectById(testTask.getPlanId());
        if (plan == null) {
            return R.fail(HttpStatus.NOT_FOUND, "未找到测试计划");
        }
        TestScene scene = tsMapper.selectById(testTask.getSceneId());
        if (scene == null) {
            return R.fail(HttpStatus.NOT_FOUND, "未找到测试场景");
        }
        LambdaQueryWrapper<TestSceneStep> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TestSceneStep::getSceneId, scene.getId())
                .orderByAsc(TestSceneStep::getStepOrder);
        List<TestSceneStep> steps = tssMapper.selectList(wrapper);
        if (steps == null || steps.isEmpty()) {
            return R.fail(HttpStatus.CONFLICT, "当前场景没有可执行步骤");
        }

        LocalDateTime now = LocalDateTime.now();
        TestTaskRun taskRun = new TestTaskRun(
                null,
                id,
                "RUNNING",
                now,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        ttrMapper.insert(taskRun);
        taskMetricAggregator.registerRunOwner(taskRun.getId(), userId);

        ListedHashTree testPlanTree;
        TaskResultCollector resultCollector = new TaskResultCollector(taskMetricAggregator, id, taskRun.getId());
        try {
            testPlanTree = jmeterTestPlanBuilder.build(plan, scene, testTask, steps, resultCollector);
        } catch (Exception e) {
            taskMetricAggregator.markRunFinished(taskRun.getId());
            taskRun.setStatus("FAILED");
            taskRun.setEndTime(LocalDateTime.now());
            testTask.setStatus("FAILED");
            testTask.setEndTime(LocalDateTime.now());
            this.updateById(testTask);
            ttrMapper.updateById(taskRun);
            tsProducer.send(userId, id, taskRun.getId(), "FAILED", "任务启动失败");
            return R.fail(500, "JMeter启动失败");
        }

        boolean started;
        try {
            started = jmeterExecutionManager.start(userId, id, taskRun.getId(), testPlanTree, plan.getDuration());
        } catch (Exception e) {
            taskMetricAggregator.markRunFinished(taskRun.getId());
            taskRun.setStatus("FAILED");
            taskRun.setEndTime(LocalDateTime.now());
            testTask.setStatus("FAILED");
            testTask.setEndTime(LocalDateTime.now());
            this.updateById(testTask);
            ttrMapper.updateById(taskRun);

            tsProducer.send(userId, id, taskRun.getId(), "FAILED", "JMeter启动失败");
            return R.fail(HttpStatus.INTERNAL_SERVER_ERROR, "JMeter启动失败");
        }

        if (!started) {
            return R.fail(HttpStatus.CONFLICT, "任务正在运行中");
        }

        testTask.setStatus("RUNNING");
        testTask.setStartTime(now);
        testTask.setEndTime(null);
        this.updateById(testTask);

        tsProducer.send(userId, id, taskRun.getId(), "RUNNING", "任务开始执行");
        return R.success();
    }

    @Override
    public R<Void> stop(Long id, Long userId) {
        TestTask testTask = this.getById(id);
        if (testTask == null) {
            return R.fail(HttpStatus.CONFLICT, "当前任务未在运行");
        }
        if (!testTask.getUserId().equals(userId)) {
            return R.fail(HttpStatus.FORBIDDEN, "没有权限操作");
        }

        LambdaQueryWrapper<TestTaskRun> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TestTaskRun::getTaskId, id)
                .eq(TestTaskRun::getStatus, "RUNNING")
                .orderByDesc(TestTaskRun::getStartTime)
                .last("limit 1");
        TestTaskRun taskRun = ttrMapper.selectOne(wrapper);

        if (taskRun == null) {
            return R.fail(HttpStatus.CONFLICT, "当前任务未在运行");
        }

        if (!"RUNNING".equals(testTask.getStatus())) {
            return R.fail(HttpStatus.CONFLICT, "未运行状态不可停止");
        }
        if (!jmeterExecutionManager.stop(id)) {
            return R.fail(HttpStatus.CONFLICT, "当前任务未在执行中");
        }

        taskMetricAggregator.markRunFinished(taskRun.getId());
        taskRun.setStatus("STOPPED");
        taskRun.setEndTime(LocalDateTime.now());
        ttrMapper.updateById(taskRun);
        testTask.setStatus("STOPPED");
        testTask.setEndTime(LocalDateTime.now());
        this.updateById(testTask);

        tsProducer.send(userId, id, taskRun.getId(), "STOPPED", "任务被用户停止");
        tcProducer.send(userId, id, taskRun.getId(), "STOPPED");
        return R.success();
    }

    @Override
    public R<Long> create(TestTaskCreateDTO request, Long userId) {
        Long sceneId = request.getSceneId();
        Long planId = request.getPlanId();
        TestScene testScene = tsMapper.selectById(sceneId);
        if (testScene == null) {
            return R.fail(HttpStatus.NOT_FOUND, "场景不存在");
        }
        if (!testScene.getUserId().equals(userId)) {
            return R.fail(HttpStatus.FORBIDDEN, "没有权限操作");
        }
        TestPlan testPlan = tpMapper.selectById(planId);
        if (testPlan == null) {
            return R.fail(HttpStatus.NOT_FOUND, "计划不存在");
        }
        if (!testPlan.getUserId().equals(userId)) {
            return R.fail(HttpStatus.FORBIDDEN, "没有权限操作");
        }

        TestTask testTask = new TestTask();
        testTask.setUserId(userId);
        testTask.setSceneId(sceneId);
        testTask.setPlanId(planId);
        testTask.setStatus("PENDING");
        testTask.setCreatedAt(LocalDateTime.now());
        testTask.setDuration(0);
        testTask.setQps(BigDecimal.valueOf(0));
        testTask.setP99(BigDecimal.valueOf(0));
        testTask.setErrorRate(BigDecimal.valueOf(0));
        testTask.setStartTime(null);
        testTask.setEndTime(null);
        this.save(testTask);

        return R.success(testTask.getId());
    }

    @Override
    public R<List<TestMetricVO>> metrics(Long id, Long userId) {
        TestTask testTask = this.getById(id);
        if (testTask == null) {
            return R.fail(HttpStatus.NOT_FOUND, "任务不存在");
        }
        if (!testTask.getUserId().equals(userId)) {
            return R.fail(HttpStatus.FORBIDDEN, "没有权限操作");
        }

        LambdaQueryWrapper<TestTaskRun> ttrWrapper = new LambdaQueryWrapper<>();
        ttrWrapper.eq(TestTaskRun::getTaskId, id)
                .orderByDesc(TestTaskRun::getStartTime)
                .last("limit 1");
        TestTaskRun taskRun = ttrMapper.selectOne(ttrWrapper);
        if (taskRun == null) {
            return R.success(List.of());
        }

        LambdaQueryWrapper<TestMetricSecond> metricWrapper = new LambdaQueryWrapper<TestMetricSecond>()
                .eq(TestMetricSecond::getTaskId, id)
                .eq(TestMetricSecond::getRunId, taskRun.getId())
                .orderByAsc(TestMetricSecond::getTs);

        List<TestMetricSecond> list = tsmMapper.selectList(metricWrapper);
        List<TestMetricVO> metricVOList = list.stream().map(TestMetricVO::fromEntity).toList();
        return R.success(metricVOList);
    }
}
