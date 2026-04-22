import { DEFAULT_ARIA2_PROFILE_JSON, DEFAULT_ARIA2_TRACKER_LIST_TEXT } from "@/domains/config/constants/aria2Defaults";

const DEFAULT_ARIA2_COMMAND = `aria2c \\
  --enable-rpc \\
  --rpc-listen-all=false \\
  --rpc-listen-port=6800 \\
  --enable-dht=true \\
  --enable-dht6=true \\
  --bt-enable-lpd=true \\
  --enable-peer-exchange=true \\
  --listen-port=51413 \\
  --dht-listen-port=51413 \\
  --seed-time=0 \\
  --follow-torrent=true \\
  --follow-metalink=true \\
  --allow-overwrite=true \\
  --bt-tracker="udp://tracker.opentrackr.org:1337/announce,udp://tracker.torrent.eu.org:451/announce,udp://open.stealth.si:80/announce,udp://tracker.tryhackx.org:6969/announce,udp://tracker.qu.ax:6969/announce,https://tracker.opentrackr.org:443/announce"`;

interface ParsedAria2Command {
  profileJson: string;
  trackerListText: string;
}

function stringifyProfile(profile: Record<string, string>) {
  return JSON.stringify(profile, null, 2);
}

function tokenizeCommand(commandText: string) {
  const normalizedCommand = commandText.replace(/\\\r?\n/g, " ");
  const tokens: string[] = [];
  let current = "";
  let quote: '"' | "'" | null = null;

  for (let index = 0; index < normalizedCommand.length; index += 1) {
    const character = normalizedCommand[index];
    if (quote) {
      if (character === quote) {
        quote = null;
      } else {
        current += character;
      }
      continue;
    }

    if (character === '"' || character === "'") {
      quote = character;
      continue;
    }

    if (/\s/.test(character)) {
      if (current) {
        tokens.push(current);
        current = "";
      }
      continue;
    }

    current += character;
  }

  if (quote) {
    throw new Error("aria2 启动命令中的引号没有正确闭合");
  }

  if (current) {
    tokens.push(current);
  }

  return tokens.filter((token) => token && token !== "\\");
}

/**
 * 解析用户输入的 aria2 启动命令，拆分为启动参数 JSON 与 Tracker 列表。
 *
 * @param commandText aria2 启动命令文本
 * @returns 解析后的 profileJson 与 trackerListText
 * @throws Error 当命令为空、参数格式错误或 JSON 无法构造时抛出
 */
export function parseAria2Command(commandText: string): ParsedAria2Command {
  const trimmedCommand = commandText.trim();
  if (!trimmedCommand) {
    throw new Error("aria2 启动命令不能为空");
  }

  const tokens = tokenizeCommand(trimmedCommand);
  const profile: Record<string, string> = {};
  let trackerListText = DEFAULT_ARIA2_TRACKER_LIST_TEXT;
  let tokenIndex = 0;

  if (tokens[0] === "aria2c") {
    tokenIndex = 1;
  }

  while (tokenIndex < tokens.length) {
    const token = tokens[tokenIndex];
    tokenIndex += 1;
    if (!token.startsWith("--")) {
      continue;
    }

    const normalizedToken = token.slice(2);
    const separatorIndex = normalizedToken.indexOf("=");
    let optionKey = normalizedToken;
    let optionValue = "true";

    if (separatorIndex >= 0) {
      optionKey = normalizedToken.slice(0, separatorIndex);
      optionValue = normalizedToken.slice(separatorIndex + 1);
    } else if (tokenIndex < tokens.length && !tokens[tokenIndex].startsWith("--")) {
      optionValue = tokens[tokenIndex];
      tokenIndex += 1;
    }

    if (optionKey === "bt-tracker") {
      trackerListText = optionValue
        .split(",")
        .map((item) => item.trim())
        .filter(Boolean)
        .join("\n");
      continue;
    }

    profile[optionKey] = optionValue;
  }

  return {
    profileJson: stringifyProfile(profile),
    trackerListText
  };
}

/**
 * 将持久化的 profileJson 与 Tracker 列表重新组装为便于编辑的 aria2 启动命令。
 *
 * @param profileJson 持久化的启动参数 JSON
 * @param trackerListText 持久化的 Tracker 列表
 * @returns 适合在页面中编辑的 aria2 启动命令文本
 */
export function buildAria2Command(profileJson?: string | null, trackerListText?: string | null) {
  let profile: Record<string, string> = {};

  try {
    profile = JSON.parse(profileJson || DEFAULT_ARIA2_PROFILE_JSON) as Record<string, string>;
  } catch (_error) {
    profile = JSON.parse(DEFAULT_ARIA2_PROFILE_JSON) as Record<string, string>;
  }

  const commandLines = ["aria2c \\"];
  Object.entries(profile).forEach(([key, value]) => {
    if (key === "enable-rpc" && value === "true") {
      commandLines.push(`  --${key} \\`);
      return;
    }
    commandLines.push(`  --${key}=${value} \\`);
  });

  const mergedTrackers = (trackerListText || DEFAULT_ARIA2_TRACKER_LIST_TEXT)
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean)
    .join(",");
  commandLines.push(`  --bt-tracker="${mergedTrackers}"`);
  return commandLines.join("\n");
}

export function getDefaultAria2Command() {
  return DEFAULT_ARIA2_COMMAND;
}
