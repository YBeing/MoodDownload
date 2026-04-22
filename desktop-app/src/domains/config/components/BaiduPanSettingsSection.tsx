import type { BaiduPanPreflightResult, BaiduPanResolveResult } from "@/domains/provider/models/provider";

const BAIDU_AUTH_EXAMPLE = `{
  "cookie": "BDUSS=***; STOKEN=***",
  "sharePassword": "abcd"
}`;

const BAIDU_PROVIDER_CONTEXT_EXAMPLE = `{
  "mode": "share-link",
  "shareUrl": "https://pan.baidu.com/s/xxxx",
  "extractCode": "abcd"
}`;

interface BaiduPanSettingsSectionProps {
  authContext: string;
  checking: boolean;
  preflightResult: BaiduPanPreflightResult | null;
  providerContext: string;
  resolveResult: BaiduPanResolveResult | null;
  resolving: boolean;
  shareUrl: string;
  onChangeAuthContext: (value: string) => void;
  onChangeProviderContext: (value: string) => void;
  onChangeShareUrl: (value: string) => void;
  onPreflight: () => Promise<void>;
  onResolve: () => Promise<void>;
}

export function BaiduPanSettingsSection(props: BaiduPanSettingsSectionProps) {
  const {
    authContext,
    checking,
    preflightResult,
    providerContext,
    resolveResult,
    resolving,
    shareUrl,
    onChangeAuthContext,
    onChangeProviderContext,
    onChangeShareUrl,
    onPreflight,
    onResolve
  } = props;

  return (
    <section className="settings-card">
      <span>百度网盘配置</span>
      <strong>预研入口（待实现正式下载）</strong>

      <div className="inline-banner inline-banner--warning">
        <strong>当前为预研入口</strong>
        <span>本期只返回能力判断、风险标签和建议下一步，不承诺稳定下载闭环。</span>
      </div>

      <label className="settings-field">
        <span>分享链接</span>
        <input
          className="field"
          onChange={(event) => onChangeShareUrl(event.target.value)}
          placeholder="https://pan.baidu.com/s/..."
          value={shareUrl}
        />
      </label>

      <div className="settings-field">
        <span>鉴权上下文格式示例</span>
        <pre className="settings-example mono-text">{BAIDU_AUTH_EXAMPLE}</pre>
      </div>

      <label className="settings-field">
        <span>鉴权上下文</span>
        <textarea
          className="field"
          onChange={(event) => onChangeAuthContext(event.target.value)}
          placeholder={BAIDU_AUTH_EXAMPLE}
          rows={5}
          value={authContext}
        />
      </label>

      <div className="settings-actions">
        <button className="button" disabled={checking} onClick={() => void onPreflight()} type="button">
          {checking ? "检测中..." : "执行预研检测"}
        </button>
      </div>

      {preflightResult ? (
        <div className="settings-card">
          <div className="drawer-kv">
            <span>能力判断</span>
            <strong>{preflightResult.capability}</strong>
          </div>
          <div className="drawer-kv">
            <span>建议下一步</span>
            <strong>{preflightResult.suggestedNextStep}</strong>
          </div>
          <div className="settings-field">
            <span>风险标签</span>
            <div className="page-header-pills">
              {preflightResult.riskFlags.length > 0 ? (
                preflightResult.riskFlags.map((riskFlag) => (
                  <span key={riskFlag} className="header-pill">
                    {riskFlag}
                  </span>
                ))
              ) : (
                <span className="muted-text">暂无风险标签</span>
              )}
            </div>
          </div>
        </div>
      ) : null}

      <div className="settings-field">
        <span>Provider 上下文格式示例</span>
        <pre className="settings-example mono-text">{BAIDU_PROVIDER_CONTEXT_EXAMPLE}</pre>
      </div>

      <label className="settings-field">
        <span>Provider 上下文</span>
        <textarea
          className="field"
          onChange={(event) => onChangeProviderContext(event.target.value)}
          placeholder={BAIDU_PROVIDER_CONTEXT_EXAMPLE}
          rows={5}
          value={providerContext}
        />
      </label>

      <div className="settings-actions">
        <button className="button-ghost" disabled={resolving} onClick={() => void onResolve()} type="button">
          {resolving ? "解析中..." : "执行解析预演"}
        </button>
      </div>

      {resolveResult ? (
        <div className="settings-card">
          <div className="drawer-kv">
            <span>解析模式</span>
            <strong>{resolveResult.resolvedMode}</strong>
          </div>
          <div className="drawer-kv">
            <span>下一步动作</span>
            <strong>{resolveResult.nextStep}</strong>
          </div>
        </div>
      ) : null}
    </section>
  );
}
