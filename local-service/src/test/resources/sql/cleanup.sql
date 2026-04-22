DELETE FROM t_task_state_log;
DELETE FROM t_download_attempt;
DELETE FROM t_download_engine_task;
DELETE FROM t_task_deletion_log;
DELETE FROM t_external_entry_log;
DELETE FROM t_external_provider_session;
DELETE FROM t_source_site_rule;
DELETE FROM t_bt_tracker_set;
DELETE FROM t_engine_runtime_profile;
DELETE FROM t_download_task;
DELETE FROM t_download_config;

INSERT INTO t_download_config (
    id,
    default_save_dir,
    max_concurrent_downloads,
    max_global_download_speed,
    max_global_upload_speed,
    browser_capture_enabled,
    clipboard_monitor_enabled,
    auto_start_enabled,
    local_api_token,
    created_at,
    updated_at
) VALUES (
    1,
    './downloads',
    3,
    0,
    0,
    1,
    1,
    0,
    NULL,
    0,
    0
);
