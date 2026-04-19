INSERT OR IGNORE INTO t_download_config (
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
