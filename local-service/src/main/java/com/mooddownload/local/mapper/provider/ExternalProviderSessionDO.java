package com.mooddownload.local.mapper.provider;

/**
 * 外部 Provider 会话持久化对象。
 */
public class ExternalProviderSessionDO {

    /** 主键 ID */
    private Long id;

    /** Provider 编码 */
    private String providerCode;

    /** 会话键 */
    private String sessionKey;

    /** 会话状态 */
    private String sessionStatus;

    /** 鉴权上下文 JSON */
    private String authContextJson;

    /** 风险标签 JSON */
    private String riskFlagsJson;

    /** 过期时间 */
    private Long expiresAt;

    /** 创建时间 */
    private Long createdAt;

    /** 更新时间 */
    private Long updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public String getSessionStatus() {
        return sessionStatus;
    }

    public void setSessionStatus(String sessionStatus) {
        this.sessionStatus = sessionStatus;
    }

    public String getAuthContextJson() {
        return authContextJson;
    }

    public void setAuthContextJson(String authContextJson) {
        this.authContextJson = authContextJson;
    }

    public String getRiskFlagsJson() {
        return riskFlagsJson;
    }

    public void setRiskFlagsJson(String riskFlagsJson) {
        this.riskFlagsJson = riskFlagsJson;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
