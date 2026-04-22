const DEFAULT_CONFIG = {
  serviceUrl: "http://127.0.0.1:18080",
  localToken: "change-me-in-prod",
  clientType: "browser-extension",
  autoCaptureMagnet: true,
  autoCaptureTorrent: true,
  autoCaptureDownloadButton: true,
  captureViaContextMenu: true
};

async function getConfig() {
  const stored = await chrome.storage.local.get(DEFAULT_CONFIG);
  return { ...DEFAULT_CONFIG, ...stored };
}

function detectBrowserCode() {
  const userAgent = navigator.userAgent || "";
  if (userAgent.includes("Edg/")) {
    return "edge";
  }
  return "chrome";
}

function buildHeaders(config) {
  return {
    "Content-Type": "application/json",
    "X-Local-Token": config.localToken,
    "X-Client-Type": config.clientType,
    "X-Request-Id": crypto.randomUUID()
  };
}

async function saveLastResult(payload) {
  await chrome.storage.local.set({
    lastCaptureResult: {
      ...payload,
      updatedAt: Date.now()
    }
  });
}

async function pingLocalService() {
  const config = await getConfig();
  const response = await fetch(new URL("/actuator/health", config.serviceUrl), {
    method: "GET"
  });
  if (!response.ok) {
    throw new Error(`本地服务不可用: HTTP ${response.status}`);
  }
  return response.json();
}

async function captureDownload(payload) {
  const config = await getConfig();
  const response = await fetch(new URL("/api/extension/capture", config.serviceUrl), {
    method: "POST",
    headers: buildHeaders(config),
    body: JSON.stringify({
      clientRequestId: crypto.randomUUID(),
      browser: detectBrowserCode(),
      tabUrl: payload.tabUrl || "",
      downloadUrl: payload.downloadUrl,
      suggestedName: payload.suggestedName || "",
      referer: payload.referer || payload.tabUrl || "",
      userAgent: navigator.userAgent,
      headerSnapshotJson: JSON.stringify(payload.headerSnapshot || {})
    })
  });

  const result = await response.json();
  if (!response.ok || result.code !== "0") {
    const message = result?.message || "接管请求失败";
    await saveLastResult({
      success: false,
      message,
      downloadUrl: payload.downloadUrl
    });
    throw new Error(message);
  }

  await saveLastResult({
    success: true,
    message: `已转发到 MoodDownload，任务 ${result.data.taskCode || result.data.taskId || ""}`.trim(),
    downloadUrl: payload.downloadUrl,
    taskId: result.data.taskId || null,
    resolvedSourceType: result.data.resolvedSourceType || "",
    siteRuleMatched: Boolean(result.data.siteRuleMatched)
  });
  return result.data;
}

function normalizeSuggestedName(linkUrl) {
  try {
    const parsedUrl = new URL(linkUrl);
    const rawName = parsedUrl.pathname.split("/").pop() || "";
    return decodeURIComponent(rawName);
  } catch (_error) {
    return "";
  }
}

function isTorrentLike(url) {
  return /\.torrent(\?.*)?$/i.test(url);
}

function isMagnetLike(url) {
  return /^magnet:\?/i.test(url);
}

async function handleCaptureMessage(message, sender) {
  if (message?.type !== "capture-download") {
    return null;
  }

  const capturePayload = {
    downloadUrl: message.downloadUrl,
    tabUrl: message.tabUrl || sender.tab?.url || "",
    suggestedName: message.suggestedName || normalizeSuggestedName(message.downloadUrl),
    referer: message.referer || sender.tab?.url || "",
    headerSnapshot: message.headerSnapshot || {}
  };

  const data = await captureDownload(capturePayload);
  return {
    ok: true,
    data
  };
}

async function createContextMenus() {
  await chrome.contextMenus.removeAll();
  chrome.contextMenus.create({
    id: "capture-link",
    title: "发送到 MoodDownload",
    contexts: ["link"]
  });
  chrome.contextMenus.create({
    id: "capture-page-url",
    title: "发送当前地址到 MoodDownload",
    contexts: ["page"]
  });
}

chrome.runtime.onInstalled.addListener(() => {
  void createContextMenus();
});

chrome.runtime.onStartup.addListener(() => {
  void createContextMenus();
});

chrome.contextMenus.onClicked.addListener((info, tab) => {
  void (async () => {
    const config = await getConfig();
    if (!config.captureViaContextMenu) {
      return;
    }

    const candidateUrl = info.linkUrl || info.pageUrl;
    if (!candidateUrl) {
      return;
    }

    await captureDownload({
      downloadUrl: candidateUrl,
      tabUrl: tab?.url || info.pageUrl || "",
      suggestedName: info.linkText || normalizeSuggestedName(candidateUrl),
      referer: info.pageUrl || tab?.url || "",
      headerSnapshot: {
        referer: info.pageUrl || tab?.url || "",
        source: "context-menu"
      }
    });
  })().catch(async (error) => {
    await saveLastResult({
      success: false,
      message: error instanceof Error ? error.message : "右键转发失败",
      downloadUrl: info.linkUrl || info.pageUrl || ""
    });
  });
});

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message?.type !== "capture-download") {
    return false;
  }

  void handleCaptureMessage(message, sender)
    .then((result) => sendResponse(result))
    .catch(async (error) => {
      await saveLastResult({
        success: false,
        message: error instanceof Error ? error.message : "自动接管失败",
        downloadUrl: message?.downloadUrl || ""
      });
      sendResponse({
        ok: false,
        message: error instanceof Error ? error.message : "自动接管失败"
      });
    });
  return true;
});

chrome.runtime.onMessage.addListener((message, _sender, sendResponse) => {
  if (message?.type === "ping-local-service") {
    void pingLocalService()
      .then((data) => sendResponse({ ok: true, data }))
      .catch((error) =>
        sendResponse({
          ok: false,
          message: error instanceof Error ? error.message : "本地服务检测失败"
        })
      );
    return true;
  }

  if (message?.type === "get-config") {
    void getConfig().then((config) => sendResponse({ ok: true, data: config }));
    return true;
  }

  if (message?.type === "update-config") {
    void chrome.storage.local
      .set(message.payload || {})
      .then(async () => {
        const config = await getConfig();
        sendResponse({ ok: true, data: config });
      })
      .catch((error) =>
        sendResponse({
          ok: false,
          message: error instanceof Error ? error.message : "保存配置失败"
        })
      );
    return true;
  }

  if (message?.type === "get-last-result") {
    void chrome.storage.local.get(["lastCaptureResult"]).then((payload) =>
      sendResponse({
        ok: true,
        data: payload.lastCaptureResult || null
      })
    );
    return true;
  }

  return false;
});
