package com.mooddownload.local.common.security;

import com.mooddownload.local.common.constant.HeaderConstants;
import com.mooddownload.local.common.constant.RequestContextConstants;
import java.io.IOException;
import java.util.UUID;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 请求追踪过滤器。
 */
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    /**
     * 注入 requestId 与 clientType，便于统一日志与错误响应追踪。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException IO 异常
     */
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = request.getHeader(HeaderConstants.REQUEST_ID);
        if (!StringUtils.hasText(requestId)) {
            requestId = generateRequestId();
        }

        String clientType = request.getHeader(HeaderConstants.CLIENT_TYPE);
        if (!StringUtils.hasText(clientType)) {
            clientType = "unknown";
        }

        RequestContext.set(requestId, clientType);
        MDC.put(RequestContextConstants.REQUEST_ID, requestId);
        MDC.put(RequestContextConstants.CLIENT_TYPE, clientType);
        response.setHeader(HeaderConstants.REQUEST_ID, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(RequestContextConstants.REQUEST_ID);
            MDC.remove(RequestContextConstants.CLIENT_TYPE);
            RequestContext.clear();
        }
    }

    private String generateRequestId() {
        return "req-" + UUID.randomUUID().toString().replace("-", "");
    }
}

