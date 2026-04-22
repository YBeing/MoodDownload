package com.mooddownload.local.service.capture;

import com.mooddownload.local.common.constant.HeaderConstants;
import com.mooddownload.local.common.enums.ErrorCode;
import com.mooddownload.local.common.exception.BizException;
import com.mooddownload.local.dal.entry.ExternalEntryLogRepository;
import com.mooddownload.local.dal.profile.SourceSiteRuleRepository;
import com.mooddownload.local.mapper.entry.ExternalEntryLogDO;
import com.mooddownload.local.mapper.profile.SourceSiteRuleDO;
import com.mooddownload.local.service.capture.convert.CaptureTaskConverter;
import com.mooddownload.local.service.capture.model.CaptureTaskResult;
import com.mooddownload.local.service.capture.model.ExtensionCaptureCommand;
import com.mooddownload.local.service.config.ConfigService;
import com.mooddownload.local.service.task.TaskWorkflowService;
import com.mooddownload.local.service.task.model.CreateTaskCommand;
import com.mooddownload.local.service.task.model.TaskOperationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 浏览器扩展接管服务。
 */
@Service
public class ExtensionCaptureService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionCaptureService.class);

    private static final Set<String> SUPPORTED_BROWSERS = new HashSet<String>(Arrays.asList("chrome", "edge"));

    private static final Set<String> SUPPORTED_CLIENT_TYPES = new HashSet<String>(Arrays.asList(
        HeaderConstants.CLIENT_TYPE_NATIVE_HOST,
        HeaderConstants.CLIENT_TYPE_BROWSER_EXTENSION
    ));

    private final ConfigService configService;

    private final TaskWorkflowService taskWorkflowService;

    private final CaptureTaskConverter captureTaskConverter;

    private final SourceSiteRuleRepository sourceSiteRuleRepository;

    private final ExternalEntryLogRepository externalEntryLogRepository;

    private final ObjectMapper objectMapper;

    public ExtensionCaptureService(
        ConfigService configService,
        TaskWorkflowService taskWorkflowService,
        CaptureTaskConverter captureTaskConverter,
        SourceSiteRuleRepository sourceSiteRuleRepository,
        ExternalEntryLogRepository externalEntryLogRepository,
        ObjectMapper objectMapper
    ) {
        this.configService = configService;
        this.taskWorkflowService = taskWorkflowService;
        this.captureTaskConverter = captureTaskConverter;
        this.sourceSiteRuleRepository = sourceSiteRuleRepository;
        this.externalEntryLogRepository = externalEntryLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 承接扩展转发的下载请求，并复用任务域创建下载任务。
     *
     * @param command 扩展接管命令
     * @return 接管结果
     */
    public CaptureTaskResult capture(ExtensionCaptureCommand command) {
        validateCommand(command);
        configService.ensureBrowserCaptureEnabled();
        String resolvedSourceType = captureTaskConverter.resolveSourceTypeCode(command.getDownloadUrl());
        SourceSiteRuleDO matchedRule = matchRule(command, resolvedSourceType);
        validateMatchedRuleRequirement(command, matchedRule);
        try {
            CreateTaskCommand createTaskCommand = captureTaskConverter.toCreateTaskCommand(
                command,
                configService.resolveSaveDir(null)
            );
            enrichTaskCommand(command, matchedRule, createTaskCommand);
            TaskOperationResult taskOperationResult = taskWorkflowService.createTask(createTaskCommand);
            persistEntryLog(command, resolvedSourceType, matchedRule, "ACCEPTED", buildRemark(matchedRule));
            LOGGER.info("处理扩展接管请求成功: clientRequestId={}, browser={}, taskId={}, tabUrl={}, matchedRuleId={}",
                command.getClientRequestId(),
                command.getBrowser(),
                taskOperationResult.getTaskModel().getId(),
                command.getTabUrl(),
                matchedRule == null ? null : matchedRule.getId());
            CaptureTaskResult captureTaskResult = captureTaskConverter.toCaptureTaskResult(taskOperationResult);
            captureTaskResult.setResolvedSourceType(resolvedSourceType);
            captureTaskResult.setSiteRuleMatched(matchedRule != null);
            return captureTaskResult;
        } catch (RuntimeException exception) {
            persistEntryLog(command, resolvedSourceType, matchedRule, "FAILED", exception.getMessage());
            throw exception;
        }
    }

    private void validateCommand(ExtensionCaptureCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "扩展接管命令不能为空");
        }
        if (!isSupportedClientType(command.getClientType())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "仅允许 native-host 或 browser-extension 调用扩展接管接口");
        }
        if (!StringUtils.hasText(command.getClientRequestId())) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "clientRequestId 不能为空");
        }
        String browser = normalizeBrowser(command.getBrowser());
        if (!SUPPORTED_BROWSERS.contains(browser)) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "不支持的浏览器类型: " + command.getBrowser());
        }
        if (!StringUtils.hasText(command.getDownloadUrl())) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "downloadUrl 不能为空");
        }
        command.setBrowser(browser);
    }

    /**
     * 判断当前调用方是否属于允许访问扩展接管接口的白名单来源。
     *
     * @param clientType 调用方类型
     * @return 是否允许访问
     */
    private boolean isSupportedClientType(String clientType) {
        return StringUtils.hasText(clientType)
            && SUPPORTED_CLIENT_TYPES.contains(clientType.trim().toLowerCase(Locale.ROOT));
    }

    private String normalizeBrowser(String browser) {
        return StringUtils.hasText(browser) ? browser.trim().toLowerCase(Locale.ROOT) : null;
    }

    private SourceSiteRuleDO matchRule(ExtensionCaptureCommand command, String resolvedSourceType) {
        String host = resolveHost(command);
        if (!StringUtils.hasText(host)) {
            return null;
        }
        List<SourceSiteRuleDO> rules = sourceSiteRuleRepository.findAll();
        for (SourceSiteRuleDO ruleDO : rules) {
            if (ruleDO == null || defaultInt(ruleDO.getEnabled(), 1) != 1) {
                continue;
            }
            if (StringUtils.hasText(ruleDO.getBrowserCode())
                && !ruleDO.getBrowserCode().trim().equalsIgnoreCase(command.getBrowser())) {
                continue;
            }
            if (StringUtils.hasText(ruleDO.getSourceType())
                && !ruleDO.getSourceType().trim().equalsIgnoreCase(resolvedSourceType)) {
                continue;
            }
            if (matchesHostPattern(host, ruleDO.getHostPattern())) {
                return ruleDO;
            }
        }
        return null;
    }

    private void persistEntryLog(
        ExtensionCaptureCommand command,
        String resolvedSourceType,
        SourceSiteRuleDO matchedRule,
        String resultStatus,
        String remark
    ) {
        ExternalEntryLogDO entryLogDO = new ExternalEntryLogDO();
        entryLogDO.setClientRequestId(command.getClientRequestId());
        entryLogDO.setEntryType("BROWSER_EXTENSION");
        entryLogDO.setBrowserCode(command.getBrowser());
        entryLogDO.setSourceType(resolvedSourceType);
        entryLogDO.setTabUrl(command.getTabUrl());
        entryLogDO.setSourceUri(command.getDownloadUrl());
        entryLogDO.setMatchedRuleId(matchedRule == null ? null : matchedRule.getId());
        entryLogDO.setResultStatus(resultStatus);
        entryLogDO.setRemark(StringUtils.hasText(remark) ? remark : null);
        entryLogDO.setCreatedAt(System.currentTimeMillis());
        externalEntryLogRepository.insert(entryLogDO);
    }

    private String buildRemark(SourceSiteRuleDO matchedRule) {
        if (matchedRule == null) {
            return "未命中站点规则";
        }
        return "命中站点规则: " + matchedRule.getId();
    }

    private String resolveHost(ExtensionCaptureCommand command) {
        String candidate = StringUtils.hasText(command.getTabUrl()) ? command.getTabUrl() : command.getDownloadUrl();
        if (!StringUtils.hasText(candidate)) {
            return null;
        }
        try {
            URI uri = new URI(candidate.trim());
            return uri.getHost();
        } catch (URISyntaxException exception) {
            LOGGER.warn("解析扩展接管来源 host 失败: url={}", candidate, exception);
            return null;
        }
    }

    private boolean matchesHostPattern(String host, String hostPattern) {
        if (!StringUtils.hasText(host) || !StringUtils.hasText(hostPattern)) {
            return false;
        }
        String normalizedPattern = hostPattern.trim()
            .replace(".", "\\.")
            .replace("*", ".*");
        return host.toLowerCase(Locale.ROOT).matches(normalizedPattern.toLowerCase(Locale.ROOT));
    }

    private int defaultInt(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private void validateMatchedRuleRequirement(ExtensionCaptureCommand command, SourceSiteRuleDO matchedRule) {
        if (matchedRule == null) {
            return;
        }
        if (defaultInt(matchedRule.getRequireHeaderSnapshot(), 0) == 1
            && !StringUtils.hasText(command.getHeaderSnapshotJson())) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "当前站点要求提供请求头快照");
        }
    }

    private void enrichTaskCommand(
        ExtensionCaptureCommand command,
        SourceSiteRuleDO matchedRule,
        CreateTaskCommand createTaskCommand
    ) {
        createTaskCommand.setEntryType("BROWSER_EXTENSION");
        createTaskCommand.setSourceProvider("GENERIC");
        createTaskCommand.setSourceSiteHost(resolveHost(command));
        createTaskCommand.setEntryContextJson(buildEntryContextJson(command, matchedRule));
        if (matchedRule != null && StringUtils.hasText(matchedRule.getProfileCode())) {
            createTaskCommand.setEngineProfileCode(matchedRule.getProfileCode().trim());
        }
    }

    private String buildEntryContextJson(ExtensionCaptureCommand command, SourceSiteRuleDO matchedRule) {
        java.util.Map<String, Object> contextMap = new java.util.LinkedHashMap<String, Object>();
        contextMap.put("browser", command.getBrowser());
        contextMap.put("tabUrl", command.getTabUrl());
        contextMap.put("downloadUrl", command.getDownloadUrl());
        contextMap.put("referer", command.getReferer());
        contextMap.put("userAgent", command.getUserAgent());
        contextMap.put("headerSnapshotJson", command.getHeaderSnapshotJson());
        contextMap.put("matchedRuleId", matchedRule == null ? null : matchedRule.getId());
        try {
            return objectMapper.writeValueAsString(contextMap);
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "序列化扩展接管上下文失败");
        }
    }
}
