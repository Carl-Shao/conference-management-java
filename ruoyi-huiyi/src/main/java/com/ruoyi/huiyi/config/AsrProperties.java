package com.ruoyi.huiyi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "huiyi.asr")
public class AsrProperties {
    private String apiUrl;
    private String model;
    private int connectTimeout = 600000;
    private int socketTimeout = 600000;
}
