package com.yuan.test.jmeter;

import com.yuan.test.entity.TestScene;
import com.yuan.test.entity.TestSceneStep;
import com.yuan.test.entity.TestTask;
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
import org.apache.jmeter.reporters.Summariser;
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
    public ListedHashTree build(com.yuan.test.entity.TestPlan plan , TestScene scene , TestTask task, List<TestSceneStep> steps, ResultCollector resultCollector) throws Exception {
        // ====================== 0. 初始化 JMeter 环境（必须第一步做！） ======================
        jmeterInitializer.initializeJmeterEngine();

        // ====================== 1. 创建测试计划（TestPlan）：根节点 ======================
        TestPlan testPlan = new TestPlan(plan.getName());
        testPlan.setFunctionalMode(false); // 关闭功能测试模式（压测用false，功能测试用true）
        testPlan.setTearDownOnShutdown(true); // 测试结束后执行清理
        testPlan.setUserDefinedVariables(new Arguments()); // 初始化用户定义变量（可选，这里留空）

        // ====================== 2. 创建线程组（ThreadGroup）：模拟用户 ======================
        ThreadGroup threadGroup = new ThreadGroup();
        threadGroup.setName(scene.getName());
        threadGroup.setNumThreads(plan.getConcurrency()); // 并发数：10个线程（模拟10个用户）
        // TODO 根据rampType设置RampUp
        threadGroup.setRampUp(5); // 升压时间：5秒内启动所有10个线程（每秒启动2个）
        threadGroup.setScheduler(true); // 开启调度器（用于设置压测时长）
        threadGroup.setDuration(plan.getDuration()); // 压测时长：60秒（1分钟）
        threadGroup.setDelay(0); // 启动延迟：0秒（立即开始）
        threadGroup.setSamplerController(createLoopController()); // 设置循环控制器（线程组的核心子控制器）

        // ====================== 3. 创建 HTTP 请求默认值（可选但推荐：统一配置域名、协议等） ======================
        // 作用：避免每个 HTTP 采样器重复写 protocol、domain、编码等，统一管理
        URL tagetUrl = new URL(plan.getTargetUrl());
        String protocol = tagetUrl.getProtocol();
        String domain = tagetUrl.getHost();
        int port = tagetUrl.getPort();
        if(port == -1) {
            port = tagetUrl.getDefaultPort();
        }

        ConfigTestElement httpDefaults = new ConfigTestElement();
        httpDefaults.setName("HTTP Request Defaults");
        httpDefaults.setProperty(TestElement.TEST_CLASS, ConfigTestElement.class.getName());
        httpDefaults.setProperty(TestElement.GUI_CLASS, HttpDefaultsGui.class.getName());
        httpDefaults.setProperty(HTTPSamplerBase.PROTOCOL, protocol);
        httpDefaults.setProperty(HTTPSamplerBase.DOMAIN, domain);
        httpDefaults.setProperty(HTTPSamplerBase.PORT, String.valueOf(port));
        httpDefaults.setProperty(HTTPSamplerBase.CONTENT_ENCODING, "UTF-8");

        // ====================== 4. 创建 HTTP 请求采样器（核心：发送请求） ======================
        List<HTTPSamplerProxy> httpSamplerList = new ArrayList<HTTPSamplerProxy>();
        // TODO step.weight权重未处理
        for(TestSceneStep step : steps){
            HTTPSamplerProxy httpSampler = createHTTPSampler(step);
            httpSamplerList.add(httpSampler);
        }


        // ====================== 5. 创建监听器（Listener：收集结果） ======================
        resultCollector.setName("汇总报告");
        resultCollector.setFilename(Paths.get(jmeterConfig.getResultsDir(), "task_"+task.getId()+".jtl").toString());

        // ====================== 6. 组装 HashTree（核心：构建层级结构） ======================
        // JMeter 的执行是基于 HashTree 的层级结构，顺序为：TestPlan → ThreadGroup → 子元素
        ListedHashTree testPlanTree = new ListedHashTree();

        // 1. 先把 TestPlan 加到根节点
        testPlanTree.add(testPlan);

        // 2. 把 ThreadGroup 加到 TestPlan 的子节点
        ListedHashTree threadGroupTree = testPlanTree.getTree(testPlan);
        threadGroupTree.add(threadGroup);

        // 3. 把 HTTP默认值、HTTP采样器、监听器 加到 ThreadGroup 的子节点
        ListedHashTree samplerTree = threadGroupTree.getTree(threadGroup);
        samplerTree.add(httpDefaults); // 先加默认值（采样器会继承默认值的配置）
        // 再加采样器
        for(HTTPSamplerProxy httpSampler : httpSamplerList){
            samplerTree.add(httpSampler);
        }
        samplerTree.add(resultCollector); // 最后加监听器

        return testPlanTree;
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
        httpSampler.setPath(step.getPath()); // 请求路径：根路径（因为默认值里已经配了域名，这里只写路径）
        httpSampler.setMethod(step.getMethod()); // 请求方法：GET
        httpSampler.setFollowRedirects(true); // 跟随重定向：是
        httpSampler.setUseKeepAlive(true); // 保持连接：是（提升性能）
        return httpSampler;
    }

    /**
     * 辅助方法：创建循环控制器（ThreadGroup 的子控制器）
     * 作用：控制每个线程的循环次数
     */
    private LoopController createLoopController() {
        LoopController loopController = new LoopController();
        loopController.setName("循环控制器");
        loopController.setLoops(-1); // 循环次数：-1 表示永久循环（由线程组的 duration 控制结束）
        loopController.setContinueForever(true); // 永久循环：是
        loopController.setProperty(TestElement.TEST_CLASS, LoopController.class.getName());
        loopController.setProperty(TestElement.GUI_CLASS, LoopControlPanel.class.getName());
        loopController.initialize(); // 必须初始化！
        return loopController;
    }

}
