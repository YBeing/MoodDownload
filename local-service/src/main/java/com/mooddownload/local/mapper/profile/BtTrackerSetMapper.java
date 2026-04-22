package com.mooddownload.local.mapper.profile;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Tracker 配置集 Mapper。
 */
public interface BtTrackerSetMapper {

    String BASE_COLUMNS = "tracker_set_code, tracker_set_name, source_type, tracker_list_text, tracker_source_url, "
        + "is_builtin, created_at, updated_at";

    /**
     * 按编码查询 Tracker 集。
     *
     * @param trackerSetCode 编码
     * @return 持久化对象
     */
    @Select("SELECT " + BASE_COLUMNS + " FROM t_bt_tracker_set WHERE tracker_set_code = #{trackerSetCode}")
    BtTrackerSetDO selectByCode(@Param("trackerSetCode") String trackerSetCode);

    /**
     * 查询全部 Tracker 集。
     *
     * @return 列表
     */
    @Select("SELECT " + BASE_COLUMNS + " FROM t_bt_tracker_set ORDER BY is_builtin DESC, tracker_set_code ASC")
    List<BtTrackerSetDO> selectAll();

    /**
     * 新增 Tracker 集。
     *
     * @param trackerSetDO 持久化对象
     * @return 影响行数
     */
    @Insert("INSERT INTO t_bt_tracker_set (tracker_set_code, tracker_set_name, source_type, tracker_list_text, "
        + "tracker_source_url, is_builtin, created_at, updated_at) VALUES (#{trackerSetCode}, #{trackerSetName}, "
        + "#{sourceType}, #{trackerListText}, #{trackerSourceUrl}, #{isBuiltin}, #{createdAt}, #{updatedAt})")
    int insert(BtTrackerSetDO trackerSetDO);

    /**
     * 更新 Tracker 集。
     *
     * @param trackerSetDO 持久化对象
     * @return 影响行数
     */
    @Update("UPDATE t_bt_tracker_set SET tracker_set_name = #{trackerSetName}, source_type = #{sourceType}, "
        + "tracker_list_text = #{trackerListText}, tracker_source_url = #{trackerSourceUrl}, "
        + "is_builtin = #{isBuiltin}, updated_at = #{updatedAt} WHERE tracker_set_code = #{trackerSetCode}")
    int updateByCode(BtTrackerSetDO trackerSetDO);
}
