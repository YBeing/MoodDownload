package com.mooddownload.local.common.security;

import com.mooddownload.local.common.constant.HeaderConstants;
import com.mooddownload.local.common.enums.ErrorCode;
import com.mooddownload.local.common.exception.BizException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 本地接口访问令牌拦截器。
 */
@Component
public class LocalTokenInterceptor implements HandlerInterceptor {

    private final LocalSecurityProperties localSecurityProperties;

    public LocalTokenInterceptor(LocalSecurityProperties localSecurityProperties) {
        this.localSecurityProperties = localSecurityProperties;
    }

    /**
     * 校验本地访问令牌，避免接口暴露给未授权调用方。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param handler 目标处理器
     * @return 是否继续执行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!localSecurityProperties.isEnabled()) {
            return true;
        }

        // 浏览器 CORS 预检请求不会携带业务令牌，由 MVC CORS 配置统一返回允许头。
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }

        String token = request.getHeader(localSecurityProperties.getTokenHeaderName());
        if (!StringUtils.hasText(token) || !token.equals(localSecurityProperties.getToken())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "缺少或错误的本地访问令牌");
        }

        String clientType = request.getHeader(HeaderConstants.CLIENT_TYPE);
        if (!StringUtils.hasText(clientType)) {
            clientType = localSecurityProperties.getTrustedClientType();
        }
        RequestContext.set(RequestContext.getRequestId(), clientType);
        return true;
    }
}
