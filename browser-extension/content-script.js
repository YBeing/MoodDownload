const AUTO_CAPTURE_URL_PATTERN = /(^magnet:\?)|(\.torrent(\?.*)?$)/i;
const HTTP_URL_PATTERN = /^https?:\/\//i;
const DOWNLOADABLE_FILE_PATTERN =
  /\.(zip|rar|7z|tar|gz|bz2|xz|iso|exe|msi|apk|dmg|pkg|deb|rpm|mp4|mkv|avi|mp3|flac|wav|pdf|epub|docx?|xlsx?|pptx?|csv)(\?.*)?$/i;
const DEFAULT_CONFIG = {
  autoCaptureMagnet: true,
  autoCaptureTorrent: true,
  autoCaptureDownloadButton: true
};

let currentConfig = { ...DEFAULT_CONFIG };

function findAnchorTarget(node) {
  if (!node) {
    return null;
  }
  if (node instanceof HTMLAnchorElement) {
    return node;
  }
  return node.closest ? node.closest("a[href]") : null;
}

function isMagnetLike(url) {
  return /^magnet:\?/i.test(url);
}

function isTorrentLike(url) {
  return /\.torrent(\?.*)?$/i.test(url);
}

function hasDownloadQueryHint(url) {
  try {
    const parsedUrl = new URL(url);
    const hintKeys = ["download", "dl", "attachment", "filename", "response-content-disposition"];
    return hintKeys.some((key) => parsedUrl.searchParams.has(key));
  } catch (_error) {
    return false;
  }
}

function isDownloadButtonLike(anchor, url) {
  if (!HTTP_URL_PATTERN.test(url)) {
    return false;
  }
  if (anchor.hasAttribute("download")) {
    return true;
  }
  if (DOWNLOADABLE_FILE_PATTERN.test(url)) {
    return true;
  }
  return hasDownloadQueryHint(url);
}

function shouldCapture(anchor, url) {
  if (!url) {
    return false;
  }
  if (AUTO_CAPTURE_URL_PATTERN.test(url)) {
    if (isMagnetLike(url)) {
      return Boolean(currentConfig.autoCaptureMagnet);
    }
    return Boolean(currentConfig.autoCaptureTorrent);
  }
  return Boolean(currentConfig.autoCaptureDownloadButton) && isDownloadButtonLike(anchor, url);
}

function fallbackToBrowser(anchor, href) {
  if (anchor.target === "_blank") {
    window.open(href, "_blank", "noopener,noreferrer");
    return;
  }
  window.location.assign(href);
}

function updateConfig(nextConfig) {
  currentConfig = {
    ...DEFAULT_CONFIG,
    ...nextConfig
  };
}

async function loadConfig() {
  const storedConfig = await chrome.storage.local.get(DEFAULT_CONFIG);
  updateConfig(storedConfig);
}

chrome.storage.onChanged.addListener((changes, areaName) => {
  if (areaName !== "local") {
    return;
  }
  const nextConfig = { ...currentConfig };
  Object.entries(changes).forEach(([key, change]) => {
    nextConfig[key] = change.newValue;
  });
  updateConfig(nextConfig);
});

void loadConfig();

document.addEventListener(
  "click",
  (event) => {
    if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
      return;
    }

    const anchor = findAnchorTarget(event.target);
    const href = anchor?.href || "";
    if (!anchor || !shouldCapture(anchor, href)) {
      return;
    }

    event.preventDefault();
    event.stopPropagation();

    chrome.runtime
      .sendMessage({
        type: "capture-download",
        downloadUrl: href,
        tabUrl: window.location.href,
        referer: window.location.href,
        suggestedName: anchor.download || anchor.textContent?.trim() || "",
        headerSnapshot: {
          referer: window.location.href,
          origin: window.location.origin,
          source: "content-script-left-click"
        }
      })
      .then((response) => {
        if (!response?.ok) {
          fallbackToBrowser(anchor, href);
        }
      })
      .catch(() => {
        fallbackToBrowser(anchor, href);
      });
  },
  true
);
