import { requestJson } from "@/shared/api/httpClient";
import type {
  BtTrackerSet,
  DownloadConfig,
  EngineRuntimeSnapshot,
  UpdateBtTrackerSetPayload,
  UpdateDownloadConfigPayload,
  UpdateEngineRuntimeProfilePayload
} from "@/domains/config/models/config";

export async function getDownloadConfig() {
  return requestJson<DownloadConfig>("/api/config");
}

export async function updateDownloadConfig(payload: UpdateDownloadConfigPayload) {
  return requestJson<DownloadConfig>("/api/config", {
    method: "PUT",
    body: payload
  });
}

export async function getEngineRuntimeSnapshot() {
  return requestJson<EngineRuntimeSnapshot>("/api/config/engine-runtime");
}

export async function updateEngineRuntimeProfile(payload: UpdateEngineRuntimeProfilePayload) {
  return requestJson<EngineRuntimeSnapshot>("/api/config/engine-runtime", {
    method: "PUT",
    body: payload
  });
}

export async function listTrackerSets() {
  return requestJson<BtTrackerSet[]>("/api/config/tracker-sets");
}

export async function updateTrackerSet(trackerSetCode: string, payload: UpdateBtTrackerSetPayload) {
  return requestJson<BtTrackerSet>(`/api/config/tracker-sets/${trackerSetCode}`, {
    method: "PUT",
    body: payload
  });
}
