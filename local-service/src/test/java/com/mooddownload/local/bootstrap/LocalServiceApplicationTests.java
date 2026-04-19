package com.mooddownload.local.bootstrap;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mooddownload.local.common.constant.HeaderConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * B1 阶段 smoke test。
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class LocalServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldLoadHealthEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(header().exists(HeaderConstants.REQUEST_ID));
    }

    @Test
    void shouldReturnWrappedSuccessResponseWhenTokenIsValid() throws Exception {
        mockMvc.perform(get("/api/system/ping")
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.message").value("ok"))
            .andExpect(jsonPath("$.data.status").value("pong"))
            .andExpect(header().exists(HeaderConstants.REQUEST_ID));
    }

    @Test
    void shouldRejectRequestWhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/system/ping"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.message").value("缺少或错误的本地访问令牌"))
            .andExpect(header().exists(HeaderConstants.REQUEST_ID));
    }

    @Test
    void shouldAllowCorsPreflightWithoutToken() throws Exception {
        mockMvc.perform(options("/api/system/ping")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers",
                    HeaderConstants.LOCAL_TOKEN + "," + HeaderConstants.CLIENT_TYPE))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
            .andExpect(header().string("Access-Control-Allow-Methods", containsString("GET")))
            .andExpect(header().string("Access-Control-Allow-Headers", containsString(HeaderConstants.LOCAL_TOKEN)))
            .andExpect(header().exists(HeaderConstants.REQUEST_ID));
    }
}
