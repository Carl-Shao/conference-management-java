package com.ruoyi.huiyi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("huiyi.llm")
public class LlmProperties {
    private String url;
    private String model;
    private int connectTimeout = 5000;
    private int socketTimeout = 60000;
}
