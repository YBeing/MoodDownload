package com.mooddownload.local.controller.task;

import com.mooddownload.local.service.task.TaskEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 任务 SSE 事件控制器。
 */
@RestController
@RequestMapping("/api/events/tasks")
public class TaskEventController {

    private final TaskEventPublisher taskEventPublisher;

    public TaskEventController(TaskEventPublisher taskEventPublisher) {
        this.taskEventPublisher = taskEventPublisher;
    }

    /**
     * 建立任务事件 SSE 连接。
     *
     * @return SSE 发射器
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTaskEvents() {
        return taskEventPublisher.subscribe();
    }
}
