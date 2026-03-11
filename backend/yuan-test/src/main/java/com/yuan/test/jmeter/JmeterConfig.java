package com.yuan.test.jmeter;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "jmeter")
public class JmeterConfig {

    private String home;

    private String scriptsDir;

    private String resultsDir;
}
