import { getRuntimeConfig } from "@/shared/api/httpClient";
import { createLogger } from "@/shared/utils/logger";
import type { TaskSseEvent } from "@/domains/task/models/task";

export type TaskStreamStatus = "idle" | "connecting" | "connected" | "reconnecting" | "error";

interface TaskEventEnvelope {
  event: string;
  data: TaskSseEvent;
}

interface TaskEventStreamHandlers {
  onStatusChange: (status: TaskStreamStatus, message: string) => void;
  onEvent: (event: TaskEventEnvelope) => void;
  onError: (error: unknown) => void;
}

const logger = createLogger("task-sse");
const retryDelays = [1200, 2400, 4800, 8000];

function buildSseHeaders() {
  const runtimeConfig = getRuntimeConfig();
  return {
    "X-Local-Token": runtimeConfig.localToken,
    "X-Client-Type": runtimeConfig.clientType,
    "X-Request-Id": globalThis.crypto?.randomUUID?.() || `${Date.now()}`
  };
}

function splitEventBlocks(buffer: string) {
  const delimiter = "\n\n";
  const normalizedBuffer = buffer.replace(/\r\n/g, "\n");
  const chunks = normalizedBuffer.split(delimiter);
  const rest = normalizedBuffer.endsWith(delimiter) ? "" : chunks.pop() || "";
  return {
    completed: chunks,
    rest
  };
}

function parseEventBlock(block: string): TaskEventEnvelope | null {
  const lines = block.split("\n");
  let eventName = "message";
  const dataLines: string[] = [];

  for (const line of lines) {
    if (line.startsWith("event:")) {
      eventName = line.slice(6).trim();
      continue;
    }
    if (line.startsWith("data:")) {
      dataLines.push(line.slice(5).trim());
    }
  }

  if (dataLines.length === 0) {
    return null;
  }

  return {
    event: eventName,
    data: JSON.parse(dataLines.join("\n")) as TaskSseEvent
  };
}

/**
 * 使用 fetch 流式读取 SSE，解决浏览器 EventSource 无法附带本地鉴权头的问题。
 *
 * @param handlers 连接状态与事件回调
 * @returns 可启动 / 停止的 SSE 客户端
 */
export function createTaskEventStream(handlers: TaskEventStreamHandlers) {
  let stopped = false;
  let activeController: AbortController | null = null;
  let retryTimer: number | null = null;
  let retryCount = 0;

  async function connect() {
    if (stopped) {
      return;
    }

    const runtimeConfig = getRuntimeConfig();
    handlers.onStatusChange(
      retryCount === 0 ? "connecting" : "reconnecting",
      retryCount === 0 ? "正在连接本地实时通道" : "实时连接已断开，正在重连"
    );

    activeController = new AbortController();

    try {
      const response = await fetch(new URL("/api/events/tasks", runtimeConfig.serviceUrl), {
        method: "GET",
        headers: buildSseHeaders(),
        signal: activeController.signal
      });

      if (!response.ok || !response.body) {
        throw new Error(`SSE 建连失败: ${response.status}`);
      }

      handlers.onStatusChange("connected", "实时连接正常");
      retryCount = 0;

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = "";

      while (!stopped) {
        const { value, done } = await reader.read();
        if (done) {
          break;
        }
        buffer += decoder.decode(value, { stream: true });
        const { completed, rest } = splitEventBlocks(buffer);
        buffer = rest;
        completed.forEach((chunk) => {
          const event = parseEventBlock(chunk);
          if (event) {
            handlers.onEvent(event);
          }
        });
      }

      if (!stopped) {
        scheduleReconnect(new Error("SSE 流已关闭"));
      }
    } catch (error) {
      if (stopped || (error instanceof DOMException && error.name === "AbortError")) {
        return;
      }
      handlers.onError(error);
      scheduleReconnect(error);
    }
  }

  function scheduleReconnect(error: unknown) {
    logger.warn("任务 SSE 将进入重连", error);
    handlers.onStatusChange("error", "实时连接中断");
    const delay = retryDelays[Math.min(retryCount, retryDelays.length - 1)];
    retryCount += 1;
    retryTimer = window.setTimeout(() => {
      void connect();
    }, delay);
  }

  return {
    start() {
      void connect();
    },
    stop() {
      stopped = true;
      if (retryTimer) {
        window.clearTimeout(retryTimer);
      }
      activeController?.abort();
      handlers.onStatusChange("idle", "实时连接已停止");
    }
  };
}
