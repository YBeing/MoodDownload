package com.mooddownload.local.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.mooddownload.local.bootstrap.LocalServiceApplication;
import com.mooddownload.local.dal.config.DownloadConfigRepository;
import com.mooddownload.local.dal.task.DownloadAttemptRepository;
import com.mooddownload.local.dal.task.DownloadTaskRepository;
import com.mooddownload.local.dal.task.TaskStateLogRepository;
import com.mooddownload.local.mapper.config.DownloadConfigDO;
import com.mooddownload.local.mapper.task.DownloadAttemptDO;
import com.mooddownload.local.mapper.task.DownloadTaskDO;
import com.mooddownload.local.mapper.task.TaskStateLogDO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

/**
 * B2 阶段持久层集成测试。
 */
@SpringBootTest(classes = LocalServiceApplication.class)
@ActiveProfiles("test")
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PersistenceLayerIntegrationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DownloadTaskRepository downloadTaskRepository;

    @Autowired
    private DownloadAttemptRepository downloadAttemptRepository;

    @Autowired
    private TaskStateLogRepository taskStateLogRepository;

    @Autowired
    private DownloadConfigRepository downloadConfigRepository;

    @Test
    void shouldInitializeAllCoreTables() {
        List<String> tableNames = jdbcTemplate.queryForList(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name IN "
                + "('t_download_task', 't_download_attempt', 't_download_config', 't_task_state_log')",
            String.class
        );

        assertThat(tableNames).containsExactlyInAnyOrder(
            "t_download_task",
            "t_download_attempt",
            "t_download_config",
            "t_task_state_log"
        );
    }

    @Test
    void shouldPersistAndReadTaskAggregateRecords() {
        long now = System.currentTimeMillis();
        DownloadTaskDO downloadTaskDO = buildDownloadTask(now);
        Long taskId = downloadTaskRepository.save(downloadTaskDO);

        DownloadTaskDO persistedTask = downloadTaskRepository.findById(taskId).orElseThrow(
            () -> new IllegalStateException("未查询到刚写入的下载任务")
        );
        assertThat(persistedTask.getTaskCode()).isEqualTo(downloadTaskDO.getTaskCode());
        assertThat(persistedTask.getDomainStatus()).isEqualTo("PENDING");

        DownloadAttemptDO downloadAttemptDO = buildDownloadAttempt(taskId, now);
        Long attemptId = downloadAttemptRepository.save(downloadAttemptDO);
        assertThat(attemptId).isNotNull();

        TaskStateLogDO taskStateLogDO = buildTaskStateLog(taskId, now);
        Long logId = taskStateLogRepository.save(taskStateLogDO);
        assertThat(logId).isNotNull();

        persistedTask.setDisplayName("updated-name.iso");
        persistedTask.setDomainStatus("RUNNING");
        persistedTask.setEngineStatus("ACTIVE");
        persistedTask.setEngineGid("gid-" + now);
        persistedTask.setCompletedSizeBytes(1024L);
        persistedTask.setDownloadSpeedBps(2048L);
        persistedTask.setRetryCount(1);
        persistedTask.setVersion(1);
        persistedTask.setLastSyncAt(now + 100L);
        persistedTask.setUpdatedAt(now + 100L);
        downloadTaskRepository.updateCoreSnapshot(persistedTask);

        DownloadTaskDO updatedTask = downloadTaskRepository.findByClientRequestId(
            downloadTaskDO.getClientRequestId()
        ).orElseThrow(() -> new IllegalStateException("未按幂等键查询到下载任务"));
        assertThat(updatedTask.getDomainStatus()).isEqualTo("RUNNING");
        assertThat(updatedTask.getEngineStatus()).isEqualTo("ACTIVE");
        assertThat(updatedTask.getEngineGid()).isEqualTo("gid-" + now);

        List<DownloadAttemptDO> attempts = downloadAttemptRepository.listByTaskId(taskId);
        assertThat(attempts).hasSize(1);
        assertThat(attempts.get(0).getAttemptNo()).isEqualTo(1);

        List<TaskStateLogDO> stateLogs = taskStateLogRepository.listByTaskId(taskId);
        assertThat(stateLogs).hasSize(1);
        assertThat(stateLogs.get(0).getToStatus()).isEqualTo("PENDING");
    }

    @Test
    void shouldReadAndUpdateSingletonConfig() {
        DownloadConfigDO downloadConfigDO = downloadConfigRepository.findSingleton().orElseThrow(
            () -> new IllegalStateException("未初始化默认下载配置")
        );
        assertThat(downloadConfigDO.getId()).isEqualTo(1);
        assertThat(downloadConfigDO.getDefaultSaveDir()).isEqualTo("./downloads");

        downloadConfigDO.setDefaultSaveDir("./downloads/updated");
        downloadConfigDO.setMaxConcurrentDownloads(5);
        downloadConfigDO.setMaxGlobalDownloadSpeed(1024);
        downloadConfigDO.setClipboardMonitorEnabled(0);
        downloadConfigDO.setUpdatedAt(System.currentTimeMillis());
        downloadConfigRepository.saveOrUpdate(downloadConfigDO);

        DownloadConfigDO updatedConfig = downloadConfigRepository.findSingleton().orElseThrow(
            () -> new IllegalStateException("更新后未查询到下载配置")
        );
        assertThat(updatedConfig.getDefaultSaveDir()).isEqualTo("./downloads/updated");
        assertThat(updatedConfig.getMaxConcurrentDownloads()).isEqualTo(5);
        assertThat(updatedConfig.getMaxGlobalDownloadSpeed()).isEqualTo(1024);
        assertThat(updatedConfig.getClipboardMonitorEnabled()).isEqualTo(0);
    }

    private DownloadTaskDO buildDownloadTask(long now) {
        DownloadTaskDO downloadTaskDO = new DownloadTaskDO();
        downloadTaskDO.setTaskCode("TASK-" + now);
        downloadTaskDO.setSourceType("HTTP");
        downloadTaskDO.setSourceUri("https://example.com/file-" + now + ".iso");
        downloadTaskDO.setSourceHash("hash-" + now);
        downloadTaskDO.setDisplayName("file-" + now + ".iso");
        downloadTaskDO.setDomainStatus("PENDING");
        downloadTaskDO.setEngineStatus("UNKNOWN");
        downloadTaskDO.setQueuePriority(100);
        downloadTaskDO.setSaveDir("./downloads");
        downloadTaskDO.setTotalSizeBytes(0L);
        downloadTaskDO.setCompletedSizeBytes(0L);
        downloadTaskDO.setDownloadSpeedBps(0L);
        downloadTaskDO.setUploadSpeedBps(0L);
        downloadTaskDO.setRetryCount(0);
        downloadTaskDO.setMaxRetryCount(3);
        downloadTaskDO.setClientRequestId("request-" + now);
        downloadTaskDO.setVersion(0);
        downloadTaskDO.setCreatedAt(now);
        downloadTaskDO.setUpdatedAt(now);
        return downloadTaskDO;
    }

    private DownloadAttemptDO buildDownloadAttempt(Long taskId, long now) {
        DownloadAttemptDO downloadAttemptDO = new DownloadAttemptDO();
        downloadAttemptDO.setTaskId(taskId);
        downloadAttemptDO.setAttemptNo(1);
        downloadAttemptDO.setTriggerReason("CREATE");
        downloadAttemptDO.setResultStatus("SUCCESS");
        downloadAttemptDO.setEngineGid("gid-attempt-" + now);
        downloadAttemptDO.setStartedAt(now);
        downloadAttemptDO.setFinishedAt(now + 10L);
        downloadAttemptDO.setCreatedAt(now);
        downloadAttemptDO.setUpdatedAt(now);
        return downloadAttemptDO;
    }

    private TaskStateLogDO buildTaskStateLog(Long taskId, long now) {
        TaskStateLogDO taskStateLogDO = new TaskStateLogDO();
        taskStateLogDO.setTaskId(taskId);
        taskStateLogDO.setFromStatus(null);
        taskStateLogDO.setToStatus("PENDING");
        taskStateLogDO.setTriggerSource("SYSTEM");
        taskStateLogDO.setTriggerType("CREATE");
        taskStateLogDO.setRemark("初始化任务");
        taskStateLogDO.setCreatedAt(now);
        return taskStateLogDO;
    }
}
