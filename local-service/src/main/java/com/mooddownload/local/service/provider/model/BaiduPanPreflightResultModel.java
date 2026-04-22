package com.mooddownload.local.service.provider.model;

import java.util.List;

/**
 * 百度网盘预检结果模型。
 */
public class BaiduPanPreflightResultModel {

    /** 能力标识 */
    private String capability;

    /** 风险标签 */
    private List<String> riskFlags;

    /** 建议下一步 */
    private String suggestedNextStep;

    public String getCapability() {
        return capability;
    }

    public void setCapability(String capability) {
        this.capability = capability;
    }

    public List<String> getRiskFlags() {
        return riskFlags;
    }

    public void setRiskFlags(List<String> riskFlags) {
        this.riskFlags = riskFlags;
    }

    public String getSuggestedNextStep() {
        return suggestedNextStep;
    }

    public void setSuggestedNextStep(String suggestedNextStep) {
        this.suggestedNextStep = suggestedNextStep;
    }
}
