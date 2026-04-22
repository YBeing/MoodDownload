import { requestJson } from "@/shared/api/httpClient";
import type {
  BaiduPanPreflightPayload,
  BaiduPanPreflightResult,
  BaiduPanResolvePayload,
  BaiduPanResolveResult
} from "@/domains/provider/models/provider";

export async function preflightBaiduPan(payload: BaiduPanPreflightPayload) {
  return requestJson<BaiduPanPreflightResult>("/api/providers/baidupan/preflight", {
    method: "POST",
    body: payload
  });
}

export async function resolveBaiduPan(payload: BaiduPanResolvePayload) {
  return requestJson<BaiduPanResolveResult>("/api/providers/baidupan/resolve", {
    method: "POST",
    body: payload
  });
}
