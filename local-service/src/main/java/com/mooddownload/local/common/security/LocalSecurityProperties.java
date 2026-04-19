package com.mooddownload.local.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 本地服务安全配置。
 */
@ConfigurationProperties(prefix = "mooddownload.security")
public class LocalSecurityProperties {

    private boolean enabled = true;

    private String token = "change-me-in-prod";

    private String tokenHeaderName = "X-Local-Token";

    private String trustedClientType = "desktop-app";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenHeaderName() {
        return tokenHeaderName;
    }

    public void setTokenHeaderName(String tokenHeaderName) {
        this.tokenHeaderName = tokenHeaderName;
    }

    public String getTrustedClientType() {
        return trustedClientType;
    }

    public void setTrustedClientType(String trustedClientType) {
        this.trustedClientType = trustedClientType;
    }
}

