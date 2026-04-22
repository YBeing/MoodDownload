package com.mooddownload.local.mapper.task;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectKey;
import org.apache.ibatis.annotations.Update;

/**
 * 下载任务 Mapper。
 */
public interface DownloadTaskMapper {

    String BASE_COLUMNS = "id, task_code, source_type, source_uri, source_hash, torrent_file_path, "
        + "torrent_file_list_json, display_name, "
        + "domain_status, engine_status, engine_gid, queue_priority, save_dir, total_size_bytes, "
        + "completed_size_bytes, download_speed_bps, upload_speed_bps, error_code, error_message, retry_count, "
        + "max_retry_count, client_request_id, entry_type, source_provider, source_site_host, "
        + "entry_context_json, engine_profile_code, open_folder_path, primary_file_path, completed_at, "
        + "last_sync_at, version, created_at, updated_at";

    /**
     * 新增下载任务记录。
     *
     * @param downloadTaskDO 下载任务持久化对象
     * @return 影响行数
     */
    @Insert("INSERT INTO t_download_task ("
        + "task_code, source_type, source_uri, source_hash, torrent_file_path, torrent_file_list_json, "
        + "display_name, domain_status, "
        + "engine_status, engine_gid, queue_priority, save_dir, total_size_bytes, completed_size_bytes, "
        + "download_speed_bps, upload_speed_bps, error_code, error_message, retry_count, max_retry_count, "
        + "client_request_id, entry_type, source_provider, source_site_host, entry_context_json, "
        + "engine_profile_code, open_folder_path, primary_file_path, completed_at, last_sync_at, version, "
        + "created_at, updated_at"
        + ") VALUES ("
        + "#{taskCode}, #{sourceType}, #{sourceUri}, #{sourceHash}, #{torrentFilePath}, #{torrentFileListJson}, "
        + "#{displayName}, "
        + "#{domainStatus}, #{engineStatus}, #{engineGid}, #{queuePriority}, #{saveDir}, #{totalSizeBytes}, "
        + "#{completedSizeBytes}, #{downloadSpeedBps}, #{uploadSpeedBps}, #{errorCode}, #{errorMessage}, "
        + "#{retryCount}, #{maxRetryCount}, #{clientRequestId}, #{entryType}, #{sourceProvider}, "
        + "#{sourceSiteHost}, #{entryContextJson}, #{engineProfileCode}, #{openFolderPath}, "
        + "#{primaryFilePath}, #{completedAt}, #{lastSyncAt}, #{version}, #{createdAt}, #{updatedAt})")
    @SelectKey(statement = "SELECT last_insert_rowid()", keyProperty = "id", before = false, resultType = Long.class)
    int insert(DownloadTaskDO downloadTaskDO);

    /**
     * 按主键查询下载任务。
     *
     * @param id 主键 ID
     * @return 下载任务持久化对象
     */
    @Select("SELECT " + BASE_COLUMNS + " FROM t_download_task WHERE id = #{id}")
    DownloadTaskDO selectById(@Param("id") Long id);

    /**
     * 按幂等键查询下载任务。
     *
     * @param clientRequestId 幂等键
     * @return 下载任务持久化对象
     */
    @Select("SELECT " + BASE_COLUMNS + " FROM t_download_task WHERE client_request_id = #{clientRequestId}")
    DownloadTaskDO selectByClientRequestId(@Param("clientRequestId") String clientRequestId);

    /**
     * 按来源哈希查询最近的可复用任务。
     *
     * @param sourceHash 来源哈希
     * @return 下载任务持久化对象
     */
    @Select("SELECT " + BASE_COLUMNS + " FROM t_download_task "
        + "WHERE source_hash = #{sourceHash} AND domain_status != 'CANCELLED' "
        + "ORDER BY updated_at DESC, id DESC LIMIT 1")
    DownloadTaskDO selectReusableBySourceHash(@Param("sourceHash") String sourceHash);

    /**
     * 按引擎 gid 查询下载任务。
     *
     * @param engineGid aria2 gid
     * @return 下载任务持久化对象
     */
    @Select("SELECT " + BASE_COLUMNS + " FROM t_download_task WHERE engine_gid = #{engineGid}")
    DownloadTaskDO selectByEngineGid(@Param("engineGid") String engineGid);

    /**
     * 查询全部未删除任务。
     *
     * @return 任务列表
     */
    @Select("SELECT " + BASE_COLUMNS + " FROM t_download_task WHERE domain_status != 'CANCELLED' "
        + "ORDER BY updated_at DESC, id DESC")
    List<DownloadTaskDO> selectAllActiveTasks();

    /**
     * 按领域状态查询待处理任务。
     *
     * @param domainStatus 领域状态
     * @param limit 最大返回条数
     * @return 任务列表
     */
    @Select("SELECT " + BASE_COLUMNS + " FROM t_download_task "
        + "WHERE domain_status = #{domainStatus} "
        + "ORDER BY queue_priority ASC, created_at ASC LIMIT #{limit}")
    List<DownloadTaskDO> selectByDomainStatus(
        @Param("domainStatus") String domainStatus,
        @Param("limit") Integer limit
    );

    /**
     * 按条件分页查询任务。
     *
     * @param status 状态过滤
     * @param keyword 关键字
     * @param offset 偏移量
     * @param limit 最大条数
     * @return 任务列表
     */
    @Select({
        "<script>",
        "SELECT " + BASE_COLUMNS,
        "FROM t_download_task",
        "WHERE 1 = 1",
        "<if test='status == null or status == \"\"'>",
        "AND domain_status != 'CANCELLED'",
        "</if>",
        "<if test='status != null and status != \"\"'>",
        "AND domain_status = #{status}",
        "</if>",
        "<if test='keyword != null and keyword != \"\"'>",
        "AND (display_name LIKE '%' || #{keyword} || '%'",
        "OR task_code LIKE '%' || #{keyword} || '%'",
        "OR source_uri LIKE '%' || #{keyword} || '%')",
        "</if>",
        "ORDER BY updated_at DESC, id DESC",
        "LIMIT #{limit} OFFSET #{offset}",
        "</script>"
    })
    List<DownloadTaskDO> selectPageByCondition(
        @Param("status") String status,
        @Param("keyword") String keyword,
        @Param("offset") Integer offset,
        @Param("limit") Integer limit
    );

    /**
     * 按条件统计任务数量。
     *
     * @param status 状态过滤
     * @param keyword 关键字
     * @return 总数量
     */
    @Select({
        "<script>",
        "SELECT COUNT(1)",
        "FROM t_download_task",
        "WHERE 1 = 1",
        "<if test='status == null or status == \"\"'>",
        "AND domain_status != 'CANCELLED'",
        "</if>",
        "<if test='status != null and status != \"\"'>",
        "AND domain_status = #{status}",
        "</if>",
        "<if test='keyword != null and keyword != \"\"'>",
        "AND (display_name LIKE '%' || #{keyword} || '%'",
        "OR task_code LIKE '%' || #{keyword} || '%'",
        "OR source_uri LIKE '%' || #{keyword} || '%')",
        "</if>",
        "</script>"
    })
    long countByCondition(
        @Param("status") String status,
        @Param("keyword") String keyword
    );

    /**
     * 更新任务核心运行快照。
     *
     * @param downloadTaskDO 下载任务持久化对象
     * @return 影响行数
     */
    @Update("UPDATE t_download_task SET "
        + "display_name = #{displayName}, "
        + "torrent_file_list_json = #{torrentFileListJson}, "
        + "domain_status = #{domainStatus}, "
        + "engine_status = #{engineStatus}, "
        + "engine_gid = #{engineGid}, "
        + "queue_priority = #{queuePriority}, "
        + "save_dir = #{saveDir}, "
        + "total_size_bytes = #{totalSizeBytes}, "
        + "completed_size_bytes = #{completedSizeBytes}, "
        + "download_speed_bps = #{downloadSpeedBps}, "
        + "upload_speed_bps = #{uploadSpeedBps}, "
        + "error_code = #{errorCode}, "
        + "error_message = #{errorMessage}, "
        + "retry_count = #{retryCount}, "
        + "max_retry_count = #{maxRetryCount}, "
        + "entry_type = #{entryType}, "
        + "source_provider = #{sourceProvider}, "
        + "source_site_host = #{sourceSiteHost}, "
        + "entry_context_json = #{entryContextJson}, "
        + "engine_profile_code = #{engineProfileCode}, "
        + "open_folder_path = #{openFolderPath}, "
        + "primary_file_path = #{primaryFilePath}, "
        + "completed_at = #{completedAt}, "
        + "last_sync_at = #{lastSyncAt}, "
        + "version = #{version}, "
        + "updated_at = #{updatedAt} "
        + "WHERE id = #{id}")
    int updateCoreSnapshot(DownloadTaskDO downloadTaskDO);
}
