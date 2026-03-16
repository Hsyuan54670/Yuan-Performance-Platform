package com.yuan.analysis;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.yuan.api")
public class YuanAnalysisApplication {
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(YuanAnalysisApplication.class, args);
    }
}
