package com.mooddownload.local.controller.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooddownload.local.bootstrap.LocalServiceApplication;
import com.mooddownload.local.client.aria2.Aria2RpcClient;
import com.mooddownload.local.client.aria2.dto.Aria2TaskFileDTO;
import com.mooddownload.local.common.constant.HeaderConstants;
import com.mooddownload.local.common.enums.ErrorCode;
import com.mooddownload.local.common.exception.BizException;
import com.mooddownload.local.dal.task.DownloadTaskRepository;
import com.mooddownload.local.mapper.task.DownloadTaskDO;
import com.mooddownload.local.service.task.state.DownloadTaskStatus;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

/**
 * B5 阶段任务接口集成测试。
 */
@SpringBootTest(classes = LocalServiceApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TaskControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DownloadTaskRepository downloadTaskRepository;

    @MockBean
    private Aria2RpcClient aria2RpcClient;

    @Test
    void shouldCreateTaskAndQueryTaskApis() throws Exception {
        when(aria2RpcClient.addUri("https://example.com/demo.iso", "./downloads", "demo.iso"))
            .thenReturn("gid-api-create");

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(buildCreatePayload("request-api-create")))
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.domainStatus").value("RUNNING"))
            .andExpect(jsonPath("$.data.engineStatus").value("ACTIVE"))
            .andExpect(jsonPath("$.data.displayName").value("demo.iso"));

        DownloadTaskDO persistedTask = downloadTaskRepository.findByClientRequestId("request-api-create")
            .orElseThrow(() -> new IllegalStateException("未查询到刚创建的任务"));
        assertThat(persistedTask.getSaveDir()).isEqualTo("./downloads");
        assertThat(persistedTask.getEngineGid()).isEqualTo("gid-api-create");

        mockMvc.perform(get("/api/tasks")
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].taskId").value(persistedTask.getId()))
            .andExpect(jsonPath("$.data.items[0].domainStatus").value("RUNNING"))
            .andExpect(jsonPath("$.data.items[0].saveDir").value("./downloads"));

        mockMvc.perform(get("/api/tasks/{id}", persistedTask.getId())
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.taskId").value(persistedTask.getId()))
            .andExpect(jsonPath("$.data.domainStatus").value("RUNNING"))
            .andExpect(jsonPath("$.data.sourceUri").value("https://example.com/demo.iso"))
            .andExpect(jsonPath("$.data.saveDir").value("./downloads"));
    }

    @Test
    void shouldImportTorrentTaskThroughMultipartEndpoint() throws Exception {
        when(aria2RpcClient.addTorrent(anyString(), eq("./downloads"), eq("demo.torrent")))
            .thenReturn("gid-api-torrent");
        when(aria2RpcClient.getFiles("gid-api-torrent")).thenReturn(java.util.Arrays.asList(
            buildTorrentFile("1", "/downloads/demo/Season 1/Episode 01.mkv", "1048576", "true"),
            buildTorrentFile("2", "/downloads/demo/Season 1/Episode 01.srt", "4096", "true")
        ));

        MockMultipartFile torrentFile = new MockMultipartFile(
            "torrentFile",
            "demo.torrent",
            "application/x-bittorrent",
            ("d4:infod6:lengthi12345e4:name8:test.txt12:piece lengthi16384e"
                + "6:pieces20:12345678901234567890ee")
                .getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/tasks/torrent")
                .file(torrentFile)
                .param("clientRequestId", "request-api-torrent")
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.displayName").value("demo.torrent"))
            .andExpect(jsonPath("$.data.domainStatus").value("RUNNING"));

        DownloadTaskDO persistedTask = downloadTaskRepository.findByClientRequestId("request-api-torrent")
            .orElseThrow(() -> new IllegalStateException("未查询到种子导入任务"));
        assertThat(persistedTask.getSourceType()).isEqualTo("TORRENT");
        assertThat(persistedTask.getSaveDir()).isEqualTo("./downloads");
        assertThat(persistedTask.getTorrentFilePath()).contains("request-api-torrent");
        assertThat(persistedTask.getSourceUri()).isEqualTo(persistedTask.getTorrentFilePath());
        assertThat(persistedTask.getEngineGid()).isEqualTo("gid-api-torrent");
        assertThat(persistedTask.getTorrentFileListJson()).contains("Episode 01.mkv");

        mockMvc.perform(get("/api/tasks/{id}", persistedTask.getId())
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.torrentFiles[0].fileIndex").value(1))
            .andExpect(jsonPath("$.data.torrentFiles[0].filePath").value("/downloads/demo/Season 1/Episode 01.mkv"))
            .andExpect(jsonPath("$.data.torrentFiles[0].fileSizeBytes").value(1048576))
            .andExpect(jsonPath("$.data.engineTasks[0].engineGid").value("gid-api-torrent"))
            .andExpect(jsonPath("$.data.engineTasks[0].metadataOnly").value(false))
            .andExpect(jsonPath("$.data.torrentFiles[1].filePath").value("/downloads/demo/Season 1/Episode 01.srt"));
    }

    @Test
    void shouldExposeMagnetFileListInTaskDetail() throws Exception {
        when(aria2RpcClient.addUri(
            "magnet:?xt=urn:btih:ABCDEF1234567890ABCDEF1234567890ABCDEF12",
            "./downloads",
            "demo-magnet"
        ))
            .thenReturn("gid-api-magnet");
        when(aria2RpcClient.getFiles("gid-api-magnet")).thenReturn(java.util.Arrays.asList(
            buildTorrentFile("1", "/downloads/demo-magnet/Season 1/Episode 01.mkv", "2097152", "true"),
            buildTorrentFile("2", "/downloads/demo-magnet/Season 1/sample.txt", "1024", "false")
        ));

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(buildCreatePayload(
                    "request-api-magnet",
                    "MAGNET",
                    "magnet:?xt=urn:btih:ABCDEF1234567890ABCDEF1234567890ABCDEF12",
                    "demo-magnet"
                )))
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.domainStatus").value("RUNNING"));

        DownloadTaskDO persistedTask = downloadTaskRepository.findByClientRequestId("request-api-magnet")
            .orElseThrow(() -> new IllegalStateException("未查询到磁力任务"));
        assertThat(persistedTask.getSourceType()).isEqualTo("MAGNET");
        assertThat(persistedTask.getEngineGid()).isEqualTo("gid-api-magnet");
        assertThat(persistedTask.getTorrentFileListJson()).contains("Episode 01.mkv");

        mockMvc.perform(get("/api/tasks/{id}", persistedTask.getId())
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.sourceType").value("MAGNET"))
            .andExpect(jsonPath("$.data.torrentMetadataReady").value(true))
            .andExpect(jsonPath("$.data.engineTasks[0].engineGid").value("gid-api-magnet"))
            .andExpect(jsonPath("$.data.torrentFiles[0].fileIndex").value(1))
            .andExpect(jsonPath("$.data.torrentFiles[0].filePath")
                .value("/downloads/demo-magnet/Season 1/Episode 01.mkv"))
            .andExpect(jsonPath("$.data.torrentFiles[0].fileSizeBytes").value(2097152))
            .andExpect(jsonPath("$.data.torrentFiles[1].selected").value(false));
    }

    @Test
    void shouldReturnFailedTaskWhenTorrentAlreadyRegisteredInAria2() throws Exception {
        when(aria2RpcClient.addTorrent(anyString(), eq("./downloads"), eq("duplicate.torrent")))
            .thenThrow(new BizException(
                ErrorCode.EXTERNAL_ENGINE_ERROR,
                "aria2 RPC 调用失败: InfoHash c8295ce630f2064f08440db1534e4992cfe4862a is already registered."
            ));

        MockMultipartFile torrentFile = new MockMultipartFile(
            "torrentFile",
            "duplicate.torrent",
            "application/x-bittorrent",
            ("d4:infod6:lengthi12345e4:name8:test.txt12:piece lengthi16384e"
                + "6:pieces20:12345678901234567890ee")
                .getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/tasks/torrent")
                .file(torrentFile)
                .param("clientRequestId", "request-api-torrent-duplicate")
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.displayName").value("duplicate.torrent"))
            .andExpect(jsonPath("$.data.domainStatus").value("FAILED"))
            .andExpect(jsonPath("$.data.engineStatus").value("ERROR"));

        DownloadTaskDO persistedTask = downloadTaskRepository.findByClientRequestId("request-api-torrent-duplicate")
            .orElseThrow(() -> new IllegalStateException("未查询到重复种子任务"));
        assertThat(persistedTask.getDomainStatus()).isEqualTo("FAILED");
        assertThat(persistedTask.getErrorCode()).isEqualTo(ErrorCode.STATE_CONFLICT.getCode());
        assertThat(persistedTask.getErrorMessage()).isEqualTo("BT 任务已存在于下载引擎，请勿重复导入");
    }

    @Test
    void shouldReuseExistingTorrentTaskWhenInfoHashMatches() throws Exception {
        when(aria2RpcClient.addTorrent(anyString(), eq("./downloads"), eq("duplicate-reuse.torrent")))
            .thenReturn("gid-api-torrent-reuse");
        when(aria2RpcClient.getFiles("gid-api-torrent-reuse")).thenReturn(java.util.Collections.singletonList(
            buildTorrentFile("1", "/downloads/demo-reuse/Episode 01.mkv", "1024", "true")
        ));

        MockMultipartFile firstTorrentFile = new MockMultipartFile(
            "torrentFile",
            "duplicate-reuse.torrent",
            "application/x-bittorrent",
            ("d4:infod6:lengthi12345e4:name8:test.txt12:piece lengthi16384e"
                + "6:pieces20:12345678901234567890ee")
                .getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile duplicateTorrentFile = new MockMultipartFile(
            "torrentFile",
            "duplicate-reuse.torrent",
            "application/x-bittorrent",
            ("d4:infod6:lengthi12345e4:name8:test.txt12:piece lengthi16384e"
                + "6:pieces20:12345678901234567890ee")
                .getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/tasks/torrent")
                .file(firstTorrentFile)
                .param("clientRequestId", "request-api-torrent-reuse-1")
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"));

        DownloadTaskDO firstTask = downloadTaskRepository.findByClientRequestId("request-api-torrent-reuse-1")
            .orElseThrow(() -> new IllegalStateException("未查询到首次种子任务"));

        mockMvc.perform(multipart("/api/tasks/torrent")
                .file(duplicateTorrentFile)
                .param("clientRequestId", "request-api-torrent-reuse-2")
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.taskId").value(firstTask.getId()))
            .andExpect(jsonPath("$.data.taskCode").value(firstTask.getTaskCode()));

        assertThat(downloadTaskRepository.findByClientRequestId("request-api-torrent-reuse-2")).isEmpty();
        assertThat(downloadTaskRepository.countByCondition(null, null)).isEqualTo(1);
        verify(aria2RpcClient, times(1)).addTorrent(anyString(), eq("./downloads"), eq("duplicate-reuse.torrent"));
    }

    @Test
    void shouldPauseResumeRetryAndDeleteTaskViaApi() throws Exception {
        Path tempDownloadDir = Files.createTempDirectory("task-controller-test");
        Files.write(tempDownloadDir.resolve("request-api-failed.iso"), "payload".getBytes(StandardCharsets.UTF_8));
        Long runningTaskId = seedTask(DownloadTaskStatus.RUNNING, 0, 3, "request-api-running", tempDownloadDir.toString());
        Long failedTaskId = seedTask(DownloadTaskStatus.FAILED, 1, 3, "request-api-failed", tempDownloadDir.toString());

        when(aria2RpcClient.addUri(
            "https://example.com/request-api-running.iso",
            tempDownloadDir.toString(),
            "request-api-running.iso"
        )).thenReturn("gid-api-running-resume");
        when(aria2RpcClient.addUri(
            "https://example.com/request-api-failed.iso",
            tempDownloadDir.toString(),
            "request-api-failed.iso"
        )).thenReturn("gid-api-failed-retry");
        when(aria2RpcClient.remove("gid-api-failed-retry")).thenReturn("gid-api-failed-retry");

        mockMvc.perform(post("/api/tasks/{id}/pause", runningTaskId)
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.taskId").value(runningTaskId))
            .andExpect(jsonPath("$.data.domainStatus").value("PAUSED"))
            .andExpect(jsonPath("$.data.operationApplied").value(true));

        mockMvc.perform(post("/api/tasks/{id}/resume", runningTaskId)
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.domainStatus").value("RUNNING"))
            .andExpect(jsonPath("$.data.operationApplied").value(true));

        mockMvc.perform(post("/api/tasks/{id}/retry", failedTaskId)
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.taskId").value(failedTaskId))
            .andExpect(jsonPath("$.data.domainStatus").value("RUNNING"))
            .andExpect(jsonPath("$.data.retryCount").value(2));

        mockMvc.perform(delete("/api/tasks/{id}", failedTaskId)
                .param("removeFiles", "true")
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.taskId").value(failedTaskId))
            .andExpect(jsonPath("$.data.removed").value(true))
            .andExpect(jsonPath("$.data.filesRemoved").value(true));

        assertThat(downloadTaskRepository.findById(runningTaskId))
            .get()
            .extracting(DownloadTaskDO::getDomainStatus)
            .isEqualTo("RUNNING");
        assertThat(downloadTaskRepository.findById(failedTaskId))
            .get()
            .extracting(DownloadTaskDO::getDomainStatus)
            .isEqualTo("CANCELLED");
        assertThat(Files.exists(tempDownloadDir.resolve("request-api-failed.iso"))).isFalse();

        mockMvc.perform(get("/api/tasks")
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].taskId").value(runningTaskId));
    }

    @Test
    void shouldRejectUnsupportedTaskStatusFilter() throws Exception {
        mockMvc.perform(get("/api/tasks")
                .param("status", "invalid-status")
                .header(HeaderConstants.LOCAL_TOKEN, "test-local-token")
                .header(HeaderConstants.CLIENT_TYPE, "desktop-app"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("COMMON_PARAM_INVALID"))
            .andExpect(jsonPath("$.message").value("不支持的任务状态: invalid-status"));
    }

    private Object buildCreatePayload(String clientRequestId) {
        return buildCreatePayload(clientRequestId, "HTTP", "https://example.com/demo.iso", "demo.iso");
    }

    private Object buildCreatePayload(String clientRequestId, String sourceType, String sourceUri, String displayName) {
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<String, Object>();
        payload.put("clientRequestId", clientRequestId);
        payload.put("sourceType", sourceType);
        payload.put("sourceUri", sourceUri);
        payload.put("displayName", displayName);
        return payload;
    }

    private Long seedTask(DownloadTaskStatus downloadTaskStatus, int retryCount, int maxRetryCount, String clientRequestId) {
        return seedTask(downloadTaskStatus, retryCount, maxRetryCount, clientRequestId, "./downloads");
    }

    private Long seedTask(
        DownloadTaskStatus downloadTaskStatus,
        int retryCount,
        int maxRetryCount,
        String clientRequestId,
        String saveDir
    ) {
        long now = System.currentTimeMillis();
        DownloadTaskDO downloadTaskDO = new DownloadTaskDO();
        downloadTaskDO.setTaskCode("TASK-" + now + "-" + clientRequestId);
        downloadTaskDO.setSourceType("HTTP");
        downloadTaskDO.setSourceUri("https://example.com/" + clientRequestId + ".iso");
        downloadTaskDO.setDisplayName(clientRequestId + ".iso");
        downloadTaskDO.setDomainStatus(downloadTaskStatus.name());
        downloadTaskDO.setEngineStatus(downloadTaskStatus == DownloadTaskStatus.RUNNING ? "ACTIVE" : "ERROR");
        downloadTaskDO.setEngineGid("gid-" + clientRequestId);
        downloadTaskDO.setQueuePriority(100);
        downloadTaskDO.setSaveDir(saveDir);
        downloadTaskDO.setTotalSizeBytes(1024L);
        downloadTaskDO.setCompletedSizeBytes(256L);
        downloadTaskDO.setDownloadSpeedBps(128L);
        downloadTaskDO.setUploadSpeedBps(0L);
        downloadTaskDO.setErrorCode(downloadTaskStatus == DownloadTaskStatus.FAILED ? "ARIA2_ERROR" : null);
        downloadTaskDO.setErrorMessage(downloadTaskStatus == DownloadTaskStatus.FAILED ? "rpc timeout" : null);
        downloadTaskDO.setRetryCount(retryCount);
        downloadTaskDO.setMaxRetryCount(maxRetryCount);
        downloadTaskDO.setClientRequestId(clientRequestId);
        downloadTaskDO.setVersion(0);
        downloadTaskDO.setCreatedAt(now);
        downloadTaskDO.setUpdatedAt(now);
        return downloadTaskRepository.save(downloadTaskDO);
    }

    private Aria2TaskFileDTO buildTorrentFile(String index, String path, String length, String selected) {
        Aria2TaskFileDTO aria2TaskFileDTO = new Aria2TaskFileDTO();
        aria2TaskFileDTO.setIndex(index);
        aria2TaskFileDTO.setPath(path);
        aria2TaskFileDTO.setLength(length);
        aria2TaskFileDTO.setSelected(selected);
        return aria2TaskFileDTO;
    }
}
