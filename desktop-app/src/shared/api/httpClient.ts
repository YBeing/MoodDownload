import type { RuntimeConfig } from "@/shared/types/runtime";
import { ApiClientError, type ApiResponse, type RequestOptions } from "@/shared/api/types";
import { createLogger } from "@/shared/utils/logger";

const logger = createLogger("http-client");

/**
 * 读取运行时配置，优先使用 Electron 预加载桥接，浏览器预览场景退回到 Vite 环境变量。
 *
 * @returns 本地服务连接配置
 */
export function getRuntimeConfig(): RuntimeConfig {
  if (window.moodDownloadBridge?.runtime) {
    return window.moodDownloadBridge.runtime;
  }
  return {
    serviceUrl: import.meta.env.VITE_LOCAL_SERVICE_URL || "http://127.0.0.1:18080",
    localToken: import.meta.env.VITE_LOCAL_SERVICE_TOKEN || "dev-local-token",
    clientType: "desktop-app",
    platform: "browser"
  };
}

function buildHeaders(body: RequestOptions["body"], runtimeConfig: RuntimeConfig, headers?: Record<string, string>) {
  const requestHeaders = new Headers(headers);
  requestHeaders.set("X-Local-Token", runtimeConfig.localToken);
  requestHeaders.set("X-Client-Type", runtimeConfig.clientType);
  requestHeaders.set("X-Request-Id", globalThis.crypto?.randomUUID?.() || `${Date.now()}`);
  if (body && !(body instanceof FormData) && !requestHeaders.has("Content-Type")) {
    requestHeaders.set("Content-Type", "application/json");
  }
  return requestHeaders;
}

/**
 * 发起统一 REST 请求并将本地服务响应转换为前端可消费的数据或异常。
 *
 * @param path 接口路径
 * @param options 请求选项
 * @returns 响应数据
 */
export async function requestJson<T>(path: string, options: RequestOptions = {}) {
  const runtimeConfig = getRuntimeConfig();
  const url = new URL(path, runtimeConfig.serviceUrl);
  const response = await fetch(url, {
    method: options.method || "GET",
    headers: buildHeaders(options.body, runtimeConfig, options.headers),
    body:
      options.body && !(options.body instanceof FormData) && typeof options.body !== "string"
        ? JSON.stringify(options.body)
        : (options.body as BodyInit | null | undefined),
    signal: options.signal
  });

  let payload: ApiResponse<T> | null = null;
  try {
    payload = (await response.json()) as ApiResponse<T>;
  } catch (error) {
    logger.error("解析接口响应失败", {
      path,
      error
    });
    throw new ApiClientError("本地服务响应格式异常", "RESPONSE_PARSE_FAILED", response.status);
  }

  if (!response.ok || payload.code !== "0") {
    logger.warn("接口调用失败", {
      path,
      status: response.status,
      code: payload.code,
      message: payload.message
    });
    throw new ApiClientError(payload.message, payload.code, response.status, payload.requestId);
  }

  return payload.data;
}
