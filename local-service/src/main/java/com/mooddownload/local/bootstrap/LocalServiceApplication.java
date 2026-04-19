package com.mooddownload.local.bootstrap;

import com.mooddownload.local.client.aria2.Aria2Properties;
import com.mooddownload.local.common.security.LocalSecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 本地服务启动入口。
 *
 * <p>当前阶段仅负责初始化 Spring Boot 工程骨架与基础设施。</p>
 */
@SpringBootApplication(scanBasePackages = "com.mooddownload.local")
@EnableConfigurationProperties({LocalSecurityProperties.class, Aria2Properties.class})
@EnableScheduling
public class LocalServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LocalServiceApplication.class, args);
    }
}
