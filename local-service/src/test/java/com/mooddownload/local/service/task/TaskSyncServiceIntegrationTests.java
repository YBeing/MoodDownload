package com.mooddownload.local.service.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.mooddownload.local.bootstrap.LocalServiceApplication;
import com.mooddownload.local.client.aria2.Aria2RpcClient;
import com.mooddownload.local.client.aria2.dto.Aria2TaskFileDTO;
import com.mooddownload.local.client.aria2.dto.Aria2TaskStatusDTO;
import com.mooddownload.local.dal.task.DownloadTaskRepository;
import com.mooddownload.local.dal.task.TaskStateLogRepository;
import com.mooddownload.local.mapper.task.DownloadTaskDO;
import com.mooddownload.local.mapper.task.TaskStateLogDO;
import com.mooddownload.local.service.task.model.DownloadTaskModel;
import com.mooddownload.local.service.task.state.DownloadTaskStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

/**
 * 任务轮询同步集成测试。
 */
@SpringBootTest(classes = LocalServiceApplication.class)
@ActiveProfiles("test")
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TaskSyncServiceIntegrationTests {

    @Autowired
    private TaskSyncService taskSyncService;

    @Autowired
    private DownloadTaskRepository downloadTaskRepository;

    @Autowired
    private TaskQueryService taskQueryService;

    @Autowired
    private TaskStateLogRepository taskStateLogRepository;

    @MockBean
    private Aria2RpcClient aria2RpcClient;

    @Test
    void shouldUpdateDownloadSpeedAndProgressFromAria2Snapshot() {
        Long taskId = seedTask(DownloadTaskStatus.RUNNING, "gid-sync-running");
        when(aria2RpcClient.tellActive()).thenReturn(Collections.singletonList(
            buildStatus("gid-sync-running", "active", "4096", "2048", "1024", "32", null, null)
        ));
        when(aria2RpcClient.tellWaiting(0, 100)).thenReturn(Collections.emptyList());
        when(aria2RpcClient.tellStopped(0, 100)).thenReturn(Collections.emptyList());

        taskSyncService.synchronizeTasks();

        DownloadTaskDO persistedTask = downloadTaskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalStateException("未查询到同步后的任务"));
        assertThat(persistedTask.getDomainStatus()).isEqualTo("RUNNING");
        assertThat(persistedTask.getCompletedSizeBytes()).isEqualTo(2048L);
        assertThat(persistedTask.getDownloadSpeedBps()).isEqualTo(1024L);
        assertThat(persistedTask.getUploadSpeedBps()).isEqualTo(32L);
        assertThat(persistedTask.getLastSyncAt()).isNotNull();
        assertThat(taskStateLogRepository.listByTaskId(taskId)).isEmpty();
    }

    @Test
    void shouldPromoteTaskToCompletedWhenStoppedQueueReturnsComplete() {
        Long taskId = seedTask(DownloadTaskStatus.RUNNING, "gid-sync-complete");
        Path downloadedFile = createDownloadedFile(2048);
        when(aria2RpcClient.tellActive()).thenReturn(Collections.emptyList());
        when(aria2RpcClient.tellWaiting(0, 100)).thenReturn(Collections.emptyList());
        when(aria2RpcClient.tellStopped(0, 100)).thenReturn(Collections.singletonList(
            buildStatus("gid-sync-complete", "complete", "8192", "8192", "0", "0", null, null)
        ));
        when(aria2RpcClient.getFiles("gid-sync-complete")).thenReturn(Collections.singletonList(
            buildTorrentFile("1", downloadedFile.toString(), "51200", "true")
        ));

        taskSyncService.synchronizeTasks();

        DownloadTaskDO persistedTask = downloadTaskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalStateException("未查询到同步后的任务"));
        assertThat(persistedTask.getDomainStatus()).isEqualTo("COMPLETED");
        assertThat(persistedTask.getCompletedSizeBytes()).isEqualTo(8192L);
        assertThat(persistedTask.getTorrentFileListJson()).contains("\"fileSizeBytes\":2048");
        assertThat(taskStateLogRepository.listByTaskId(taskId))
            .extracting(log -> log.getToStatus())
            .containsExactly("COMPLETED");
    }

    @Test
    void shouldRebindMagnetTaskToFollowUpGidAndRefreshTorrentFiles() {
        Long taskId = seedTask(DownloadTaskStatus.RUNNING, "gid-sync-metadata", "MAGNET");
        when(aria2RpcClient.tellActive()).thenReturn(Collections.singletonList(
            buildStatus("gid-sync-real", "active", "734003200", "104857600", "2048", "64", null, null)
        ));
        when(aria2RpcClient.tellWaiting(0, 100)).thenReturn(Collections.emptyList());
        when(aria2RpcClient.tellStopped(0, 100)).thenReturn(Collections.singletonList(
            buildStatus("gid-sync-metadata", "complete", "0", "0", "0", "0", null, null)
        ));
        when(aria2RpcClient.getFiles("gid-sync-real")).thenReturn(Arrays.asList(
            buildTorrentFile("1", "/downloads/demo/Season 1/Episode 01.mkv", "734003200", "true")
        ));

        Aria2TaskStatusDTO realDownloadStatus = buildStatus(
            "gid-sync-real", "active", "734003200", "104857600", "2048", "64", null, null
        );
        realDownloadStatus.setBelongsTo("gid-sync-metadata");
        Aria2TaskStatusDTO metadataStatus = buildStatus(
            "gid-sync-metadata", "complete", "0", "0", "0", "0", null, null
        );
        metadataStatus.setFollowedBy(Collections.singletonList("gid-sync-real"));
        when(aria2RpcClient.tellActive()).thenReturn(Collections.singletonList(realDownloadStatus));
        when(aria2RpcClient.tellStopped(0, 100)).thenReturn(Collections.singletonList(metadataStatus));

        taskSyncService.synchronizeTasks();

        DownloadTaskDO persistedTask = downloadTaskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalStateException("未查询到续接后的磁力任务"));
        assertThat(persistedTask.getEngineGid()).isEqualTo("gid-sync-real");
        assertThat(persistedTask.getDomainStatus()).isEqualTo("RUNNING");
        assertThat(persistedTask.getCompletedSizeBytes()).isEqualTo(104857600L);
        assertThat(persistedTask.getTotalSizeBytes()).isEqualTo(734003200L);
        assertThat(persistedTask.getTorrentFileListJson()).contains("Episode 01.mkv");
        DownloadTaskModel aggregatedTask = taskQueryService.getTaskById(taskId);
        assertThat(aggregatedTask.getEngineTasks()).hasSize(2);
        assertThat(aggregatedTask.getEngineTasks())
            .extracting(engineTask -> engineTask.getEngineGid())
            .containsExactly("gid-sync-real", "gid-sync-metadata");
        assertThat(aggregatedTask.getTorrentFiles()).hasSize(1);
        assertThat(taskStateLogRepository.listByTaskId(taskId)).isEmpty();
    }

    @Test
    void shouldKeepRecentUserPausedStatusWhenBtEngineStillReportsActive() {
        Long taskId = seedTask(DownloadTaskStatus.PAUSED, "gid-sync-real", "MAGNET");
        saveStateLog(taskId, "RUNNING", "PAUSED", "USER", "PAUSE", System.currentTimeMillis());
        Aria2TaskStatusDTO realDownloadStatus = buildStatus(
            "gid-sync-real", "active", "734003200", "104857600", "2048", "64", null, null
        );
        realDownloadStatus.setBelongsTo("gid-sync-metadata");
        Aria2TaskStatusDTO metadataStatus = buildStatus(
            "gid-sync-metadata", "complete", "0", "0", "0", "0", null, null
        );
        metadataStatus.setFollowedBy(Collections.singletonList("gid-sync-real"));
        when(aria2RpcClient.tellActive()).thenReturn(Collections.singletonList(realDownloadStatus));
        when(aria2RpcClient.tellWaiting(0, 100)).thenReturn(Collections.emptyList());
        when(aria2RpcClient.tellStopped(0, 100)).thenReturn(Collections.singletonList(metadataStatus));
        when(aria2RpcClient.getFiles("gid-sync-real")).thenReturn(Collections.singletonList(
            buildTorrentFile("1", "/downloads/demo/Season 1/Episode 01.mkv", "734003200", "true")
        ));

        taskSyncService.synchronizeTasks();

        DownloadTaskDO persistedTask = downloadTaskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalStateException("未查询到暂停保护后的磁力任务"));
        assertThat(persistedTask.getDomainStatus()).isEqualTo("PAUSED");
        assertThat(persistedTask.getCompletedSizeBytes()).isEqualTo(0L);
        assertThat(taskStateLogRepository.listByTaskId(taskId))
            .extracting(TaskStateLogDO::getToStatus)
            .containsExactly("PAUSED");
    }

    private Long seedTask(DownloadTaskStatus downloadTaskStatus, String engineGid) {
        return seedTask(downloadTaskStatus, engineGid, "TORRENT");
    }

    private Long seedTask(DownloadTaskStatus downloadTaskStatus, String engineGid, String sourceType) {
        long now = System.currentTimeMillis();
        DownloadTaskDO downloadTaskDO = new DownloadTaskDO();
        downloadTaskDO.setTaskCode("TASK-" + now + "-" + engineGid);
        downloadTaskDO.setSourceType(sourceType);
        downloadTaskDO.setSourceUri("/tmp/" + engineGid + ".torrent");
        downloadTaskDO.setTorrentFilePath("/tmp/" + engineGid + ".torrent");
        downloadTaskDO.setDisplayName(engineGid + ".torrent");
        downloadTaskDO.setDomainStatus(downloadTaskStatus.name());
        downloadTaskDO.setEngineStatus("ACTIVE");
        downloadTaskDO.setEngineGid(engineGid);
        downloadTaskDO.setQueuePriority(100);
        downloadTaskDO.setSaveDir("./downloads");
        downloadTaskDO.setTotalSizeBytes(0L);
        downloadTaskDO.setCompletedSizeBytes(0L);
        downloadTaskDO.setDownloadSpeedBps(0L);
        downloadTaskDO.setUploadSpeedBps(0L);
        downloadTaskDO.setRetryCount(0);
        downloadTaskDO.setMaxRetryCount(3);
        downloadTaskDO.setClientRequestId("sync-" + engineGid);
        downloadTaskDO.setVersion(0);
        downloadTaskDO.setCreatedAt(now);
        downloadTaskDO.setUpdatedAt(now);
        return downloadTaskRepository.save(downloadTaskDO);
    }

    private void saveStateLog(
        Long taskId,
        String fromStatus,
        String toStatus,
        String triggerSource,
        String triggerType,
        long createdAt
    ) {
        TaskStateLogDO taskStateLogDO = new TaskStateLogDO();
        taskStateLogDO.setTaskId(taskId);
        taskStateLogDO.setFromStatus(fromStatus);
        taskStateLogDO.setToStatus(toStatus);
        taskStateLogDO.setTriggerSource(triggerSource);
        taskStateLogDO.setTriggerType(triggerType);
        taskStateLogDO.setRemark("测试状态日志");
        taskStateLogDO.setCreatedAt(createdAt);
        taskStateLogRepository.save(taskStateLogDO);
    }

    private Aria2TaskStatusDTO buildStatus(
        String gid,
        String status,
        String totalLength,
        String completedLength,
        String downloadSpeed,
        String uploadSpeed,
        String errorCode,
        String errorMessage
    ) {
        Aria2TaskStatusDTO aria2TaskStatusDTO = new Aria2TaskStatusDTO();
        aria2TaskStatusDTO.setGid(gid);
        aria2TaskStatusDTO.setStatus(status);
        aria2TaskStatusDTO.setTotalLength(totalLength);
        aria2TaskStatusDTO.setCompletedLength(completedLength);
        aria2TaskStatusDTO.setDownloadSpeed(downloadSpeed);
        aria2TaskStatusDTO.setUploadSpeed(uploadSpeed);
        aria2TaskStatusDTO.setErrorCode(errorCode);
        aria2TaskStatusDTO.setErrorMessage(errorMessage);
        return aria2TaskStatusDTO;
    }

    private Aria2TaskFileDTO buildTorrentFile(String index, String path, String length, String selected) {
        Aria2TaskFileDTO aria2TaskFileDTO = new Aria2TaskFileDTO();
        aria2TaskFileDTO.setIndex(index);
        aria2TaskFileDTO.setPath(path);
        aria2TaskFileDTO.setLength(length);
        aria2TaskFileDTO.setSelected(selected);
        return aria2TaskFileDTO;
    }

    private Path createDownloadedFile(long sizeBytes) {
        try {
            Path downloadedFile = Files.createTempFile("task-sync-downloaded", ".bin");
            Files.write(downloadedFile, new byte[Math.toIntExact(sizeBytes)]);
            return downloadedFile;
        } catch (Exception exception) {
            throw new IllegalStateException("创建测试下载文件失败", exception);
        }
    }
}
