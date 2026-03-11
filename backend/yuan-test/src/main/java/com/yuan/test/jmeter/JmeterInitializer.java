package com.yuan.test.jmeter;


import org.apache.jmeter.util.JMeterUtils;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;

@Component
public class JmeterInitializer {

    private final JmeterConfig jmeterConfig;

    public JmeterInitializer(JmeterConfig jmeterConfig) {
        this.jmeterConfig = jmeterConfig;
    }

    public void initializeJmeterEngine() {
        // 加载 JMeter 配置文件（jmeter.properties），这里用默认配置即可
        // 注意：如果不初始化，会报 "JMeter properties not found" 错误
        String jmeterHome = jmeterConfig.getHome(); // 可以放一个空的 jmeter.properties 在这里，或者用 JMeterUtils.setJMeterHome()
        JMeterUtils.loadJMeterProperties(Paths.get(jmeterHome, "bin/jmeter.properties").toString());
        JMeterUtils.initLocale(); // 初始化本地化（避免中文乱码）
    }

}
