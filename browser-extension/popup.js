async function sendMessage(message) {
  return chrome.runtime.sendMessage(message);
}

function setText(id, value) {
  const target = document.getElementById(id);
  if (target) {
    target.textContent = value;
  }
}

async function refreshStatus() {
  try {
    const response = await sendMessage({ type: "ping-local-service" });
    if (!response) {
      setText("service-status", "插件后台未返回检测结果");
      return;
    }
    setText("service-status", response.ok ? "本地服务可连接" : response.message || "本地服务不可连接");
  } catch (error) {
    setText("service-status", error instanceof Error ? error.message : "本地服务检测失败");
  }
}

async function loadLastResult() {
  try {
    const response = await sendMessage({ type: "get-last-result" });
    const result = response?.data;
    if (!result) {
      setText("last-result-status", "暂无");
      setText("last-result-type", "-");
      setText("last-result-rule", "-");
      setText("last-result-message", "还没有接管记录。");
      return;
    }
    setText("last-result-status", result.success ? "成功" : "失败");
    setText("last-result-type", result.resolvedSourceType || "-");
    setText("last-result-rule", result.siteRuleMatched ? "已命中" : "未命中");
    setText("last-result-message", result.message || result.downloadUrl || "最近一次接管已完成。");
  } catch (error) {
    setText("last-result-status", "失败");
    setText("last-result-type", "-");
    setText("last-result-rule", "-");
    setText("last-result-message", error instanceof Error ? error.message : "读取最近结果失败");
  }
}

document.getElementById("refresh-status")?.addEventListener("click", () => {
  void refreshStatus();
  void loadLastResult();
});

document.getElementById("open-options")?.addEventListener("click", () => {
  chrome.runtime.openOptionsPage();
});

void refreshStatus();
void loadLastResult();
