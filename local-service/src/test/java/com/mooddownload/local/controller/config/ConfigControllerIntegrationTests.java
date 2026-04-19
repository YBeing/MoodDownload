package com.mooddownload.local.controller.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooddownload.local.bootstrap.LocalServiceApplication;
import com.mooddownload.local.common.constant.HeaderConstants;
import com.mooddownload.local.dal.config.DownloadConfigRepository;
import com.mooddownload.local.mapper.config.DownloadConfigDO;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
}
