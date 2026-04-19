import { createContext, useContext, useEffect, useState, type PropsWithChildren } from "react";
import { createTaskEventStream, type TaskStreamStatus } from "@/shared/api/task-event-stream";
import { createLogger } from "@/shared/utils/logger";
import type { TaskSseEvent } from "@/domains/task/models/task";

interface TaskEventsContextValue {
  status: TaskStreamStatus;
  lastEvent: TaskSseEvent | null;
  statusMessage: string;
}

const logger = createLogger("task-events");

const TaskEventsContext = createContext<TaskEventsContextValue>({
  status: "idle",
  lastEvent: null,
  statusMessage: "实时连接未启动"
});

export function TaskEventsProvider({ children }: PropsWithChildren) {
  const [status, setStatus] = useState<TaskStreamStatus>("idle");
  const [lastEvent, setLastEvent] = useState<TaskSseEvent | null>(null);
  const [statusMessage, setStatusMessage] = useState("实时连接未启动");

  useEffect(() => {
    const stream = createTaskEventStream({
      onStatusChange(nextStatus, message) {
        setStatus(nextStatus);
        setStatusMessage(message);
      },
      onEvent(event) {
        if (event.event !== "task.updated" || !event.data?.taskId) {
          return;
        }
        setLastEvent(event.data);
      },
      onError(error) {
        logger.warn("任务 SSE 连接异常", error);
      }
    });

    stream.start();
    return () => {
      stream.stop();
    };
  }, []);

  return (
    <TaskEventsContext.Provider
      value={{
        status,
        lastEvent,
        statusMessage
      }}
    >
      {children}
    </TaskEventsContext.Provider>
  );
}

export function useTaskEvents() {
  return useContext(TaskEventsContext);
}
