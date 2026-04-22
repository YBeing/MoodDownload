package com.mooddownload.local.controller.provider.vo;

/**
 * 百度网盘预研解析响应。
 */
public class BaiduPanResolveResponse {

    /** 解析模式 */
    private String resolvedMode;

    /** 下一步动作 */
    private String nextStep;

    public String getResolvedMode() {
        return resolvedMode;
    }

    public void setResolvedMode(String resolvedMode) {
        this.resolvedMode = resolvedMode;
    }

    public String getNextStep() {
        return nextStep;
    }

    public void setNextStep(String nextStep) {
        this.nextStep = nextStep;
    }
}
