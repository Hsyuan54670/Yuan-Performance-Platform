package com.yuan.test;

import com.yuan.test.jmeter.JmeterConfig;
import com.yuan.test.jmeter.JmeterTestPlanBuilder;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.save.SaveService;
import org.apache.jorphan.collections.HashTree;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileOutputStream;

@SpringBootTest
public class UnitTest {
    @Autowired
    JmeterConfig jmeterConfig;
    @Test
    public void testJmeterTestPlanBuild() throws Exception {
//        // 1. 构建 HashTree
//        JmeterTestPlanBuilder builder = new JmeterTestPlanBuilder(jmeterConfig);
//        HashTree testPlanTree = builder.build();
//
//        // 2. （可选）保存为 JMX 文件（方便用 JMeter GUI 打开查看）
//        SaveService.saveTree(testPlanTree, new FileOutputStream(jmeterConfig.getScriptsDir()+"generated_test_plan.jmx"));
//        System.out.println("JMX 文件已生成：generated_test_plan.jmx");
//
//        // 3. 运行压测
//        StandardJMeterEngine jmeterEngine = new StandardJMeterEngine();
//        jmeterEngine.configure(testPlanTree); // 配置 HashTree
//        jmeterEngine.run(); // 启动压测
//        System.out.println("压测完成！");
    }
}
