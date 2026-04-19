package com.mooddownload.local.service.task;

import com.mooddownload.local.common.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * aria2 轮询同步定时任务。
 */
@Component
@Profile("!test")
public class TaskSyncScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskSyncScheduler.class);

    private final TaskSyncService taskSyncService;

    public TaskSyncScheduler(TaskSyncService taskSyncService) {
        this.taskSyncService = taskSyncService;
    }

    /**
     * 定时拉取 aria2 当前快照并回写本地任务。
     */
    @Scheduled(
        fixedDelayString = "${mooddownload.aria2.sync-interval-millis:3000}",
        initialDelayString = "${mooddownload.aria2.sync-initial-delay-millis:1000}"
    )
    public void synchronizeTasks() {
        try {
            taskSyncService.synchronizeTasks();
        } catch (BizException exception) {
            LOGGER.warn("aria2 定时同步失败: message={}", exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            LOGGER.error("aria2 定时同步出现未预期异常", exception);
        }
    }
}
