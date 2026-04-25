package com.mooddownload.local.mapper.task;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectKey;

/**
 * 任务状态日志 Mapper。
 */
public interface TaskStateLogMapper {

    String BASE_COLUMNS = "id, task_id, from_status, to_status, trigger_source, trigger_type, remark, created_at";

    /**
     * 新增任务状态日志。
     *
     * @param taskStateLogDO 状态日志持久化对象
     * @return 影响行数
     */
    @Insert("INSERT INTO t_task_state_log ("
        + "task_id, from_status, to_status, trigger_source, trigger_type, remark, created_at"
        + ") VALUES ("
        + "#{taskId}, #{fromStatus}, #{toStatus}, #{triggerSource}, #{triggerType}, #{remark}, #{createdAt})")
    @SelectKey(statement = "SELECT last_insert_rowid()", keyProperty = "id", before = false, resultType = Long.class)
    int insert(TaskStateLogDO taskStateLogDO);

    /**
     * 按任务 ID 查询状态日志。
     *
     * @param taskId 任务 ID
     * @return 状态日志列表
     */
    @Select("SELECT " + BASE_COLUMNS
        + " FROM t_task_state_log WHERE task_id = #{taskId} ORDER BY created_at ASC, id ASC")
    List<TaskStateLogDO> selectByTaskId(@Param("taskId") Long taskId);

    /**
     * 查询任务最近一条状态日志。
     *
     * @param taskId 任务 ID
     * @return 最近状态日志，不存在返回 null
     */
    @Select("SELECT " + BASE_COLUMNS
        + " FROM t_task_state_log WHERE task_id = #{taskId} ORDER BY created_at DESC, id DESC LIMIT 1")
    TaskStateLogDO selectLatestByTaskId(@Param("taskId") Long taskId);
}
