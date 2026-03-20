package com.yuan.test.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuan.api.test.dto.TestMetricDTO;
import com.yuan.api.test.dto.TestRunBaselineDTO;
import com.yuan.api.test.dto.TestRunContextDTO;
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
import com.yuan.test.mq.TestStatusProducer;
import com.yuan.test.service.TestTaskService;
import com.yuan.test.vo.TestMetricVO;
import com.yuan.test.vo.TestTaskRunVO;
import com.yuan.test.vo.TestTaskVO;
import org.apache.jorphan.collections.ListedHashTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TestTaskServiceImpl extends ServiceImpl<TestTaskMapper, TestTask> implements TestTaskService {
    @Autowired
    TestPlanMapper tpMapper;
    @Autowired
    TestSceneMapper tsMapper;
    @Autowired
    TestSceneStepMapper tssMapper;
    @Autowired
    TestTaskMapper ttMapper;
    @Autowired
    TestTaskRunMapper ttrMapper;
    @Autowired
    TestMetricSecondMapper tmsMapper;
    @Autowired
    JmeterTestPlanBuilder jmeterTestPlanBuilder;
    @Autowired
    JmeterExecutionManager jmeterExecutionManager;
    @Autowired
    TaskMetricAggregator taskMetricAggregator;
    @Autowired
    TestStatusProducer tsProducer;

    @Override
    public R<List<TestTaskVO>> tasks(Long userId) {
        LambdaQueryWrapper<TestTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TestTask::getUserId, userId)
                .orderByDesc(TestTask::getCreatedAt);
        List<TestTask> list = this.list(wrapper);

        list.forEach(task -> {
            TestScene scene = tsMapper.selectById(task.getSceneId());
            TestPlan plan = tpMapper.selectById(task.getPlanId());
            task.setSceneName(scene != null ? scene.getName() : "场景已删除");
            task.setPlanName(plan != null ? plan.getName() : "计划已删除");
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
                null,
                null,
                now,
                now
        );
        ttrMapper.insert(taskRun);
        taskMetricAggregator.registerRunOwner(taskRun.getId(), userId);

        ListedHashTree testPlanTree;
        TaskResultCollector resultCollector = new TaskResultCollector(taskMetricAggregator, id, taskRun.getId());
        try {
            testPlanTree = jmeterTestPlanBuilder.build(plan, scene, testTask, steps, resultCollector, taskRun);
        } catch (Exception e) {
            taskMetricAggregator.markRunFinished(taskRun.getId());
            taskRun.setStatus("FAILED");
            taskRun.setEndTime(LocalDateTime.now());
            testTask.setStatus("FAILED");
            testTask.setEndTime(LocalDateTime.now());
            this.updateById(testTask);
            ttrMapper.updateById(taskRun);
            tsProducer.send(userId, id, taskRun.getId(), "FAILED", "任务启动失败");
            return R.fail(HttpStatus.INTERNAL_SERVER_ERROR, "JMeter启动失败");
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

        // 这里先把任务状态切到 RUNNING，但真正的开始时间以后台执行线程发出的 RUNNING 事件为准。
        testTask.setStatus("RUNNING");
        testTask.setStartTime(null);
        testTask.setEndTime(null);
        this.updateById(testTask);
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
                .orderByDesc(TestTaskRun::getCreatedAt)
                .last("limit 1");
        TestTaskRun taskRun = ttrMapper.selectOne(wrapper);

        if (taskRun == null) {
            return R.fail(HttpStatus.CONFLICT, "当前任务未在运行");
        }

        if (!"RUNNING".equals(testTask.getStatus())) {
            return R.fail(HttpStatus.CONFLICT, "当前任务未在运行");
        }

        boolean stopped = jmeterExecutionManager.stop(id);
        if (!stopped) {
            return R.fail(HttpStatus.CONFLICT, "当前任务未在运行");
        }

        // STOPPED 状态和完成事件统一由执行管理器在 JMeter 真正退出时落库并发送，避免提前结束资源跟踪窗口。
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
        testTask.setQps(BigDecimal.ZERO);
        testTask.setP99(BigDecimal.ZERO);
        testTask.setErrorRate(BigDecimal.ZERO);
        testTask.setStartTime(null);
        testTask.setEndTime(null);
        this.save(testTask);

        return R.success(testTask.getId());
    }

    @Override
    public R<List<TestTaskRunVO>> runs(Long taskId, Long userId) {
        TestTask testTask = this.getById(taskId);
        if (testTask == null) {
            return R.fail(HttpStatus.NOT_FOUND, "任务不存在");
        }
        if (!testTask.getUserId().equals(userId)) {
            return R.fail(HttpStatus.FORBIDDEN, "没有权限操作");
        }

        LambdaQueryWrapper<TestTaskRun> runWrapper = new LambdaQueryWrapper<>();
        runWrapper.eq(TestTaskRun::getTaskId, taskId)
                .orderByDesc(TestTaskRun::getCreatedAt)
                .orderByDesc(TestTaskRun::getId);

        List<TestTaskRunVO> runs = ttrMapper.selectList(runWrapper).stream()
                .map(TestTaskRunVO::fromEntity)
                .toList();
        return R.success(runs);
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

        List<TestMetricSecond> list = tmsMapper.selectList(metricWrapper);
        List<TestMetricVO> metricVOList = list.stream().map(TestMetricVO::fromEntity).toList();
        return R.success(metricVOList);
    }

    @Override
    public R<List<TestMetricDTO>> runMetrics(Long runId, Long userId) {
        TestTaskRun testTaskRun = ttrMapper.selectById(runId);
        if (testTaskRun == null) {
            return R.fail(HttpStatus.NOT_FOUND, "测试运行不存在");
        }
        TestTask testTask = ttMapper.selectById(testTaskRun.getTaskId());
        if (testTask == null) {
            return R.fail(HttpStatus.NOT_FOUND, "测试运行不存在");
        }
        if (!testTask.getUserId().equals(userId)) {
            return R.fail(HttpStatus.FORBIDDEN, "没有权限操作");
        }
        LambdaQueryWrapper<TestMetricSecond> tmsWrapper = new LambdaQueryWrapper<>();
        tmsWrapper
                .eq(TestMetricSecond::getTaskId, testTaskRun.getTaskId())
                .eq(TestMetricSecond::getRunId, testTaskRun.getId())
                .orderByAsc(TestMetricSecond::getTs);
        List<TestMetricDTO> testMetricDTOList = tmsMapper.selectList(tmsWrapper)
                .stream()
                .map(testMetricSecond -> {
                    TestMetricDTO testMetricDTO = new TestMetricDTO();
                    testMetricDTO.setErrorRate(testMetricSecond.getErrorRate());
                    testMetricDTO.setP99(testMetricSecond.getP99());
                    testMetricDTO.setQps(testMetricSecond.getQps());
                    testMetricDTO.setP90(testMetricSecond.getP90());
                    testMetricDTO.setP50(testMetricSecond.getP50());
                    testMetricDTO.setTime(testMetricSecond.getTs());
                    return testMetricDTO;
                })
                .toList();
        return R.success(testMetricDTOList);
    }

    @Override
    public R<TestRunContextDTO> runContext(Long runId, Long userId) {
        TestTaskRun testTaskRun = ttrMapper.selectById(runId);
        if (testTaskRun == null) {
            return R.fail(HttpStatus.NOT_FOUND, "测试运行不存在");
        }
        TestTask testTask = ttMapper.selectById(testTaskRun.getTaskId());
        if (testTask == null) {
            return R.fail(HttpStatus.NOT_FOUND, "测试运行不存在");
        }
        if (!testTask.getUserId().equals(userId)) {
            return R.fail(HttpStatus.FORBIDDEN, "没有权限操作");
        }

        TestPlan testPlan = tpMapper.selectById(testTask.getPlanId());
        TestScene testScene = tsMapper.selectById(testTask.getSceneId());

        TestRunContextDTO testRunContextDTO = new TestRunContextDTO();
        testRunContextDTO.setRunId(runId);
        testRunContextDTO.setTaskId(testTask.getId());
        testRunContextDTO.setPlanName(testPlan != null ? testPlan.getName() : "计划已删除");
        testRunContextDTO.setSceneName(testScene != null ? testScene.getName() : "场景已删除");
        testRunContextDTO.setStatus(testTaskRun.getStatus());
        testRunContextDTO.setStartTime(testTaskRun.getStartTime());
        testRunContextDTO.setEndTime(testTaskRun.getEndTime());
        testRunContextDTO.setUserId(userId);
        testRunContextDTO.setConcurrency(testPlan != null ? testPlan.getConcurrency() : 0);
        testRunContextDTO.setDurationSeconds(testPlan != null ? testPlan.getDuration() : 0);

        return R.success(testRunContextDTO);
    }

    @Override
    public R<TestRunBaselineDTO> runBaseline(Long runId, Long userId) {
        TestTaskRun currentRun = ttrMapper.selectById(runId);
        if (currentRun == null) {
            return R.fail(HttpStatus.NOT_FOUND, "测试运行不存在");
        }
        TestTask testTask = ttMapper.selectById(currentRun.getTaskId());
        if (testTask == null) {
            return R.fail(HttpStatus.NOT_FOUND, "测试运行不存在");
        }
        if (!testTask.getUserId().equals(userId)) {
            return R.fail(HttpStatus.FORBIDDEN, "没有权限操作");
        }

        LambdaQueryWrapper<TestTaskRun> baselineWrapper = new LambdaQueryWrapper<>();
        baselineWrapper.eq(TestTaskRun::getTaskId, currentRun.getTaskId())
                .eq(TestTaskRun::getStatus, "SUCCESS")
                .lt(TestTaskRun::getId, currentRun.getId())
                .orderByDesc(TestTaskRun::getId)
                .last("limit 1");
        TestTaskRun baselineRun = ttrMapper.selectOne(baselineWrapper);
        if (baselineRun == null) {
            return R.success(emptyBaseline());
        }

        LambdaQueryWrapper<TestMetricSecond> metricWrapper = new LambdaQueryWrapper<>();
        metricWrapper.eq(TestMetricSecond::getTaskId, baselineRun.getTaskId())
                .eq(TestMetricSecond::getRunId, baselineRun.getId())
                .orderByAsc(TestMetricSecond::getTs);
        List<TestMetricSecond> metrics = tmsMapper.selectList(metricWrapper);

        return R.success(buildBaseline(baselineRun.getId(), metrics));
    }

    private TestRunBaselineDTO emptyBaseline() {
        TestRunBaselineDTO baseline = new TestRunBaselineDTO();
        baseline.setBaselineRunId(null);
        baseline.setAvgQps(BigDecimal.ZERO);
        baseline.setP99(BigDecimal.ZERO);
        baseline.setErrorRate(BigDecimal.ZERO);
        return baseline;
    }

    private TestRunBaselineDTO buildBaseline(Long baselineRunId, List<TestMetricSecond> metrics) {
        TestRunBaselineDTO baseline = new TestRunBaselineDTO();
        baseline.setBaselineRunId(baselineRunId);
        if (metrics == null || metrics.isEmpty()) {
            baseline.setAvgQps(BigDecimal.ZERO);
            baseline.setP99(BigDecimal.ZERO);
            baseline.setErrorRate(BigDecimal.ZERO);
            return baseline;
        }

        BigDecimal avgQps = metrics.stream()
                .map(TestMetricSecond::getQps)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(metrics.size()), 2, RoundingMode.HALF_UP);

        TestMetricSecond lastMetric = metrics.get(metrics.size() - 1);
        baseline.setAvgQps(avgQps);
        baseline.setP99(lastMetric.getP99());
        baseline.setErrorRate(lastMetric.getErrorRate());
        return baseline;
    }
}



