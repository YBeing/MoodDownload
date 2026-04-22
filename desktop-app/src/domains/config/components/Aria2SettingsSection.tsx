import type { EngineRuntimeSnapshot } from "@/domains/config/models/config";
import { getDefaultAria2Command } from "@/domains/config/utils/aria2Command";

interface Aria2SettingsSectionProps {
  aria2Command: string;
  runtimeSnapshot: EngineRuntimeSnapshot | null;
  saving: boolean;
  onChangeCommand: (value: string) => void;
  onSubmit: () => Promise<void>;
}

export function Aria2SettingsSection(props: Aria2SettingsSectionProps) {
  const { aria2Command, runtimeSnapshot, saving, onChangeCommand, onSubmit } = props;
  const defaultAria2Command = getDefaultAria2Command();

  if (!runtimeSnapshot) {
    return (
      <section className="settings-card">
        <span>aria2 配置</span>
        <strong>启动命令</strong>
        <div className="empty-state">正在读取 aria2 配置...</div>
      </section>
    );
  }

  return (
    <section className="settings-card">
      <span>aria2 配置</span>
      <strong>启动命令</strong>
      <span className="muted-text">保存后会询问是否立即重启 aria2 引擎；如果不重启，本次只保存配置。</span>

      <label className="settings-field">
        <span>aria2 启动命令</span>
        <textarea
          className="field mono-text"
          onChange={(event) => onChangeCommand(event.target.value)}
          placeholder={defaultAria2Command}
          rows={12}
          value={aria2Command}
        />
      </label>

      <div className="settings-actions">
        <button className="button" disabled={saving} onClick={() => void onSubmit()} type="button">
          {saving ? "保存中..." : "保存配置"}
        </button>
      </div>
    </section>
  );
}
