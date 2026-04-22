package com.mooddownload.local.common.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * SQLite 启动补列迁移，兼容本地已有数据库文件。
 */
@Component
public class SqliteSchemaMigrationRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqliteSchemaMigrationRunner.class);

    private final DataSource dataSource;

    public SqliteSchemaMigrationRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ensureTorrentFileListColumn();
        ensureDownloadTaskColumns();
        ensureDownloadConfigColumns();
        ensureDownloadEngineTaskTable();
        ensureEngineRuntimeProfileTable();
        ensureBtTrackerSetTable();
        ensureSourceSiteRuleTable();
        ensureExternalEntryLogTable();
        ensureTaskDeletionLogTable();
        ensureExternalProviderSessionTable();
    }

    /**
     * 为旧版下载任务表补充 BT 文件列表字段。
     *
     * @throws SQLException 数据库访问异常
     */
    private void ensureTorrentFileListColumn() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if (hasColumn(connection, "t_download_task", "torrent_file_list_json")) {
                return;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE t_download_task ADD COLUMN torrent_file_list_json TEXT");
                LOGGER.info("SQLite 表结构补列成功: table=t_download_task, column=torrent_file_list_json");
            }
        }
    }

    /**
     * 为旧版本地数据库补齐子任务快照表，兼容 BT 多子任务聚合查询。
     *
     * @throws SQLException 数据库访问异常
     */
    private void ensureDownloadEngineTaskTable() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS t_download_engine_task ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "task_id INTEGER NOT NULL,"
                + "engine_gid TEXT NOT NULL UNIQUE,"
                + "parent_engine_gid TEXT,"
                + "engine_status TEXT NOT NULL DEFAULT 'UNKNOWN',"
                + "torrent_file_list_json TEXT,"
                + "metadata_only INTEGER NOT NULL DEFAULT 0,"
                + "total_size_bytes INTEGER NOT NULL DEFAULT 0,"
                + "completed_size_bytes INTEGER NOT NULL DEFAULT 0,"
                + "download_speed_bps INTEGER NOT NULL DEFAULT 0,"
                + "upload_speed_bps INTEGER NOT NULL DEFAULT 0,"
                + "error_code TEXT,"
                + "error_message TEXT,"
                + "created_at INTEGER NOT NULL,"
                + "updated_at INTEGER NOT NULL,"
                + "CONSTRAINT fk_engine_task_task FOREIGN KEY (task_id) REFERENCES t_download_task(id)"
                + ")");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_engine_task_task "
                + "ON t_download_engine_task(task_id, metadata_only, total_size_bytes DESC, id ASC)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_engine_task_parent "
                + "ON t_download_engine_task(parent_engine_gid)");
            LOGGER.info("SQLite 子任务快照表检查完成: table=t_download_engine_task");
        }
    }

    /**
     * 为下载任务表补齐迭代版字段。
     *
     * @throws SQLException 数据库访问异常
     */
    private void ensureDownloadTaskColumns() throws SQLException {
        ensureColumn("t_download_task", "entry_type", "ALTER TABLE t_download_task "
            + "ADD COLUMN entry_type TEXT NOT NULL DEFAULT 'MANUAL'");
        ensureColumn("t_download_task", "source_provider", "ALTER TABLE t_download_task "
            + "ADD COLUMN source_provider TEXT NOT NULL DEFAULT 'GENERIC'");
        ensureColumn("t_download_task", "source_site_host", "ALTER TABLE t_download_task "
            + "ADD COLUMN source_site_host TEXT");
        ensureColumn("t_download_task", "entry_context_json", "ALTER TABLE t_download_task "
            + "ADD COLUMN entry_context_json TEXT");
        ensureColumn("t_download_task", "engine_profile_code", "ALTER TABLE t_download_task "
            + "ADD COLUMN engine_profile_code TEXT NOT NULL DEFAULT 'default'");
        ensureColumn("t_download_task", "open_folder_path", "ALTER TABLE t_download_task "
            + "ADD COLUMN open_folder_path TEXT");
        ensureColumn("t_download_task", "primary_file_path", "ALTER TABLE t_download_task "
            + "ADD COLUMN primary_file_path TEXT");
        ensureColumn("t_download_task", "completed_at", "ALTER TABLE t_download_task "
            + "ADD COLUMN completed_at INTEGER");
    }

    /**
     * 为下载配置表补齐迭代版字段。
     *
     * @throws SQLException 数据库访问异常
     */
    private void ensureDownloadConfigColumns() throws SQLException {
        ensureColumn("t_download_config", "active_engine_profile_code", "ALTER TABLE t_download_config "
            + "ADD COLUMN active_engine_profile_code TEXT NOT NULL DEFAULT 'default'");
        ensureColumn("t_download_config", "delete_to_recycle_bin_enabled", "ALTER TABLE t_download_config "
            + "ADD COLUMN delete_to_recycle_bin_enabled INTEGER NOT NULL DEFAULT 0");
    }

    /**
     * 确保引擎配置模板表存在。
     *
     * @throws SQLException 数据库访问异常
     */
    private void ensureEngineRuntimeProfileTable() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS t_engine_runtime_profile ("
                + "profile_code TEXT PRIMARY KEY,"
                + "profile_name TEXT NOT NULL,"
                + "tracker_set_code TEXT,"
                + "apply_scope TEXT NOT NULL,"
                + "profile_json TEXT NOT NULL,"
                + "enabled INTEGER NOT NULL DEFAULT 1,"
                + "is_default INTEGER NOT NULL DEFAULT 0,"
                + "version INTEGER NOT NULL DEFAULT 0,"
                + "created_at INTEGER NOT NULL,"
                + "updated_at INTEGER NOT NULL"
                + ")");
        }
    }

    /**
     * 确保 Tracker 配置表存在。
     *
     * @throws SQLException 数据库访问异常
     */
    private void ensureBtTrackerSetTable() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS t_bt_tracker_set ("
                + "tracker_set_code TEXT PRIMARY KEY,"
                + "tracker_set_name TEXT NOT NULL,"
                + "source_type TEXT NOT NULL DEFAULT 'BT',"
                + "tracker_list_text TEXT NOT NULL,"
                + "tracker_source_url TEXT,"
                + "is_builtin INTEGER NOT NULL DEFAULT 0,"
                + "created_at INTEGER NOT NULL,"
                + "updated_at INTEGER NOT NULL"
                + ")");
        }
    }

    /**
     * 确保站点规则表存在。
     *
     * @throws SQLException 数据库访问异常
     */
    private void ensureSourceSiteRuleTable() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS t_source_site_rule ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "host_pattern TEXT NOT NULL,"
                + "source_type TEXT,"
                + "browser_code TEXT,"
                + "profile_code TEXT NOT NULL,"
                + "tracker_set_code TEXT,"
                + "require_header_snapshot INTEGER NOT NULL DEFAULT 0,"
                + "enabled INTEGER NOT NULL DEFAULT 1,"
                + "priority INTEGER NOT NULL DEFAULT 100,"
                + "created_at INTEGER NOT NULL,"
                + "updated_at INTEGER NOT NULL"
                + ")");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_source_site_rule_host_priority "
                + "ON t_source_site_rule(host_pattern, priority, enabled)");
        }
    }

    /**
     * 确保外部入口审计表存在。
     *
     * @throws SQLException 数据库访问异常
     */
    private void ensureExternalEntryLogTable() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS t_external_entry_log ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "client_request_id TEXT NOT NULL,"
                + "entry_type TEXT NOT NULL,"
                + "browser_code TEXT,"
                + "source_type TEXT,"
                + "tab_url TEXT,"
                + "source_uri TEXT,"
                + "matched_rule_id INTEGER,"
                + "result_status TEXT NOT NULL,"
                + "remark TEXT,"
                + "created_at INTEGER NOT NULL"
                + ")");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_external_entry_log_request "
                + "ON t_external_entry_log(client_request_id, created_at)");
        }
    }

    /**
     * 确保任务删除审计表存在。
     *
     * @throws SQLException 数据库访问异常
     */
    private void ensureTaskDeletionLogTable() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS t_task_deletion_log ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "task_id INTEGER NOT NULL,"
                + "delete_mode TEXT NOT NULL,"
                + "output_removed INTEGER NOT NULL DEFAULT 0,"
                + "artifact_removed INTEGER NOT NULL DEFAULT 0,"
                + "recycle_bin_used INTEGER NOT NULL DEFAULT 0,"
                + "result_status TEXT NOT NULL,"
                + "operator_source TEXT NOT NULL,"
                + "created_at INTEGER NOT NULL,"
                + "CONSTRAINT fk_task_deletion_log_task FOREIGN KEY (task_id) REFERENCES t_download_task(id)"
                + ")");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_task_deletion_log_task_created "
                + "ON t_task_deletion_log(task_id, created_at)");
        }
    }

    /**
     * 确保外部 Provider 会话表存在。
     *
     * @throws SQLException 数据库访问异常
     */
    private void ensureExternalProviderSessionTable() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS t_external_provider_session ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "provider_code TEXT NOT NULL,"
                + "session_key TEXT NOT NULL UNIQUE,"
                + "session_status TEXT NOT NULL,"
                + "auth_context_json TEXT,"
                + "risk_flags_json TEXT,"
                + "expires_at INTEGER,"
                + "created_at INTEGER NOT NULL,"
                + "updated_at INTEGER NOT NULL"
                + ")");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_external_provider_session_provider_status "
                + "ON t_external_provider_session(provider_code, session_status, updated_at)");
        }
    }

    private void ensureColumn(String tableName, String columnName, String ddl) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if (hasColumn(connection, tableName, columnName)) {
                return;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute(ddl);
                LOGGER.info("SQLite 表结构补列成功: table={}, column={}", tableName, columnName);
            }
        }
    }

    private boolean hasColumn(Connection connection, String tableName, String columnName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (resultSet.next()) {
                if (columnName.equalsIgnoreCase(resultSet.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }
}
