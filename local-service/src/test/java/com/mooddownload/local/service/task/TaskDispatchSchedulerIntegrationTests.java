package com.mooddownload.local.service.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.mooddownload.local.bootstrap.LocalServiceApplication;
import com.mooddownload.local.client.aria2.Aria2RpcClient;
import com.mooddownload.local.common.enums.ErrorCode;
import com.mooddownload.local.common.exception.BizException;
import com.mooddownload.local.dal.task.DownloadAttemptRepository;
import com.mooddownload.local.dal.task.DownloadTaskRepository;
import com.mooddownload.local.dal.task.TaskStateLogRepository;
import com.mooddownload.local.mapper.task.DownloadAttemptDO;
import com.mooddownload.local.mapper.task.DownloadTaskDO;
import com.mooddownload.local.mapper.task.TaskStateLogDO;
import com.mooddownload.local.service.task.model.DownloadTaskModel;
import com.mooddownload.local.service.task.state.DownloadTaskStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

/**
 * B4 阶段调度器集成测试。
 */
@SpringBootTest(classes = LocalServiceApplication.class)
@ActiveProfiles("test")
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TaskDispatchSchedulerIntegrationTests {

    @Autowired
    private TaskDispatchScheduler taskDispatchScheduler;

    @Autowired
    private TaskQueryService taskQueryService;

    @Autowired
    private DownloadTaskRepository downloadTaskRepository;

    @Autowired
    private DownloadAttemptRepository downloadAttemptRepository;

    @Autowired
    private TaskStateLogRepository taskStateLogRepository;

    @MockBean
    private Aria2RpcClient aria2RpcClient;

    @Test
    void shouldDispatchPendingTaskToRunning() {
        Long taskId = seedPendingTask("request-dispatch-success");
        when(aria2RpcClient.addUri("https://example.com/request-dispatch-success.iso", "./downloads",
            null)).thenReturn("gid-success");

        taskDispatchScheduler.dispatchPendingTasks();

        DownloadTaskModel persistedTask = taskQueryService.getTaskById(taskId);
        assertThat(persistedTask.getDomainStatus()).isEqualTo(DownloadTaskStatus.RUNNING.name());
        assertThat(persistedTask.getEngineStatus()).isEqualTo("ACTIVE");
        assertThat(persistedTask.getEngineGid()).isEqualTo("gid-success");

        List<DownloadAttemptDO> attempts = downloadAttemptRepository.listByTaskId(taskId);
        assertThat(attempts).hasSize(1);
        assertThat(attempts.get(0).getResultStatus()).isEqualTo("SUCCESS");

        List<TaskStateLogDO> stateLogs = taskStateLogRepository.listByTaskId(taskId);
        assertThat(stateLogs).hasSize(2);
        assertThat(stateLogs.get(0).getToStatus()).isEqualTo("DISPATCHING");
        assertThat(stateLogs.get(1).getToStatus()).isEqualTo("RUNNING");
    }

    @Test
    void shouldFallbackToPendingWhenAria2CallFails() {
        Long taskId = seedPendingTask("request-dispatch-failed");
        when(aria2RpcClient.addUri("https://example.com/request-dispatch-failed.iso", "./downloads",
            null)).thenThrow(
                new BizException(ErrorCode.EXTERNAL_ENGINE_ERROR, "aria2 unavailable")
            );

        taskDispatchScheduler.dispatchPendingTasks();

        DownloadTaskModel persistedTask = taskQueryService.getTaskById(taskId);
        assertThat(persistedTask.getDomainStatus()).isEqualTo(DownloadTaskStatus.PENDING.name());
        assertThat(persistedTask.getErrorCode()).isEqualTo(ErrorCode.EXTERNAL_ENGINE_ERROR.getCode());
        assertThat(persistedTask.getErrorMessage()).isEqualTo("aria2 unavailable");

        List<DownloadAttemptDO> attempts = downloadAttemptRepository.listByTaskId(taskId);
        assertThat(attempts).hasSize(1);
        assertThat(attempts.get(0).getResultStatus()).isEqualTo("FAILED");

        List<TaskStateLogDO> stateLogs = taskStateLogRepository.listByTaskId(taskId);
        assertThat(stateLogs).hasSize(2);
        assertThat(stateLogs.get(0).getToStatus()).isEqualTo("DISPATCHING");
        assertThat(stateLogs.get(1).getToStatus()).isEqualTo("PENDING");
    }

    @Test
    void shouldFailFinallyWhenBtInfoHashAlreadyRegistered() {
        Long taskId = seedPendingTask("request-dispatch-duplicate-bt", "MAGNET",
            "magnet:?xt=urn:btih:C8295CE630F2064F08440DB1534E4992CFE4862A", "duplicate-bt");
        when(aria2RpcClient.addUri(
            "magnet:?xt=urn:btih:C8295CE630F2064F08440DB1534E4992CFE4862A",
            "./downloads",
            "duplicate-bt"
        )).thenThrow(new BizException(
            ErrorCode.EXTERNAL_ENGINE_ERROR,
            "aria2 RPC 调用失败: InfoHash c8295ce630f2064f08440db1534e4992cfe4862a is already registered."
        ));

        taskDispatchScheduler.dispatchPendingTasks();

        DownloadTaskModel persistedTask = taskQueryService.getTaskById(taskId);
        assertThat(persistedTask.getDomainStatus()).isEqualTo(DownloadTaskStatus.FAILED.name());
        assertThat(persistedTask.getErrorCode()).isEqualTo(ErrorCode.STATE_CONFLICT.getCode());
        assertThat(persistedTask.getErrorMessage()).isEqualTo("BT 任务已存在于下载引擎，请勿重复导入");

        List<TaskStateLogDO> stateLogs = taskStateLogRepository.listByTaskId(taskId);
        assertThat(stateLogs).hasSize(2);
        assertThat(stateLogs.get(0).getToStatus()).isEqualTo("DISPATCHING");
        assertThat(stateLogs.get(1).getToStatus()).isEqualTo("FAILED");
    }

    private Long seedPendingTask(String clientRequestId) {
        return seedPendingTask(
            clientRequestId,
            "HTTP",
            "https://example.com/" + clientRequestId + ".iso",
            clientRequestId + ".iso"
        );
    }

    private Long seedPendingTask(String clientRequestId, String sourceType, String sourceUri, String displayName) {
        long now = System.currentTimeMillis();
        DownloadTaskDO downloadTaskDO = new DownloadTaskDO();
        downloadTaskDO.setTaskCode("TASK-" + now + "-" + clientRequestId);
        downloadTaskDO.setSourceType(sourceType);
        downloadTaskDO.setSourceUri(sourceUri);
        downloadTaskDO.setDisplayName(displayName);
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
        downloadTaskDO.setClientRequestId(clientRequestId);
        downloadTaskDO.setVersion(0);
        downloadTaskDO.setCreatedAt(now);
        downloadTaskDO.setUpdatedAt(now);
        return downloadTaskRepository.save(downloadTaskDO);
    }
}
