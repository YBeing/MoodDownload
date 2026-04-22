package com.mooddownload.local.controller.provider.convert;

import com.mooddownload.local.controller.provider.vo.BaiduPanPreflightResponse;
import com.mooddownload.local.controller.provider.vo.BaiduPanResolveResponse;
import com.mooddownload.local.service.provider.model.BaiduPanPreflightResultModel;
import com.mooddownload.local.service.provider.model.BaiduPanResolveResultModel;
import org.springframework.stereotype.Component;

/**
 * 百度网盘 Provider 接口模型转换器。
 */
@Component
public class BaiduPanProviderControllerConverter {

    /**
     * 将预检结果转换为接口响应。
     *
     * @param resultModel 预检结果
     * @return 接口响应
     */
    public BaiduPanPreflightResponse toPreflightResponse(BaiduPanPreflightResultModel resultModel) {
        BaiduPanPreflightResponse response = new BaiduPanPreflightResponse();
        response.setCapability(resultModel.getCapability());
        response.setRiskFlags(resultModel.getRiskFlags());
        response.setSuggestedNextStep(resultModel.getSuggestedNextStep());
        return response;
    }

    /**
     * 将解析结果转换为接口响应。
     *
     * @param resultModel 解析结果
     * @return 接口响应
     */
    public BaiduPanResolveResponse toResolveResponse(BaiduPanResolveResultModel resultModel) {
        BaiduPanResolveResponse response = new BaiduPanResolveResponse();
        response.setResolvedMode(resultModel.getResolvedMode());
        response.setNextStep(resultModel.getNextStep());
        return response;
    }
}
