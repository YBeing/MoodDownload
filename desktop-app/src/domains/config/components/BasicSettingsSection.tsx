import type { DownloadConfig } from "@/domains/config/models/config";
import { formatDateTime } from "@/shared/utils/formatters";

interface BasicSettingsSectionProps {
  config: DownloadConfig | null;
  errorMessage: string;
  loading: boolean;
  saving: boolean;
  successMessage: string;
  onSelectDefaultSaveDir: () => Promise<void>;
  onSubmit: () => Promise<void>;
  onUpdateField: <K extends keyof DownloadConfig>(key: K, value: DownloadConfig[K]) => void;
}

export function BasicSettingsSection(props: BasicSettingsSectionProps) {
  const { config, errorMessage, loading, saving, successMessage, onSelectDefaultSaveDir, onSubmit, onUpdateField } = props;

  return (
    <section className="settings-card">
      <span>基础设置</span>
      <strong>下载目录、并发与基础开关</strong>

      {loading ? <div className="empty-state">正在读取本地配置...</div> : null}

      {!loading && errorMessage ? (
        <div className="error-state">
          <div>
            <strong>配置加载失败</strong>
            <p>{errorMessage}</p>
          </div>
        </div>
      ) : null}

      {!loading && config ? (
        <>
          {successMessage ? (
            <div className="inline-banner inline-banner--success">
              <strong>保存成功</strong>
              <span>{successMessage}</span>
            </div>
          ) : null}

          <div className="settings-form">
            <label className="settings-field">
              <span>默认下载目录</span>
              <div className="folder-picker">
                <input className="field folder-picker__input" readOnly value={config.defaultSaveDir} />
                <button
                  className="button-ghost folder-picker__button"
                  disabled={saving}
                  onClick={() => void onSelectDefaultSaveDir()}
                  type="button"
                >
                  选择文件夹
                </button>
              </div>
            </label>

            <label className="settings-field">
              <span>最大并发下载数</span>
              <input
                className="field"
                min={1}
                onChange={(event) => onUpdateField("maxConcurrentDownloads", Number(event.target.value))}
                type="number"
                value={config.maxConcurrentDownloads}
              />
            </label>

            <label className="settings-field">
              <span>全局下载限速（B/s）</span>
              <input
                className="field"
                min={0}
                onChange={(event) => onUpdateField("maxGlobalDownloadSpeed", Number(event.target.value))}
                type="number"
                value={config.maxGlobalDownloadSpeed}
              />
            </label>

            <label className="settings-field">
              <span>全局上传限速（B/s）</span>
              <input
                className="field"
                min={0}
                onChange={(event) => onUpdateField("maxGlobalUploadSpeed", Number(event.target.value))}
                type="number"
                value={config.maxGlobalUploadSpeed}
              />
            </label>

            <div className="toggle-row">
              <div>
                <strong>浏览器接管</strong>
                <span>开启后会对新入队的外部 HTTP / HTTPS 任务给出浏览器接管结果提示。</span>
              </div>
              <button
                className={config.browserCaptureEnabled ? "toggle toggle--checked" : "toggle"}
                onClick={() => onUpdateField("browserCaptureEnabled", !config.browserCaptureEnabled)}
                type="button"
              />
            </div>

            <div className="toggle-row">
              <div>
                <strong>剪贴板监听</strong>
                <span>开启后会轮询系统剪贴板中的下载地址，并可直接带入新建任务弹窗确认。</span>
              </div>
              <button
                className={config.clipboardMonitorEnabled ? "toggle toggle--checked" : "toggle"}
                onClick={() => onUpdateField("clipboardMonitorEnabled", !config.clipboardMonitorEnabled)}
                type="button"
              />
            </div>
          </div>

          <div className="settings-actions">
            <span className="muted-text">最近更新时间：{formatDateTime(config.updatedAt)}</span>
            <button className="button" disabled={saving} onClick={() => void onSubmit()} type="button">
              {saving ? "保存中..." : "保存设置"}
            </button>
          </div>
        </>
      ) : null}
    </section>
  );
}
