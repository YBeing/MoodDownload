package com.mooddownload.local.common.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 扫描配置。
 */
@Configuration
@MapperScan(basePackages = "com.mooddownload.local.mapper")
public class MybatisConfiguration {
}
