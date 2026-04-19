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
        ensureDownloadEngineTaskTable();
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
