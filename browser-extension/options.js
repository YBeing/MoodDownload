const FIELD_IDS = {
  serviceUrl: "service-url",
  localToken: "local-token",
  clientType: "client-type",
  autoCaptureMagnet: "auto-capture-magnet",
  autoCaptureTorrent: "auto-capture-torrent",
  autoCaptureDownloadButton: "auto-capture-download-button",
  captureViaContextMenu: "capture-via-context-menu"
};

async function sendMessage(message) {
  return chrome.runtime.sendMessage(message);
}

function readField(id) {
  return document.getElementById(id);
}

async function loadConfig() {
  const response = await sendMessage({ type: "get-config" });
  if (!response?.ok) {
    throw new Error(response?.message || "读取配置失败");
  }

  readField(FIELD_IDS.serviceUrl).value = response.data.serviceUrl || "";
  readField(FIELD_IDS.localToken).value = response.data.localToken || "";
  readField(FIELD_IDS.clientType).value = response.data.clientType || "";
  readField(FIELD_IDS.autoCaptureMagnet).checked = Boolean(response.data.autoCaptureMagnet);
  readField(FIELD_IDS.autoCaptureTorrent).checked = Boolean(response.data.autoCaptureTorrent);
  readField(FIELD_IDS.autoCaptureDownloadButton).checked = Boolean(response.data.autoCaptureDownloadButton);
  readField(FIELD_IDS.captureViaContextMenu).checked = Boolean(response.data.captureViaContextMenu);
}

async function saveConfig() {
  const payload = {
    serviceUrl: readField(FIELD_IDS.serviceUrl).value.trim(),
    localToken: readField(FIELD_IDS.localToken).value.trim(),
    clientType: readField(FIELD_IDS.clientType).value.trim(),
    autoCaptureMagnet: readField(FIELD_IDS.autoCaptureMagnet).checked,
    autoCaptureTorrent: readField(FIELD_IDS.autoCaptureTorrent).checked,
    autoCaptureDownloadButton: readField(FIELD_IDS.autoCaptureDownloadButton).checked,
    captureViaContextMenu: readField(FIELD_IDS.captureViaContextMenu).checked
  };

  const resultNode = document.getElementById("save-result");
  try {
    const response = await sendMessage({
      type: "update-config",
      payload
    });
    resultNode.textContent = response?.ok ? "设置已保存。" : response?.message || "保存失败";
  } catch (error) {
    resultNode.textContent = error instanceof Error ? error.message : "保存失败";
  }
}

document.getElementById("save-options")?.addEventListener("click", () => {
  void saveConfig();
});

void loadConfig().catch((error) => {
  const resultNode = document.getElementById("save-result");
  resultNode.textContent = error instanceof Error ? error.message : "读取配置失败";
});
