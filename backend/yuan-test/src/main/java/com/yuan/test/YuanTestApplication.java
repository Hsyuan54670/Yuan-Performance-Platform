package com.yuan.test;

import com.yuan.test.jmeter.JmeterConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(JmeterConfig.class)
public class YuanTestApplication {
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(YuanTestApplication.class, args);
    }
}