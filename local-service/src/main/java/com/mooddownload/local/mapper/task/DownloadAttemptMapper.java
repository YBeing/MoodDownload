package com.mooddownload.local.mapper.task;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectKey;

/**
 * 下载尝试 Mapper。
 */
public interface DownloadAttemptMapper {

    String BASE_COLUMNS = "id, task_id, attempt_no, trigger_reason, result_status, engine_gid, fail_phase, "
        + "fail_message, started_at, finished_at, created_at, updated_at";

    /**
     * 新增下载尝试记录。
     *
     * @param downloadAttemptDO 下载尝试持久化对象
     * @return 影响行数
     */
    @Insert("INSERT INTO t_download_attempt ("
        + "task_id, attempt_no, trigger_reason, result_status, engine_gid, fail_phase, fail_message, "
        + "started_at, finished_at, created_at, updated_at"
        + ") VALUES ("
        + "#{taskId}, #{attemptNo}, #{triggerReason}, #{resultStatus}, #{engineGid}, #{failPhase}, "
        + "#{failMessage}, #{startedAt}, #{finishedAt}, #{createdAt}, #{updatedAt})")
    @SelectKey(statement = "SELECT last_insert_rowid()", keyProperty = "id", before = false, resultType = Long.class)
    int insert(DownloadAttemptDO downloadAttemptDO);

    /**
     * 按任务 ID 查询尝试记录。
     *
     * @param taskId 任务 ID
     * @return 尝试记录列表
     */
    @Select("SELECT " + BASE_COLUMNS + " FROM t_download_attempt WHERE task_id = #{taskId} ORDER BY attempt_no ASC")
    List<DownloadAttemptDO> selectByTaskId(@Param("taskId") Long taskId);
}
