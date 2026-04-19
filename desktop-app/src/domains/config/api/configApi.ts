import { requestJson } from "@/shared/api/httpClient";
import type { DownloadConfig, UpdateDownloadConfigPayload } from "@/domains/config/models/config";

export async function getDownloadConfig() {
  return requestJson<DownloadConfig>("/api/config");
}

export async function updateDownloadConfig(payload: UpdateDownloadConfigPayload) {
  return requestJson<DownloadConfig>("/api/config", {
    method: "PUT",
    body: payload
  });
}
