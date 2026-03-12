package com.yuan.monitor;


import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
public class YuanMonitorApplication {
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(YuanMonitorApplication.class, args);
    }
}