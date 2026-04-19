package com.mooddownload.local.controller.task;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mooddownload.local.bootstrap.LocalServiceApplication;
import com.mooddownload.local.common.constant.HeaderConstants;
import com.mooddownload.local.service.task.TaskEventPublisher;
import com.mooddownload.local.service.task.model.DownloadTaskModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * B5 阶段任务事件接口集成测试。
 */
@SpringBootTest(classes = LocalServiceApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TaskEventControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskEventPublisher taskEventPublisher;

    @AfterEach
    void tearDown() {
        taskEventPublisher.completeAllEmitters();
    }

    @Test
    void shouldSendReadyEventImmediatelyAfterSubscribe() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/api/events/tasks")
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(request().asyncStarted())
            .andReturn();

        taskEventPublisher.completeAllEmitters();

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
            .andExpect(content().string(containsString("event:stream.ready")))
            .andExpect(content().string(containsString("\"eventType\":\"stream.ready\"")));
    }

    @Test
    void shouldStreamTaskUpdatedEventViaSse() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/api/events/tasks")
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(request().asyncStarted())
            .andReturn();

        taskEventPublisher.publishTaskUpdated(buildTaskModel());
        taskEventPublisher.completeAllEmitters();

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
            .andExpect(content().string(containsString("event:stream.ready")))
            .andExpect(content().string(containsString("event:task.updated")))
            .andExpect(content().string(containsString("\"taskId\":1001")))
            .andExpect(content().string(containsString("\"taskCode\":\"TASK-SSE-1001\"")))
            .andExpect(content().string(containsString("\"domainStatus\":\"RUNNING\"")));
    }

    private DownloadTaskModel buildTaskModel() {
        DownloadTaskModel downloadTaskModel = new DownloadTaskModel();
        downloadTaskModel.setId(1001L);
        downloadTaskModel.setTaskCode("TASK-SSE-1001");
        downloadTaskModel.setDomainStatus("RUNNING");
        downloadTaskModel.setEngineStatus("ACTIVE");
        downloadTaskModel.setTotalSizeBytes(100L);
        downloadTaskModel.setCompletedSizeBytes(50L);
        downloadTaskModel.setDownloadSpeedBps(2048L);
        return downloadTaskModel;
    }
}
