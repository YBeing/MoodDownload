package com.mooddownload.local.common.config;

import com.mooddownload.local.common.constant.HeaderConstants;
import com.mooddownload.local.common.security.LocalTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 基础配置。
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final LocalTokenInterceptor localTokenInterceptor;

    public WebMvcConfiguration(LocalTokenInterceptor localTokenInterceptor) {
        this.localTokenInterceptor = localTokenInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localTokenInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns(
                "/actuator/health",
                "/actuator/health/**",
                "/actuator/info",
                "/actuator/info/**",
                "/error"
            );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 本地服务仅绑定 127.0.0.1，真实访问控制依赖本地令牌，因此允许跨源预检通过。
        registry.addMapping("/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders(HeaderConstants.REQUEST_ID)
            .allowCredentials(false)
            .maxAge(3600);
    }
}
