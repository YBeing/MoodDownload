package com.mooddownload.local.service.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooddownload.local.common.enums.ErrorCode;
import com.mooddownload.local.common.exception.BizException;
import com.mooddownload.local.dal.provider.ExternalProviderSessionRepository;
import com.mooddownload.local.mapper.provider.ExternalProviderSessionDO;
import com.mooddownload.local.service.provider.model.BaiduPanPreflightResultModel;
import com.mooddownload.local.service.provider.model.BaiduPanResolveResultModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 百度网盘预研 Provider 服务。
 */
@Service
public class BaiduPanProviderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaiduPanProviderService.class);

    private static final String PROVIDER_CODE = "BAIDUPAN";

    private static final TypeReference<LinkedHashMap<String, Object>> JSON_MAP_TYPE =
        new TypeReference<LinkedHashMap<String, Object>>() { };

    private final ExternalProviderSessionRepository externalProviderSessionRepository;

    private final ObjectMapper objectMapper;

    public BaiduPanProviderService(
        ExternalProviderSessionRepository externalProviderSessionRepository,
        ObjectMapper objectMapper
    ) {
        this.externalProviderSessionRepository = externalProviderSessionRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 预检百度网盘接入能力。
     *
     * @param shareUrl 分享链接
     * @param authContext 鉴权上下文
     * @return 预检结果
     */
    @Transactional(rollbackFor = Exception.class)
    public BaiduPanPreflightResultModel preflight(String shareUrl, String authContext) {
        if (!StringUtils.hasText(shareUrl) && !StringUtils.hasText(authContext)) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "shareUrl 或 authContext 至少提供一个");
        }
        String normalizedShareUrl = normalizeBlank(shareUrl);
        String normalizedAuthContext = normalizeBlank(authContext);

        List<String> riskFlags = new ArrayList<String>();
        riskFlags.add("PRE_RESEARCH_ONLY");
        riskFlags.add("COMPLIANCE_REVIEW_REQUIRED");
        riskFlags.add("SIGNATURE_VOLATILE");

        String capability;
        String suggestedNextStep;
        if (isBaiduPanShareLink(normalizedShareUrl)) {
            capability = "SHARE_LINK_PRE_RESEARCH";
            riskFlags.add("SHARE_LINK_NEEDS_BROWSER_ASSIST");
            suggestedNextStep = "建议先通过浏览器辅助采集分享页上下文，再评估可下载性";
        } else if (StringUtils.hasText(normalizedAuthContext)) {
            capability = "AUTH_CONTEXT_PRE_RESEARCH";
            riskFlags.add("AUTH_CONTEXT_EXPIRES_FAST");
            suggestedNextStep = "建议短期会话内完成解析验证，不承诺稳定复用";
        } else {
            capability = "UNSUPPORTED_INPUT_PRE_RESEARCH";
            riskFlags.add("INPUT_NOT_RECOGNIZED");
            suggestedNextStep = "当前仅建议对标准分享链接或浏览器鉴权上下文做预研";
        }

        persistSession(normalizedShareUrl, normalizedAuthContext, riskFlags, "PRECHECKED");
        BaiduPanPreflightResultModel resultModel = new BaiduPanPreflightResultModel();
        resultModel.setCapability(capability);
        resultModel.setRiskFlags(riskFlags);
        resultModel.setSuggestedNextStep(suggestedNextStep);
        LOGGER.info("百度网盘预检完成: capability={}, riskSize={}", capability, riskFlags.size());
        return resultModel;
    }

    /**
     * 解析百度网盘预研上下文。
     *
     * @param providerContext Provider 上下文
     * @return 解析结果
     */
    @Transactional(rollbackFor = Exception.class)
    public BaiduPanResolveResultModel resolve(String providerContext) {
        if (!StringUtils.hasText(providerContext)) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "providerContext 不能为空");
        }
        String normalizedContext = providerContext.trim();
        String resolvedMode;
        String nextStep;
        if (isJsonObject(normalizedContext)) {
            Map<String, Object> contextMap = readJsonMap(normalizedContext);
            String shareUrl = asText(contextMap.get("shareUrl"));
            String authContext = asText(contextMap.get("authContext"));
            if (isBaiduPanShareLink(shareUrl) && StringUtils.hasText(authContext)) {
                resolvedMode = "BROWSER_ASSISTED_SHARE_LINK";
                nextStep = "建议优先走浏览器辅助解析链路，不承诺直链稳定性";
            } else if (isBaiduPanShareLink(shareUrl)) {
                resolvedMode = "SHARE_LINK_ANALYSIS_ONLY";
                nextStep = "当前仅建议识别分享页类型与风险，不直接生成下载任务";
            } else if (StringUtils.hasText(authContext)) {
                resolvedMode = "AUTH_CONTEXT_ANALYSIS_ONLY";
                nextStep = "当前仅建议校验鉴权上下文有效期与可复用性";
            } else {
                resolvedMode = "PRE_RESEARCH_ONLY";
                nextStep = "当前上下文不足，需补充分享链接或鉴权信息";
            }
        } else if (isBaiduPanShareLink(normalizedContext)) {
            resolvedMode = "SHARE_LINK_ANALYSIS_ONLY";
            nextStep = "建议先补充浏览器侧上下文，再决定是否继续解析";
        } else {
            resolvedMode = "PRE_RESEARCH_ONLY";
            nextStep = "未识别的 providerContext，当前仅保留预研结论";
        }
        BaiduPanResolveResultModel resultModel = new BaiduPanResolveResultModel();
        resultModel.setResolvedMode(resolvedMode);
        resultModel.setNextStep(nextStep);
        LOGGER.info("百度网盘预研解析完成: resolvedMode={}", resolvedMode);
        return resultModel;
    }

    private void persistSession(
        String shareUrl,
        String authContext,
        List<String> riskFlags,
        String sessionStatus
    ) {
        long now = System.currentTimeMillis();
        ExternalProviderSessionDO sessionDO = new ExternalProviderSessionDO();
        sessionDO.setProviderCode(PROVIDER_CODE);
        sessionDO.setSessionKey(UUID.randomUUID().toString());
        sessionDO.setSessionStatus(sessionStatus);
        sessionDO.setAuthContextJson(writeContextJson(shareUrl, authContext));
        sessionDO.setRiskFlagsJson(writeRiskFlagsJson(riskFlags));
        sessionDO.setExpiresAt(now + 10 * 60 * 1000L);
        sessionDO.setCreatedAt(now);
        sessionDO.setUpdatedAt(now);
        externalProviderSessionRepository.saveOrUpdate(sessionDO);
    }

    private boolean isBaiduPanShareLink(String shareUrl) {
        if (!StringUtils.hasText(shareUrl)) {
            return false;
        }
        String normalized = shareUrl.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("pan.baidu.com/s/")
            || normalized.contains("pan.baidu.com/share/init")
            || normalized.contains("yun.baidu.com/s/");
    }

    private String writeContextJson(String shareUrl, String authContext) {
        Map<String, Object> contextMap = new LinkedHashMap<String, Object>();
        contextMap.put("shareUrl", shareUrl);
        contextMap.put("authContext", authContext);
        try {
            return objectMapper.writeValueAsString(contextMap);
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "序列化 provider 上下文失败");
        }
    }

    private String writeRiskFlagsJson(List<String> riskFlags) {
        try {
            return objectMapper.writeValueAsString(riskFlags);
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "序列化 provider 风险标签失败");
        }
    }

    private boolean isJsonObject(String providerContext) {
        return providerContext.startsWith("{") && providerContext.endsWith("}");
    }

    private Map<String, Object> readJsonMap(String providerContext) {
        try {
            return objectMapper.readValue(providerContext, JSON_MAP_TYPE);
        } catch (IOException exception) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "providerContext 必须是合法 JSON");
        }
    }

    private String asText(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private String normalizeBlank(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
