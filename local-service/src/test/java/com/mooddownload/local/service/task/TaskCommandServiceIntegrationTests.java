package com.mooddownload.local.service.task;

import static org.assertj.core.api.Assertions.assertThat;

import com.mooddownload.local.bootstrap.LocalServiceApplication;
import com.mooddownload.local.dal.task.DownloadTaskRepository;
import com.mooddownload.local.dal.task.TaskStateLogRepository;
import com.mooddownload.local.mapper.task.DownloadTaskDO;
import com.mooddownload.local.mapper.task.TaskStateLogDO;
import com.mooddownload.local.service.task.model.CreateTaskCommand;
import com.mooddownload.local.service.task.model.DownloadTaskModel;
import com.mooddownload.local.service.task.model.TaskOperationResult;
import com.mooddownload.local.service.task.state.DownloadTaskStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

/**
 * B3 阶段任务命令服务集成测试。
 */
@SpringBootTest(classes = LocalServiceApplication.class)
@ActiveProfiles("test")
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TaskCommandServiceIntegrationTests {

    @Autowired
    private TaskCommandService taskCommandService;

    @Autowired
    private TaskQueryService taskQueryService;

    @Autowired
    private DownloadTaskRepository downloadTaskRepository;

    @Autowired
    private TaskStateLogRepository taskStateLogRepository;

    @Test
    void shouldCreateTaskAndReturnIdempotentResultForDuplicateRequest() {
        CreateTaskCommand createTaskCommand = buildCreateTaskCommand("request-create");

        TaskOperationResult firstResult = taskCommandService.createTask(createTaskCommand);
        TaskOperationResult duplicateResult = taskCommandService.createTask(createTaskCommand);

        assertThat(firstResult.isIdempotent()).isFalse();
        assertThat(firstResult.getTaskDomainEvent()).isNotNull();
        assertThat(firstResult.getTaskModel().getId()).isNotNull();
        assertThat(duplicateResult.isIdempotent()).isTrue();
        assertThat(duplicateResult.getTaskModel().getId()).isEqualTo(firstResult.getTaskModel().getId());

        DownloadTaskModel persistedTask = taskQueryService.getTaskByClientRequestId("request-create");
        assertThat(persistedTask.getDomainStatus()).isEqualTo(DownloadTaskStatus.PENDING.name());

        List<TaskStateLogDO> stateLogs = taskStateLogRepository.listByTaskId(firstResult.getTaskModel().getId());
        assertThat(stateLogs).hasSize(1);
        assertThat(stateLogs.get(0).getTriggerType()).isEqualTo("CREATE");
    }

    @Test
    void shouldReuseExistingTaskWhenBtSourceHashMatches() {
        CreateTaskCommand firstCommand = buildCreateTaskCommand("request-magnet-first");
        firstCommand.setSourceType("MAGNET");
        firstCommand.setSourceUri("magnet:?xt=urn:btih:C8295CE630F2064F08440DB1534E4992CFE4862A");
        firstCommand.setSourceHash("C8295CE630F2064F08440DB1534E4992CFE4862A");
        firstCommand.setDisplayName("demo-magnet");

        CreateTaskCommand duplicateCommand = buildCreateTaskCommand("request-magnet-second");
        duplicateCommand.setSourceType("MAGNET");
        duplicateCommand.setSourceUri("magnet:?xt=urn:btih:c8295ce630f2064f08440db1534e4992cfe4862a&dn=demo");
        duplicateCommand.setSourceHash("C8295CE630F2064F08440DB1534E4992CFE4862A");
        duplicateCommand.setDisplayName("demo-magnet-duplicate");

        TaskOperationResult firstResult = taskCommandService.createTask(firstCommand);
        TaskOperationResult duplicateResult = taskCommandService.createTask(duplicateCommand);

        assertThat(firstResult.isIdempotent()).isFalse();
        assertThat(duplicateResult.isIdempotent()).isTrue();
        assertThat(duplicateResult.getTaskModel().getId()).isEqualTo(firstResult.getTaskModel().getId());
        assertThat(downloadTaskRepository.countByCondition(null, null)).isEqualTo(1);
    }

    @Test
    void shouldPauseAndResumeTask() {
        Long taskId = seedTask(DownloadTaskStatus.RUNNING, 0, 3, "request-running");

        TaskOperationResult pauseResult = taskCommandService.pauseTask(taskId);
        TaskOperationResult resumeResult = taskCommandService.resumeTask(taskId);

        assertThat(pauseResult.isIdempotent()).isFalse();
        assertThat(pauseResult.getTaskModel().getDomainStatus()).isEqualTo(DownloadTaskStatus.PAUSED.name());
        assertThat(resumeResult.isIdempotent()).isFalse();
        assertThat(resumeResult.getTaskModel().getDomainStatus()).isEqualTo(DownloadTaskStatus.RUNNING.name());

        DownloadTaskModel persistedTask = taskQueryService.getTaskById(taskId);
        assertThat(persistedTask.getDomainStatus()).isEqualTo(DownloadTaskStatus.RUNNING.name());
        assertThat(persistedTask.getVersion()).isEqualTo(2);

        List<TaskStateLogDO> stateLogs = taskStateLogRepository.listByTaskId(taskId);
        assertThat(stateLogs).hasSize(2);
        assertThat(stateLogs.get(0).getToStatus()).isEqualTo("PAUSED");
        assertThat(stateLogs.get(1).getToStatus()).isEqualTo("RUNNING");
    }

    @Test
    void shouldRetryFailedTask() {
        Long taskId = seedTask(DownloadTaskStatus.FAILED, 1, 3, "request-failed");

        TaskOperationResult retryResult = taskCommandService.retryTask(taskId);

        assertThat(retryResult.isIdempotent()).isFalse();
        assertThat(retryResult.getTaskModel().getDomainStatus()).isEqualTo(DownloadTaskStatus.PENDING.name());
        assertThat(retryResult.getTaskModel().getRetryCount()).isEqualTo(2);

        DownloadTaskModel persistedTask = taskQueryService.getTaskById(taskId);
        assertThat(persistedTask.getDomainStatus()).isEqualTo(DownloadTaskStatus.PENDING.name());
        assertThat(persistedTask.getRetryCount()).isEqualTo(2);

        List<TaskStateLogDO> stateLogs = taskStateLogRepository.listByTaskId(taskId);
        assertThat(stateLogs).hasSize(1);
        assertThat(stateLogs.get(0).getTriggerType()).isEqualTo("RETRY");
    }

    private CreateTaskCommand buildCreateTaskCommand(String clientRequestId) {
        CreateTaskCommand createTaskCommand = new CreateTaskCommand();
        createTaskCommand.setClientRequestId(clientRequestId);
        createTaskCommand.setSourceType("HTTP");
        createTaskCommand.setSourceUri("https://example.com/" + clientRequestId + ".iso");
        createTaskCommand.setDisplayName(clientRequestId + ".iso");
        createTaskCommand.setSaveDir("./downloads");
        createTaskCommand.setMaxRetryCount(3);
        createTaskCommand.setClientType("electron");
        return createTaskCommand;
    }

    private Long seedTask(
        DownloadTaskStatus downloadTaskStatus,
        int retryCount,
        int maxRetryCount,
        String clientRequestId
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
        downloadTaskDO.setSaveDir("./downloads");
        downloadTaskDO.setTotalSizeBytes(0L);
        downloadTaskDO.setCompletedSizeBytes(0L);
        downloadTaskDO.setDownloadSpeedBps(0L);
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
}
