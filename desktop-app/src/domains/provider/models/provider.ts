export interface BaiduPanPreflightPayload {
  shareUrl?: string;
  authContext?: string;
}

export interface BaiduPanPreflightResult {
  capability: string;
  riskFlags: string[];
  suggestedNextStep: string;
}

export interface BaiduPanResolvePayload {
  providerContext: string;
}

export interface BaiduPanResolveResult {
  resolvedMode: string;
  nextStep: string;
}
