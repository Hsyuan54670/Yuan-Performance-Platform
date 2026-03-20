package com.yuan.test.jmeter;

import com.yuan.test.constant.RampType;
import com.yuan.test.entity.TestScene;
import com.yuan.test.entity.TestSceneStep;
import com.yuan.test.entity.TestTask;
import com.yuan.test.entity.TestTaskRun;
import lombok.extern.slf4j.Slf4j;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.control.gui.LoopControlPanel;
import org.apache.jmeter.protocol.http.config.gui.HttpDefaultsGui;
import org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jorphan.collections.ListedHashTree;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * JMeter 测试计划构建器
 * 功能：构建一个「测试计划 → 线程组 → HTTP请求默认值 → HTTP采样器 → 监听器」的完整压测脚本
 */
@Slf4j
@Component
public class JmeterTestPlanBuilder {
    private static final int DEFAULT_STAIR_STEPS = 5;
    private static final int MAX_RAMP_WINDOW_SECONDS = 60;

    private final JmeterConfig jmeterConfig;
    private final JmeterInitializer jmeterInitializer;

    public JmeterTestPlanBuilder(JmeterConfig jmeterConfig, JmeterInitializer jmeterInitializer) {
        this.jmeterConfig = jmeterConfig;
        this.jmeterInitializer = jmeterInitializer;
    }

    /**
     * 核心方法：构建完整的 JMeter HashTree（JMeter的执行树结构）
     * @return 可直接运行的 HashTree
     */
    public ListedHashTree build(com.yuan.test.entity.TestPlan plan, TestScene scene, TestTask task,
                                List<TestSceneStep> steps, ResultCollector resultCollector, TestTaskRun taskRun) throws Exception {
        // ====================== 0. 初始化 JMeter 环境（必须第一步做！） ======================
        jmeterInitializer.initializeJmeterEngine();

        // ====================== 1. 创建测试计划（TestPlan）：根节点 ======================
        TestPlan testPlan = new TestPlan(plan.getName());
        testPlan.setFunctionalMode(false);
        testPlan.setTearDownOnShutdown(true);
        testPlan.setUserDefinedVariables(new Arguments());

        // ====================== 2. 计算线程组方案：线性加压用单线程组，阶梯加压拆成多个分段线程组 ======================
        List<ThreadGroupSpec> threadGroupSpecs = buildThreadGroupSpecs(plan, scene);

        // ====================== 3. 创建 HTTP 请求默认值（可选但推荐：统一配置域名、协议等） ======================
        URL targetUrl = new URL(plan.getTargetUrl());
        String protocol = targetUrl.getProtocol();
        String domain = targetUrl.getHost();
        int port = targetUrl.getPort();
        if (port == -1) {
            port = targetUrl.getDefaultPort();
        }

        // ====================== 4. 创建监听器（Listener：收集结果） ======================
        resultCollector.setName("汇总报告");
        resultCollector.setFilename(Paths.get(jmeterConfig.getResultsDir(), "run_" + taskRun.getId() + ".jtl").toString());

        // ====================== 5. 组装 HashTree（核心：构建层级结构） ======================
        ListedHashTree testPlanTree = new ListedHashTree();
        testPlanTree.add(testPlan);

        ListedHashTree threadGroupTree = testPlanTree.getTree(testPlan);
        for (ThreadGroupSpec spec : threadGroupSpecs) {
            ThreadGroup threadGroup = createThreadGroup(spec);
            threadGroupTree.add(threadGroup);

            // 每个线程组都挂一份 HTTP 默认值和采样器，保证线性/阶梯两种模式下请求树结构一致。
            ListedHashTree samplerTree = threadGroupTree.getTree(threadGroup);
            samplerTree.add(createHttpDefaults(protocol, domain, port));
            // TODO step.weight 权重未处理
            for (TestSceneStep step : steps) {
                samplerTree.add(createHTTPSampler(step));
            }
        }
        // 监听器挂在 TestPlan 层，统一接收所有线程组的结果。
        threadGroupTree.add(resultCollector);

        return testPlanTree;
    }

    private List<ThreadGroupSpec> buildThreadGroupSpecs(com.yuan.test.entity.TestPlan plan, TestScene scene) {
        int concurrency = Math.max(1, plan.getConcurrency());
        int durationSeconds = Math.max(1, plan.getDuration());
        int rampWindowSeconds = computeRampWindowSeconds(durationSeconds);
        RampType rampType = RampType.fromValue(plan.getRampType());

        if (rampType == RampType.STAIR) {
            return buildStairThreadGroups(scene.getName(), concurrency, durationSeconds, rampWindowSeconds);
        }

        return List.of(new ThreadGroupSpec(
                scene.getName(),
                concurrency,
                Math.max(1, rampWindowSeconds),
                0,
                durationSeconds
        ));
    }

    /**
     * 当前计划模型还没有独立的加压窗口字段，所以这里先用一套稳定的默认策略：
     * 在总时长的前半段内完成升压，但最长不超过 60 秒，避免长任务一直卡在升压阶段。
     */
    private int computeRampWindowSeconds(int durationSeconds) {
        if (durationSeconds <= 1) {
            return 1;
        }
        return Math.max(1, Math.min(MAX_RAMP_WINDOW_SECONDS, durationSeconds / 2));
    }

    /**
     * 阶梯加压的默认策略：
     * 把目标并发拆成最多 5 个台阶，在 rampWindow 内逐级抬高负载；
     * 每一级线程组都只负责“新增的那一批用户”，并持续跑到整轮压测结束。
     */
    private List<ThreadGroupSpec> buildStairThreadGroups(String sceneName, int concurrency,
                                                         int durationSeconds, int rampWindowSeconds) {
        int stepCount = Math.max(1, Math.min(Math.min(DEFAULT_STAIR_STEPS, concurrency), rampWindowSeconds));
        int baseThreads = concurrency / stepCount;
        int remainderThreads = concurrency % stepCount;
        List<ThreadGroupSpec> specs = new ArrayList<>(stepCount);

        for (int stepIndex = 0; stepIndex < stepCount; stepIndex++) {
            int threadsForStep = baseThreads + (stepIndex < remainderThreads ? 1 : 0);
            int delaySeconds = Math.min(durationSeconds - 1,
                    (int) Math.floor((double) stepIndex * rampWindowSeconds / stepCount));
            int activeDurationSeconds = Math.max(1, durationSeconds - delaySeconds);
            specs.add(new ThreadGroupSpec(
                    sceneName + "-step-" + (stepIndex + 1),
                    threadsForStep,
                    1,
                    delaySeconds,
                    activeDurationSeconds
            ));
        }

        return specs;
    }

    private ThreadGroup createThreadGroup(ThreadGroupSpec spec) {
        ThreadGroup threadGroup = new ThreadGroup();
        threadGroup.setName(spec.name());
        threadGroup.setNumThreads(spec.threads());
        threadGroup.setRampUp(spec.rampUpSeconds());
        threadGroup.setScheduler(true);
        threadGroup.setDuration(spec.durationSeconds());
        threadGroup.setDelay(spec.delaySeconds());
        threadGroup.setSamplerController(createLoopController());
        return threadGroup;
    }

    private ConfigTestElement createHttpDefaults(String protocol, String domain, int port) {
        ConfigTestElement httpDefaults = new ConfigTestElement();
        httpDefaults.setName("HTTP Request Defaults");
        httpDefaults.setProperty(TestElement.TEST_CLASS, ConfigTestElement.class.getName());
        httpDefaults.setProperty(TestElement.GUI_CLASS, HttpDefaultsGui.class.getName());
        httpDefaults.setProperty(HTTPSamplerBase.PROTOCOL, protocol);
        httpDefaults.setProperty(HTTPSamplerBase.DOMAIN, domain);
        httpDefaults.setProperty(HTTPSamplerBase.PORT, String.valueOf(port));
        httpDefaults.setProperty(HTTPSamplerBase.CONTENT_ENCODING, "UTF-8");
        return httpDefaults;
    }

    /*
     * 创建http采样器
     * */
    @NotNull
    private static HTTPSamplerProxy createHTTPSampler(TestSceneStep step) {
        HTTPSamplerProxy httpSampler = new HTTPSamplerProxy();
        httpSampler.setName(step.getName());
        httpSampler.setProperty(TestElement.TEST_CLASS, HTTPSamplerProxy.class.getName());
        httpSampler.setProperty(TestElement.GUI_CLASS, HttpTestSampleGui.class.getName());
        httpSampler.setPath(step.getPath());
        httpSampler.setMethod(step.getMethod());
        httpSampler.setFollowRedirects(true);
        httpSampler.setUseKeepAlive(true);
        return httpSampler;
    }

    /**
     * 辅助方法：创建循环控制器（ThreadGroup 的子控制器）
     * 作用：控制每个线程的循环次数
     */
    private LoopController createLoopController() {
        LoopController loopController = new LoopController();
        loopController.setName("循环控制器");
        loopController.setLoops(-1);
        loopController.setContinueForever(true);
        loopController.setProperty(TestElement.TEST_CLASS, LoopController.class.getName());
        loopController.setProperty(TestElement.GUI_CLASS, LoopControlPanel.class.getName());
        loopController.initialize();
        return loopController;
    }

    private record ThreadGroupSpec(
            String name,
            int threads,
            int rampUpSeconds,
            int delaySeconds,
            int durationSeconds
    ) {
    }
}
