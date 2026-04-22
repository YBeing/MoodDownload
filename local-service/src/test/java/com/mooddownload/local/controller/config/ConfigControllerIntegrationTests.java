package com.mooddownload.local.controller.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooddownload.local.bootstrap.LocalServiceApplication;
import com.mooddownload.local.client.aria2.Aria2RpcClient;
import com.mooddownload.local.common.constant.HeaderConstants;
import com.mooddownload.local.dal.config.DownloadConfigRepository;
import com.mooddownload.local.dal.profile.EngineRuntimeProfileRepository;
import com.mooddownload.local.dal.profile.SourceSiteRuleRepository;
import com.mooddownload.local.mapper.config.DownloadConfigDO;
import com.mooddownload.local.mapper.profile.EngineRuntimeProfileDO;
import com.mooddownload.local.mapper.profile.SourceSiteRuleDO;
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

/**
 * B5 阶段配置接口集成测试。
 */
@SpringBootTest(classes = LocalServiceApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ConfigControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DownloadConfigRepository downloadConfigRepository;

    @Autowired
    private EngineRuntimeProfileRepository engineRuntimeProfileRepository;

    @Autowired
    private SourceSiteRuleRepository sourceSiteRuleRepository;

    @MockBean
    private Aria2RpcClient aria2RpcClient;

    @Test
    void shouldGetAndUpdateConfig() throws Exception {
        mockMvc.perform(get("/api/config")
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.defaultSaveDir").value("./downloads"))
            .andExpect(jsonPath("$.data.maxConcurrentDownloads").value(3))
            .andExpect(jsonPath("$.data.autoStartEnabled").value(false));

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("defaultSaveDir", "./downloads/custom");
        payload.put("maxConcurrentDownloads", 5);
        payload.put("maxGlobalDownloadSpeed", 2048);
        payload.put("maxGlobalUploadSpeed", 512);
        payload.put("browserCaptureEnabled", Boolean.FALSE);
        payload.put("clipboardMonitorEnabled", Boolean.FALSE);

        mockMvc.perform(put("/api/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(payload))
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.defaultSaveDir").value("./downloads/custom"))
            .andExpect(jsonPath("$.data.maxConcurrentDownloads").value(5))
            .andExpect(jsonPath("$.data.maxGlobalDownloadSpeed").value(2048))
            .andExpect(jsonPath("$.data.maxGlobalUploadSpeed").value(512))
            .andExpect(jsonPath("$.data.browserCaptureEnabled").value(false))
            .andExpect(jsonPath("$.data.clipboardMonitorEnabled").value(false));

        DownloadConfigDO downloadConfigDO = downloadConfigRepository.findSingleton()
            .orElseThrow(() -> new IllegalStateException("未查询到更新后的下载配置"));
        assertThat(downloadConfigDO.getDefaultSaveDir()).isEqualTo("./downloads/custom");
        assertThat(downloadConfigDO.getMaxConcurrentDownloads()).isEqualTo(5);
        assertThat(downloadConfigDO.getBrowserCaptureEnabled()).isEqualTo(0);
        assertThat(downloadConfigDO.getClipboardMonitorEnabled()).isEqualTo(0);
    }

    @Test
    void shouldRejectInvalidConfigUpdate() throws Exception {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("maxConcurrentDownloads", 0);

        mockMvc.perform(put("/api/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(payload))
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("COMMON_PARAM_INVALID"))
            .andExpect(jsonPath("$.message").value("maxConcurrentDownloads 必须大于 0"));
    }

    @Test
    void shouldGetUpdateAndApplyEngineRuntimeProfile() throws Exception {
        mockMvc.perform(get("/api/config/engine-runtime")
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.activeProfileCode").value("default"))
            .andExpect(jsonPath("$.data.profiles[0].profileCode").value("default"))
            .andExpect(jsonPath("$.data.applyStatus").value("IDLE"));

        Map<String, Object> updatePayload = new LinkedHashMap<String, Object>();
        updatePayload.put("profileCode", "default");
        updatePayload.put("profileJson", "{\"max-concurrent-downloads\":\"8\",\"bt-tracker\":\"udp://tracker.example:80/announce\"}");

        mockMvc.perform(put("/api/config/engine-runtime")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(updatePayload))
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.activeProfileCode").value("default"))
            .andExpect(jsonPath("$.data.applyStatus").value("UPDATED"));

        EngineRuntimeProfileDO profileDO = engineRuntimeProfileRepository.findByCode("default")
            .orElseThrow(() -> new IllegalStateException("未查询到默认配置模板"));
        assertThat(profileDO.getProfileJson()).contains("max-concurrent-downloads");

        when(aria2RpcClient.changeGlobalOption(anyMap())).thenReturn("OK");
        Map<String, Object> applyPayload = new LinkedHashMap<String, Object>();
        applyPayload.put("profileCode", "default");
        applyPayload.put("forceRestart", false);

        mockMvc.perform(post("/api/config/engine-runtime/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(applyPayload))
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.profileCode").value("default"))
            .andExpect(jsonPath("$.data.applyStatus").value("PARTIALLY_APPLIED"))
            .andExpect(jsonPath("$.data.restartRequired").value(true));

        verify(aria2RpcClient).changeGlobalOption(anyMap());
    }

    @Test
    void shouldListAndUpdateTrackerSets() throws Exception {
        mockMvc.perform(get("/api/config/tracker-sets")
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data[0].trackerSetCode").value("builtin-default"));

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("trackerSetName", "公共 Tracker");
        payload.put("trackerListText", "udp://tracker.example:80/announce");
        payload.put("sourceUrl", "https://example.com/trackers.txt");

        mockMvc.perform(put("/api/config/tracker-sets/{code}", "public-set")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(payload))
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.trackerSetCode").value("public-set"))
            .andExpect(jsonPath("$.data.trackerSetName").value("公共 Tracker"))
            .andExpect(jsonPath("$.data.sourceUrl").value("https://example.com/trackers.txt"));
    }

    @Test
    void shouldListAndUpdateSourceSiteRules() throws Exception {
        mockMvc.perform(get("/api/config/engine-runtime")
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isOk());

        SourceSiteRuleDO sourceSiteRuleDO = new SourceSiteRuleDO();
        sourceSiteRuleDO.setHostPattern("*.example.com");
        sourceSiteRuleDO.setSourceType("BT");
        sourceSiteRuleDO.setBrowserCode("chrome");
        sourceSiteRuleDO.setProfileCode("default");
        sourceSiteRuleDO.setTrackerSetCode("builtin-default");
        sourceSiteRuleDO.setRequireHeaderSnapshot(1);
        sourceSiteRuleDO.setEnabled(1);
        sourceSiteRuleDO.setPriority(100);
        sourceSiteRuleDO.setCreatedAt(System.currentTimeMillis());
        sourceSiteRuleDO.setUpdatedAt(System.currentTimeMillis());
        sourceSiteRuleRepository.saveOrUpdate(sourceSiteRuleDO);

        mockMvc.perform(get("/api/config/site-rules")
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data[0].hostPattern").value("*.example.com"));

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("hostPattern", "download.example.com");
        payload.put("profileCode", "default");
        payload.put("trackerSetCode", "builtin-default");

        mockMvc.perform(put("/api/config/site-rules/{id}", sourceSiteRuleDO.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(payload))
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.id").value(sourceSiteRuleDO.getId()))
            .andExpect(jsonPath("$.data.hostPattern").value("download.example.com"))
            .andExpect(jsonPath("$.data.profileCode").value("default"));
    }
}
