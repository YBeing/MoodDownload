package com.mooddownload.local.service.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mooddownload.local.common.exception.BizException;
import com.mooddownload.local.service.task.model.CreateTaskCommand;
import com.mooddownload.local.service.task.model.DownloadTaskModel;
import com.mooddownload.local.service.task.state.DownloadTaskStatus;
import com.mooddownload.local.service.task.state.TaskTriggerType;
import org.junit.jupiter.api.Test;

/**
 * B3 阶段任务状态机单元测试。
 */
class TaskStateMachineServiceTests {

    private final TaskStateMachineService taskStateMachineService = new TaskStateMachineService();

    @Test
    void shouldInitializeTaskAsPending() {
        long now = 1000L;
        DownloadTaskModel downloadTaskModel = taskStateMachineService.initializeTask(buildCreateTaskCommand(), now);

        assertThat(downloadTaskModel.getTaskCode()).startsWith("TASK-" + now + "-");
        assertThat(downloadTaskModel.getDomainStatus()).isEqualTo(DownloadTaskStatus.PENDING.name());
        assertThat(downloadTaskModel.getEngineStatus()).isEqualTo("UNKNOWN");
        assertThat(downloadTaskModel.getRetryCount()).isEqualTo(0);
        assertThat(downloadTaskModel.getMaxRetryCount()).isEqualTo(3);
        assertThat(downloadTaskModel.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void shouldPausePendingTask() {
        DownloadTaskModel downloadTaskModel = buildTaskModel(DownloadTaskStatus.PENDING, 0, 3);

        taskStateMachineService.transit(downloadTaskModel, TaskTriggerType.PAUSE, 2000L);

        assertThat(downloadTaskModel.getDomainStatus()).isEqualTo(DownloadTaskStatus.PAUSED.name());
        assertThat(downloadTaskModel.getEngineStatus()).isEqualTo("PAUSED");
        assertThat(downloadTaskModel.getVersion()).isEqualTo(1);
    }

    @Test
    void shouldResumePausedTaskToPending() {
        DownloadTaskModel downloadTaskModel = buildTaskModel(DownloadTaskStatus.PAUSED, 0, 3);
        downloadTaskModel.setEngineStatus("PAUSED");

        taskStateMachineService.transit(downloadTaskModel, TaskTriggerType.RESUME, 2000L);

        assertThat(downloadTaskModel.getDomainStatus()).isEqualTo(DownloadTaskStatus.PENDING.name());
        assertThat(downloadTaskModel.getEngineStatus()).isEqualTo("UNKNOWN");
        assertThat(downloadTaskModel.getVersion()).isEqualTo(1);
    }

    @Test
    void shouldRetryFailedTaskAndIncreaseRetryCount() {
        DownloadTaskModel downloadTaskModel = buildTaskModel(DownloadTaskStatus.FAILED, 1, 3);
        downloadTaskModel.setErrorCode("ARIA2_ERROR");
        downloadTaskModel.setErrorMessage("rpc timeout");

        taskStateMachineService.transit(downloadTaskModel, TaskTriggerType.RETRY, 2000L);

        assertThat(downloadTaskModel.getDomainStatus()).isEqualTo(DownloadTaskStatus.PENDING.name());
        assertThat(downloadTaskModel.getRetryCount()).isEqualTo(2);
        assertThat(downloadTaskModel.getErrorCode()).isNull();
        assertThat(downloadTaskModel.getErrorMessage()).isNull();
    }

    @Test
    void shouldCancelCompletedTask() {
        DownloadTaskModel downloadTaskModel = buildTaskModel(DownloadTaskStatus.COMPLETED, 0, 3);
        downloadTaskModel.setEngineStatus("COMPLETE");

        taskStateMachineService.transit(downloadTaskModel, TaskTriggerType.CANCEL, 2000L);

        assertThat(downloadTaskModel.getDomainStatus()).isEqualTo(DownloadTaskStatus.CANCELLED.name());
        assertThat(downloadTaskModel.getEngineStatus()).isEqualTo("REMOVED");
    }

    @Test
    void shouldRejectInvalidTransition() {
        DownloadTaskModel downloadTaskModel = buildTaskModel(DownloadTaskStatus.COMPLETED, 0, 3);

        assertThatThrownBy(() -> taskStateMachineService.transit(downloadTaskModel, TaskTriggerType.PAUSE, 2000L))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("状态流转不允许");
    }

    @Test
    void shouldRejectRetryWhenReachMaxRetryCount() {
        DownloadTaskModel downloadTaskModel = buildTaskModel(DownloadTaskStatus.FAILED, 3, 3);

        assertThatThrownBy(() -> taskStateMachineService.transit(downloadTaskModel, TaskTriggerType.RETRY, 2000L))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("已达到最大重试次数");
    }

    private CreateTaskCommand buildCreateTaskCommand() {
        CreateTaskCommand createTaskCommand = new CreateTaskCommand();
        createTaskCommand.setClientRequestId("request-1");
        createTaskCommand.setSourceType("HTTP");
        createTaskCommand.setSourceUri("https://example.com/archive/file.iso");
        createTaskCommand.setSaveDir("./downloads");
        createTaskCommand.setClientType("electron");
        return createTaskCommand;
    }

    private DownloadTaskModel buildTaskModel(
        DownloadTaskStatus downloadTaskStatus,
        int retryCount,
        int maxRetryCount
    ) {
        DownloadTaskModel downloadTaskModel = new DownloadTaskModel();
        downloadTaskModel.setId(1L);
        downloadTaskModel.setTaskCode("TASK-1");
        downloadTaskModel.setSourceType("HTTP");
        downloadTaskModel.setSourceUri("https://example.com/file.iso");
        downloadTaskModel.setDisplayName("file.iso");
        downloadTaskModel.setDomainStatus(downloadTaskStatus.name());
        downloadTaskModel.setEngineStatus("UNKNOWN");
        downloadTaskModel.setQueuePriority(100);
        downloadTaskModel.setSaveDir("./downloads");
        downloadTaskModel.setTotalSizeBytes(0L);
        downloadTaskModel.setCompletedSizeBytes(0L);
        downloadTaskModel.setDownloadSpeedBps(0L);
        downloadTaskModel.setUploadSpeedBps(0L);
        downloadTaskModel.setRetryCount(retryCount);
        downloadTaskModel.setMaxRetryCount(maxRetryCount);
        downloadTaskModel.setClientRequestId("request-1");
        downloadTaskModel.setVersion(0);
        downloadTaskModel.setCreatedAt(1000L);
        downloadTaskModel.setUpdatedAt(1000L);
        return downloadTaskModel;
    }
}
