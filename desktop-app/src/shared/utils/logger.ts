/**
 * 创建前端轻量日志器，用于 API、SSE 和关键交互链路的可观测性兜底。
 *
 * @param scope 日志作用域
 * @returns 日志方法集合
 */
export function createLogger(scope: string) {
  return {
    info(message: string, payload?: unknown) {
      console.info(`[MoodDownload][${scope}] ${message}`, payload ?? "");
    },
    warn(message: string, payload?: unknown) {
      console.warn(`[MoodDownload][${scope}] ${message}`, payload ?? "");
    },
    error(message: string, payload?: unknown) {
      console.error(`[MoodDownload][${scope}] ${message}`, payload ?? "");
    }
  };
}
