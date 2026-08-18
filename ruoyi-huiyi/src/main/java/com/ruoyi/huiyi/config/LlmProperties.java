package com.ruoyi.huiyi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("huiyi.llm")
public class LlmProperties {
    private String apiUrl;
    private String model;
    private int connectTimeout = 600000;
    private int socketTimeout = 600000;
}
