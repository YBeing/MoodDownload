package com.mooddownload.local.service.provider.model;

/**
 * 百度网盘预研解析结果模型。
 */
public class BaiduPanResolveResultModel {

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
