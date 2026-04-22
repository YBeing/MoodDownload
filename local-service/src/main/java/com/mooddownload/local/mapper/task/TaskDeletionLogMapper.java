package com.mooddownload.local.mapper.task;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 任务删除审计 Mapper。
 */
public interface TaskDeletionLogMapper {

    String BASE_COLUMNS = "id, task_id, delete_mode, output_removed, artifact_removed, recycle_bin_used, "
        + "result_status, operator_source, created_at";

    /**
     * 新增任务删除审计记录。
     *
     * @param taskDeletionLogDO 持久化对象
     * @return 影响行数
     */
    @Insert("INSERT INTO t_task_deletion_log (task_id, delete_mode, output_removed, artifact_removed, "
        + "recycle_bin_used, result_status, operator_source, created_at) VALUES (#{taskId}, #{deleteMode}, "
        + "#{outputRemoved}, #{artifactRemoved}, #{recycleBinUsed}, #{resultStatus}, #{operatorSource}, "
        + "#{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TaskDeletionLogDO taskDeletionLogDO);

    /**
     * 按任务查询删除审计记录。
     *
     * @param taskId 任务 ID
     * @return 审计列表
     */
    @Select("SELECT " + BASE_COLUMNS + " FROM t_task_deletion_log WHERE task_id = #{taskId} ORDER BY id DESC")
    List<TaskDeletionLogDO> selectByTaskId(@Param("taskId") Long taskId);
}
