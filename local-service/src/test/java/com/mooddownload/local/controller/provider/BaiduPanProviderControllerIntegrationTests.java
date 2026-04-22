package com.mooddownload.local.controller.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooddownload.local.bootstrap.LocalServiceApplication;
import com.mooddownload.local.common.constant.HeaderConstants;
import com.mooddownload.local.dal.provider.ExternalProviderSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

/**
 * B5 阶段百度网盘预研接口集成测试。
 */
@SpringBootTest(classes = LocalServiceApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class BaiduPanProviderControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExternalProviderSessionRepository externalProviderSessionRepository;

    @Test
    void shouldPreflightShareLinkAndPersistSession() throws Exception {
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<String, Object>();
        payload.put("shareUrl", "https://pan.baidu.com/s/1abcdEFG");

        mockMvc.perform(post("/api/providers/baidupan/preflight")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(payload))
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.capability").value("SHARE_LINK_PRE_RESEARCH"))
            .andExpect(jsonPath("$.data.riskFlags[0]").value("PRE_RESEARCH_ONLY"));

        assertThat(externalProviderSessionRepository.findByProviderCode("BAIDUPAN")).hasSize(1);
    }

    @Test
    void shouldResolveBrowserAssistedShareLinkContext() throws Exception {
        java.util.Map<String, Object> providerContext = new java.util.LinkedHashMap<String, Object>();
        providerContext.put("shareUrl", "https://pan.baidu.com/s/1abcdEFG");
        providerContext.put("authContext", "{\"cookie\":\"BDUSS=demo\"}");

        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<String, Object>();
        payload.put("providerContext", objectMapper.writeValueAsString(providerContext));

        mockMvc.perform(post("/api/providers/baidupan/resolve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(payload))
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.resolvedMode").value("BROWSER_ASSISTED_SHARE_LINK"))
            .andExpect(jsonPath("$.data.nextStep").value("建议优先走浏览器辅助解析链路，不承诺直链稳定性"));
    }

    @Test
    void shouldRejectPreflightWithoutAnyInput() throws Exception {
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<String, Object>();

        mockMvc.perform(post("/api/providers/baidupan/preflight")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(payload))
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("COMMON_PARAM_INVALID"))
            .andExpect(jsonPath("$.message").value("shareUrl 或 authContext 至少提供一个"));
    }
}
