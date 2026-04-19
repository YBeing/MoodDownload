package com.mooddownload.local.mapper.task;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectKey;

/**
 * 下载子任务 Mapper。
 */
public interface DownloadEngineTaskMapper {

    String BASE_COLUMNS = "id, task_id, engine_gid, parent_engine_gid, engine_status, "
        + "torrent_file_list_json, metadata_only, total_size_bytes, completed_size_bytes, "
        + "download_speed_bps, upload_speed_bps, error_code, error_message, created_at, updated_at";

    /**
     * 新增下载子任务记录。
     *
     * @param downloadEngineTaskDO 子任务持久化对象
     * @return 影响行数
     */
    @Insert("INSERT INTO t_download_engine_task ("
        + "task_id, engine_gid, parent_engine_gid, engine_status, torrent_file_list_json, metadata_only, "
        + "total_size_bytes, completed_size_bytes, download_speed_bps, upload_speed_bps, error_code, error_message, "
        + "created_at, updated_at"
        + ") VALUES ("
        + "#{taskId}, #{engineGid}, #{parentEngineGid}, #{engineStatus}, #{torrentFileListJson}, #{metadataOnly}, "
        + "#{totalSizeBytes}, #{completedSizeBytes}, #{downloadSpeedBps}, #{uploadSpeedBps}, "
        + "#{errorCode}, #{errorMessage}, #{createdAt}, #{updatedAt})")
    @SelectKey(statement = "SELECT last_insert_rowid()", keyProperty = "id", before = false, resultType = Long.class)
    int insert(DownloadEngineTaskDO downloadEngineTaskDO);

    /**
     * 按任务 ID 查询下载子任务列表。
     *
     * @param taskId 业务任务 ID
     * @return 子任务列表
     */
    @Select("SELECT " + BASE_COLUMNS + " FROM t_download_engine_task WHERE task_id = #{taskId} "
        + "ORDER BY metadata_only ASC, total_size_bytes DESC, id ASC")
    List<DownloadEngineTaskDO> selectByTaskId(@Param("taskId") Long taskId);

    /**
     * 按引擎 gid 查询下载子任务。
     *
     * @param engineGid aria2 gid
     * @return 子任务持久化对象
     */
    @Select("SELECT " + BASE_COLUMNS + " FROM t_download_engine_task WHERE engine_gid = #{engineGid}")
    DownloadEngineTaskDO selectByEngineGid(@Param("engineGid") String engineGid);

    /**
     * 按任务 ID 删除既有子任务快照。
     *
     * @param taskId 业务任务 ID
     * @return 影响行数
     */
    @Delete("DELETE FROM t_download_engine_task WHERE task_id = #{taskId}")
    int deleteByTaskId(@Param("taskId") Long taskId);
}
