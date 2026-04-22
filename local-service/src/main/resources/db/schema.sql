CREATE TABLE IF NOT EXISTS t_download_task (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_code TEXT NOT NULL UNIQUE,
    source_type TEXT NOT NULL,
    source_uri TEXT NOT NULL,
    source_hash TEXT,
    torrent_file_path TEXT,
    torrent_file_list_json TEXT,
    display_name TEXT,
    domain_status TEXT NOT NULL DEFAULT 'PENDING',
    engine_status TEXT NOT NULL DEFAULT 'UNKNOWN',
    engine_gid TEXT UNIQUE,
    queue_priority INTEGER NOT NULL DEFAULT 100,
    save_dir TEXT NOT NULL,
    total_size_bytes INTEGER NOT NULL DEFAULT 0,
    completed_size_bytes INTEGER NOT NULL DEFAULT 0,
    download_speed_bps INTEGER NOT NULL DEFAULT 0,
    upload_speed_bps INTEGER NOT NULL DEFAULT 0,
    error_code TEXT,
    error_message TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retry_count INTEGER NOT NULL DEFAULT 3,
    client_request_id TEXT UNIQUE,
    entry_type TEXT NOT NULL DEFAULT 'MANUAL',
    source_provider TEXT NOT NULL DEFAULT 'GENERIC',
    source_site_host TEXT,
    entry_context_json TEXT,
    engine_profile_code TEXT NOT NULL DEFAULT 'default',
    open_folder_path TEXT,
    primary_file_path TEXT,
    completed_at INTEGER,
    last_sync_at INTEGER,
    version INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_task_status_priority
    ON t_download_task(domain_status, queue_priority, created_at);
CREATE INDEX IF NOT EXISTS idx_task_source_hash
    ON t_download_task(source_hash);
CREATE INDEX IF NOT EXISTS idx_task_updated_at
    ON t_download_task(updated_at);

CREATE TABLE IF NOT EXISTS t_download_engine_task (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    engine_gid TEXT NOT NULL UNIQUE,
    parent_engine_gid TEXT,
    engine_status TEXT NOT NULL DEFAULT 'UNKNOWN',
    torrent_file_list_json TEXT,
    metadata_only INTEGER NOT NULL DEFAULT 0,
    total_size_bytes INTEGER NOT NULL DEFAULT 0,
    completed_size_bytes INTEGER NOT NULL DEFAULT 0,
    download_speed_bps INTEGER NOT NULL DEFAULT 0,
    upload_speed_bps INTEGER NOT NULL DEFAULT 0,
    error_code TEXT,
    error_message TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    CONSTRAINT fk_engine_task_task FOREIGN KEY (task_id) REFERENCES t_download_task(id)
);

CREATE INDEX IF NOT EXISTS idx_engine_task_task
    ON t_download_engine_task(task_id, metadata_only, total_size_bytes DESC, id ASC);
CREATE INDEX IF NOT EXISTS idx_engine_task_parent
    ON t_download_engine_task(parent_engine_gid);

CREATE TABLE IF NOT EXISTS t_download_attempt (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    attempt_no INTEGER NOT NULL,
    trigger_reason TEXT NOT NULL,
    result_status TEXT NOT NULL,
    engine_gid TEXT,
    fail_phase TEXT,
    fail_message TEXT,
    started_at INTEGER NOT NULL,
    finished_at INTEGER,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    CONSTRAINT fk_attempt_task FOREIGN KEY (task_id) REFERENCES t_download_task(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_attempt_task_no
    ON t_download_attempt(task_id, attempt_no);
CREATE INDEX IF NOT EXISTS idx_attempt_task_created
    ON t_download_attempt(task_id, created_at);

CREATE TABLE IF NOT EXISTS t_download_config (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    default_save_dir TEXT NOT NULL,
    max_concurrent_downloads INTEGER NOT NULL DEFAULT 3,
    max_global_download_speed INTEGER NOT NULL DEFAULT 0,
    max_global_upload_speed INTEGER NOT NULL DEFAULT 0,
    browser_capture_enabled INTEGER NOT NULL DEFAULT 1,
    clipboard_monitor_enabled INTEGER NOT NULL DEFAULT 1,
    auto_start_enabled INTEGER NOT NULL DEFAULT 0,
    active_engine_profile_code TEXT NOT NULL DEFAULT 'default',
    delete_to_recycle_bin_enabled INTEGER NOT NULL DEFAULT 0,
    local_api_token TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS t_task_state_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    from_status TEXT,
    to_status TEXT NOT NULL,
    trigger_source TEXT NOT NULL,
    trigger_type TEXT NOT NULL,
    remark TEXT,
    created_at INTEGER NOT NULL,
    CONSTRAINT fk_state_log_task FOREIGN KEY (task_id) REFERENCES t_download_task(id)
);

CREATE INDEX IF NOT EXISTS idx_state_log_task_created
    ON t_task_state_log(task_id, created_at);

CREATE TABLE IF NOT EXISTS t_engine_runtime_profile (
    profile_code TEXT PRIMARY KEY,
    profile_name TEXT NOT NULL,
    tracker_set_code TEXT,
    apply_scope TEXT NOT NULL,
    profile_json TEXT NOT NULL,
    enabled INTEGER NOT NULL DEFAULT 1,
    is_default INTEGER NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS t_bt_tracker_set (
    tracker_set_code TEXT PRIMARY KEY,
    tracker_set_name TEXT NOT NULL,
    source_type TEXT NOT NULL DEFAULT 'BT',
    tracker_list_text TEXT NOT NULL,
    tracker_source_url TEXT,
    is_builtin INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS t_source_site_rule (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    host_pattern TEXT NOT NULL,
    source_type TEXT,
    browser_code TEXT,
    profile_code TEXT NOT NULL,
    tracker_set_code TEXT,
    require_header_snapshot INTEGER NOT NULL DEFAULT 0,
    enabled INTEGER NOT NULL DEFAULT 1,
    priority INTEGER NOT NULL DEFAULT 100,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_source_site_rule_host_priority
    ON t_source_site_rule(host_pattern, priority, enabled);

CREATE TABLE IF NOT EXISTS t_external_entry_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    client_request_id TEXT NOT NULL,
    entry_type TEXT NOT NULL,
    browser_code TEXT,
    source_type TEXT,
    tab_url TEXT,
    source_uri TEXT,
    matched_rule_id INTEGER,
    result_status TEXT NOT NULL,
    remark TEXT,
    created_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_external_entry_log_request
    ON t_external_entry_log(client_request_id, created_at);

CREATE TABLE IF NOT EXISTS t_task_deletion_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    delete_mode TEXT NOT NULL,
    output_removed INTEGER NOT NULL DEFAULT 0,
    artifact_removed INTEGER NOT NULL DEFAULT 0,
    recycle_bin_used INTEGER NOT NULL DEFAULT 0,
    result_status TEXT NOT NULL,
    operator_source TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    CONSTRAINT fk_task_deletion_log_task FOREIGN KEY (task_id) REFERENCES t_download_task(id)
);

CREATE INDEX IF NOT EXISTS idx_task_deletion_log_task_created
    ON t_task_deletion_log(task_id, created_at);

CREATE TABLE IF NOT EXISTS t_external_provider_session (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    provider_code TEXT NOT NULL,
    session_key TEXT NOT NULL UNIQUE,
    session_status TEXT NOT NULL,
    auth_context_json TEXT,
    risk_flags_json TEXT,
    expires_at INTEGER,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_external_provider_session_provider_status
    ON t_external_provider_session(provider_code, session_status, updated_at);
