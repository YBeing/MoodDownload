package com.mooddownload.local.controller.capture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooddownload.local.bootstrap.LocalServiceApplication;
import com.mooddownload.local.client.aria2.Aria2RpcClient;
import com.mooddownload.local.common.constant.HeaderConstants;
import com.mooddownload.local.dal.config.DownloadConfigRepository;
import com.mooddownload.local.dal.entry.ExternalEntryLogRepository;
import com.mooddownload.local.dal.profile.SourceSiteRuleRepository;
import com.mooddownload.local.dal.task.DownloadTaskRepository;
import com.mooddownload.local.mapper.config.DownloadConfigDO;
import com.mooddownload.local.mapper.entry.ExternalEntryLogDO;
import com.mooddownload.local.mapper.profile.SourceSiteRuleDO;
import com.mooddownload.local.mapper.task.DownloadTaskDO;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * B6 阶段扩展接管接口集成测试。
 */
@SpringBootTest(classes = LocalServiceApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ExtensionCaptureControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DownloadTaskRepository downloadTaskRepository;

    @Autowired
    private DownloadConfigRepository downloadConfigRepository;

    @Autowired
    private ExternalEntryLogRepository externalEntryLogRepository;

    @Autowired
    private SourceSiteRuleRepository sourceSiteRuleRepository;

    @MockBean
    private Aria2RpcClient aria2RpcClient;

    @Test
    void shouldCaptureBrowserDownloadWhenClientIsNativeHost() throws Exception {
        when(aria2RpcClient.addUri(
            "https://example.com/files/demo.iso",
            "./downloads",
            null
        )).thenReturn("gid-extension-1");

        MvcResult firstResult = mockMvc.perform(post("/api/extension/capture")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(buildPayload("request-extension-1")))
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, HeaderConstants.CLIENT_TYPE_NATIVE_HOST))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.accepted").value(true))
            .andExpect(jsonPath("$.data.domainStatus").value("RUNNING"))
            .andExpect(jsonPath("$.data.resolvedSourceType").value("HTTPS"))
            .andExpect(jsonPath("$.data.siteRuleMatched").value(false))
            .andReturn();

        JsonNode firstBody = objectMapper.readTree(firstResult.getResponse().getContentAsByteArray());
        long firstTaskId = firstBody.path("data").path("taskId").asLong();

        MvcResult duplicateResult = mockMvc.perform(post("/api/extension/capture")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(buildPayload("request-extension-1")))
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, HeaderConstants.CLIENT_TYPE_NATIVE_HOST))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.accepted").value(true))
            .andReturn();

        JsonNode duplicateBody = objectMapper.readTree(duplicateResult.getResponse().getContentAsByteArray());
        assertThat(duplicateBody.path("data").path("taskId").asLong()).isEqualTo(firstTaskId);
        assertThat(downloadTaskRepository.countByCondition(null, null)).isEqualTo(1L);

        DownloadTaskDO downloadTaskDO = downloadTaskRepository.findByClientRequestId("request-extension-1")
            .orElseThrow(() -> new IllegalStateException("未查询到扩展接管创建的任务"));
        assertThat(downloadTaskDO.getSourceType()).isEqualTo("HTTPS");
        assertThat(downloadTaskDO.getSourceUri()).isEqualTo("https://example.com/files/demo.iso");
        assertThat(downloadTaskDO.getDisplayName()).isEqualTo("demo-from-browser.iso");
        assertThat(downloadTaskDO.getSaveDir()).isEqualTo("./downloads");
        assertThat(downloadTaskDO.getDomainStatus()).isEqualTo("RUNNING");
        assertThat(downloadTaskDO.getEngineGid()).isEqualTo("gid-extension-1");
        assertThat(downloadTaskDO.getEntryType()).isEqualTo("BROWSER_EXTENSION");
        assertThat(downloadTaskDO.getSourceSiteHost()).isEqualTo("example.com");
        assertThat(downloadTaskDO.getEntryContextJson()).contains("detail-page");

        ExternalEntryLogDO entryLogDO = externalEntryLogRepository.findLatestByClientRequestId("request-extension-1")
            .orElseThrow(() -> new IllegalStateException("未查询到扩展接管审计日志"));
        assertThat(entryLogDO.getSourceType()).isEqualTo("HTTPS");
        assertThat(entryLogDO.getMatchedRuleId()).isNull();
        assertThat(entryLogDO.getResultStatus()).isEqualTo("ACCEPTED");
    }

    @Test
    void shouldCaptureBrowserDownloadWhenClientIsBrowserExtension() throws Exception {
        when(aria2RpcClient.addUri(
            "https://example.com/files/demo.iso",
            "./downloads",
            null
        )).thenReturn("gid-extension-browser");

        mockMvc.perform(post("/api/extension/capture")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(buildPayload("request-extension-browser")))
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, HeaderConstants.CLIENT_TYPE_BROWSER_EXTENSION))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.accepted").value(true))
            .andExpect(jsonPath("$.data.resolvedSourceType").value("HTTPS"))
            .andExpect(jsonPath("$.data.siteRuleMatched").value(false));
    }

    @Test
    void shouldRejectCaptureWhenClientTypeIsNotNativeHost() throws Exception {
        mockMvc.perform(post("/api/extension/capture")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(buildPayload("request-extension-2")))
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, HeaderConstants.CLIENT_TYPE_DESKTOP_APP))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.message").value("仅允许 native-host 或 browser-extension 调用扩展接管接口"));
    }

    @Test
    void shouldRejectCaptureWhenBrowserCaptureDisabled() throws Exception {
        DownloadConfigDO downloadConfigDO = downloadConfigRepository.findSingleton()
            .orElseThrow(() -> new IllegalStateException("未查询到默认配置"));
        downloadConfigDO.setBrowserCaptureEnabled(0);
        downloadConfigDO.setUpdatedAt(System.currentTimeMillis());
        downloadConfigRepository.saveOrUpdate(downloadConfigDO);

        mockMvc.perform(post("/api/extension/capture")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(buildPayload("request-extension-3")))
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, HeaderConstants.CLIENT_TYPE_NATIVE_HOST))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CAPTURE_DISABLED"))
            .andExpect(jsonPath("$.message").value("浏览器接管未开启"));
    }

    @Test
    void shouldRejectUnsupportedBrowserType() throws Exception {
        Map<String, Object> payload = buildPayload("request-extension-4");
        payload.put("browser", "firefox");

        mockMvc.perform(post("/api/extension/capture")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(payload))
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, HeaderConstants.CLIENT_TYPE_NATIVE_HOST))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("COMMON_PARAM_INVALID"))
            .andExpect(jsonPath("$.message").value("不支持的浏览器类型: firefox"));
    }

    @Test
    void shouldMarkRuleMatchedWhenSiteRuleExists() throws Exception {
        SourceSiteRuleDO sourceSiteRuleDO = new SourceSiteRuleDO();
        sourceSiteRuleDO.setHostPattern("example.com");
        sourceSiteRuleDO.setSourceType("HTTPS");
        sourceSiteRuleDO.setBrowserCode("chrome");
        sourceSiteRuleDO.setProfileCode("default");
        sourceSiteRuleDO.setTrackerSetCode("builtin-default");
        sourceSiteRuleDO.setRequireHeaderSnapshot(0);
        sourceSiteRuleDO.setEnabled(1);
        sourceSiteRuleDO.setPriority(10);
        sourceSiteRuleDO.setCreatedAt(System.currentTimeMillis());
        sourceSiteRuleDO.setUpdatedAt(System.currentTimeMillis());
        sourceSiteRuleRepository.saveOrUpdate(sourceSiteRuleDO);

        when(aria2RpcClient.addUri(
            "https://example.com/files/demo.iso",
            "./downloads",
            null
        )).thenReturn("gid-extension-rule");

        mockMvc.perform(post("/api/extension/capture")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(buildPayload("request-extension-rule")))
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, HeaderConstants.CLIENT_TYPE_NATIVE_HOST))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.siteRuleMatched").value(true))
            .andExpect(jsonPath("$.data.resolvedSourceType").value("HTTPS"));

        ExternalEntryLogDO entryLogDO = externalEntryLogRepository.findLatestByClientRequestId("request-extension-rule")
            .orElseThrow(() -> new IllegalStateException("未查询到命中规则的审计日志"));
        assertThat(entryLogDO.getMatchedRuleId()).isEqualTo(sourceSiteRuleDO.getId());

        DownloadTaskDO taskDO = downloadTaskRepository.findByClientRequestId("request-extension-rule")
            .orElseThrow(() -> new IllegalStateException("未查询到命中规则创建的任务"));
        assertThat(taskDO.getEngineProfileCode()).isEqualTo("default");
        assertThat(taskDO.getEntryContextJson()).contains("\"matchedRuleId\":" + sourceSiteRuleDO.getId());
    }

    @Test
    void shouldRejectWhenMatchedRuleRequiresHeaderSnapshotButRequestMissingIt() throws Exception {
        SourceSiteRuleDO sourceSiteRuleDO = new SourceSiteRuleDO();
        sourceSiteRuleDO.setHostPattern("example.com");
        sourceSiteRuleDO.setSourceType("HTTPS");
        sourceSiteRuleDO.setBrowserCode("chrome");
        sourceSiteRuleDO.setProfileCode("default");
        sourceSiteRuleDO.setTrackerSetCode("builtin-default");
        sourceSiteRuleDO.setRequireHeaderSnapshot(1);
        sourceSiteRuleDO.setEnabled(1);
        sourceSiteRuleDO.setPriority(10);
        sourceSiteRuleDO.setCreatedAt(System.currentTimeMillis());
        sourceSiteRuleDO.setUpdatedAt(System.currentTimeMillis());
        sourceSiteRuleRepository.saveOrUpdate(sourceSiteRuleDO);

        Map<String, Object> payload = buildPayload("request-extension-header-required");
        payload.remove("headerSnapshotJson");

        mockMvc.perform(post("/api/extension/capture")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(payload))
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, HeaderConstants.CLIENT_TYPE_NATIVE_HOST))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("COMMON_PARAM_INVALID"))
            .andExpect(jsonPath("$.message").value("当前站点要求提供请求头快照"));
    }

    private Map<String, Object> buildPayload(String clientRequestId) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("clientRequestId", clientRequestId);
        payload.put("browser", "chrome");
        payload.put("tabUrl", "https://example.com/detail-page");
        payload.put("downloadUrl", "https://example.com/files/demo.iso");
        payload.put("suggestedName", "demo-from-browser.iso");
        payload.put("referer", "https://example.com/detail-page");
        payload.put("userAgent", "Mozilla/5.0 Test");
        payload.put("headerSnapshotJson", "{\"accept\":\"*/*\"}");
        return payload;
    }
}
