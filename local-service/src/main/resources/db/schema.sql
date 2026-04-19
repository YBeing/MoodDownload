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
