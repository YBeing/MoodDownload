package com.mooddownload.local.service.task;

import com.mooddownload.local.service.task.event.TaskSseEvent;
import com.mooddownload.local.service.task.model.DownloadTaskModel;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 任务 SSE 事件发布器。
 */
@Component
public class TaskEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskEventPublisher.class);

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<SseEmitter>();

    /**
     * 注册 SSE 订阅。
     *
     * @return SSE 发射器
     */
    public SseEmitter subscribe() {
        SseEmitter sseEmitter = new SseEmitter(0L);
        emitters.add(sseEmitter);
        sseEmitter.onCompletion(() -> emitters.remove(sseEmitter));
        sseEmitter.onTimeout(() -> {
            emitters.remove(sseEmitter);
            sseEmitter.complete();
        });
        sseEmitter.onError(throwable -> emitters.remove(sseEmitter));
        try {
            sseEmitter.send(SseEmitter.event()
                .name("stream.ready")
                .data(buildReadyEvent()));
        } catch (IOException exception) {
            LOGGER.warn("发送任务 SSE 首包失败，将移除订阅", exception);
            emitters.remove(sseEmitter);
            sseEmitter.completeWithError(exception);
            return sseEmitter;
        }
        LOGGER.info("注册任务 SSE 订阅成功: emitterCount={}", emitters.size());
        return sseEmitter;
    }

    /**
     * 发布任务更新事件。
     *
     * @param downloadTaskModel 任务快照
     */
    public void publishTaskUpdated(DownloadTaskModel downloadTaskModel) {
        TaskSseEvent taskSseEvent = buildTaskUpdatedEvent(downloadTaskModel);
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                    .name(taskSseEvent.getEventType())
                    .data(taskSseEvent));
            } catch (IOException exception) {
                LOGGER.warn("发送任务 SSE 事件失败，将移除订阅: taskId={}", taskSseEvent.getTaskId(), exception);
                emitter.completeWithError(exception);
                emitters.remove(emitter);
            }
        }
        LOGGER.info("发布任务 SSE 事件完成: taskId={}, eventType={}, emitterCount={}",
            taskSseEvent.getTaskId(), taskSseEvent.getEventType(), emitters.size());
    }

    /**
     * 主动关闭全部订阅，供测试和停机阶段使用。
     */
    public void completeAllEmitters() {
        for (SseEmitter emitter : emitters) {
            emitter.complete();
        }
        emitters.clear();
    }

    /**
     * 构造 SSE 建连首包，确保前端在没有任务事件时也能及时感知连接已建立。
     *
     * @return SSE 首包事件
     */
    private TaskSseEvent buildReadyEvent() {
        TaskSseEvent taskSseEvent = new TaskSseEvent();
        taskSseEvent.setEventType("stream.ready");
        taskSseEvent.setTimestamp(System.currentTimeMillis());
        return taskSseEvent;
    }

    private TaskSseEvent buildTaskUpdatedEvent(DownloadTaskModel downloadTaskModel) {
        TaskSseEvent taskSseEvent = new TaskSseEvent();
        taskSseEvent.setEventType("task.updated");
        taskSseEvent.setTaskId(downloadTaskModel.getId());
        taskSseEvent.setTaskCode(downloadTaskModel.getTaskCode());
        taskSseEvent.setDomainStatus(downloadTaskModel.getDomainStatus());
        taskSseEvent.setEngineStatus(downloadTaskModel.getEngineStatus());
        taskSseEvent.setProgress(calculateProgress(downloadTaskModel));
        taskSseEvent.setDownloadSpeedBps(downloadTaskModel.getDownloadSpeedBps());
        taskSseEvent.setTimestamp(System.currentTimeMillis());
        return taskSseEvent;
    }

    private Double calculateProgress(DownloadTaskModel downloadTaskModel) {
        long totalSizeBytes = downloadTaskModel.getTotalSizeBytes() == null ? 0L : downloadTaskModel.getTotalSizeBytes();
        long completedSizeBytes = downloadTaskModel.getCompletedSizeBytes() == null
            ? 0L
            : downloadTaskModel.getCompletedSizeBytes();
        if (totalSizeBytes <= 0L) {
            return 0D;
        }
        return BigDecimal.valueOf(completedSizeBytes)
            .divide(BigDecimal.valueOf(totalSizeBytes), 4, RoundingMode.HALF_UP)
            .doubleValue();
    }
}
