package com.mooddownload.local.common.config;

import com.mooddownload.local.client.aria2.Aria2Properties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 基础配置。
 */
@Configuration
public class RestTemplateConfiguration {

    /**
     * 构建 aria2 RPC 专用 RestTemplate。
     *
     * @param restTemplateBuilder Spring RestTemplate 构建器
     * @param aria2Properties aria2 配置
     * @return RestTemplate
     */
    @Bean(name = "aria2RestTemplate")
    public RestTemplate aria2RestTemplate(
        RestTemplateBuilder restTemplateBuilder,
        Aria2Properties aria2Properties
    ) {
        return restTemplateBuilder
            .setConnectTimeout(java.time.Duration.ofMillis(aria2Properties.getConnectTimeoutMillis()))
            .setReadTimeout(java.time.Duration.ofMillis(aria2Properties.getReadTimeoutMillis()))
            .build();
    }
}
