package com.mooddownload.local.service.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 失败任务重试骨架。
 *
 * <p>B4 先保留失败任务重试入口，后续在 B5/B7 补充自动重试策略和限流控制。
 */
@Component
public class TaskRetryScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskRetryScheduler.class);

    /**
     * 扫描失败任务并准备重试。
     */
    public void retryFailedTasks() {
        LOGGER.debug("B3 重试骨架已就绪，B4 将在此补充失败任务扫描与引擎重试逻辑");
    }
}
