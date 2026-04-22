package com.mooddownload.local.mapper.profile;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 引擎运行配置模板 Mapper。
 */
public interface EngineRuntimeProfileMapper {

    String BASE_COLUMNS = "profile_code, profile_name, tracker_set_code, apply_scope, profile_json, enabled, "
        + "is_default, version, created_at, updated_at";

    /**
     * 按编码查询配置模板。
     *
     * @param profileCode 模板编码
     * @return 持久化对象
     */
    @Select("SELECT " + BASE_COLUMNS + " FROM t_engine_runtime_profile WHERE profile_code = #{profileCode}")
    EngineRuntimeProfileDO selectByCode(@Param("profileCode") String profileCode);

    /**
     * 查询全部配置模板。
     *
     * @return 配置模板列表
     */
    @Select("SELECT " + BASE_COLUMNS + " FROM t_engine_runtime_profile ORDER BY is_default DESC, profile_code ASC")
    List<EngineRuntimeProfileDO> selectAll();

    /**
     * 新增配置模板。
     *
     * @param profileDO 持久化对象
     * @return 影响行数
     */
    @Insert("INSERT INTO t_engine_runtime_profile (profile_code, profile_name, tracker_set_code, apply_scope, "
        + "profile_json, enabled, is_default, version, created_at, updated_at) VALUES (#{profileCode}, "
        + "#{profileName}, #{trackerSetCode}, #{applyScope}, #{profileJson}, #{enabled}, #{isDefault}, "
        + "#{version}, #{createdAt}, #{updatedAt})")
    int insert(EngineRuntimeProfileDO profileDO);

    /**
     * 更新配置模板。
     *
     * @param profileDO 持久化对象
     * @return 影响行数
     */
    @Update("UPDATE t_engine_runtime_profile SET profile_name = #{profileName}, tracker_set_code = #{trackerSetCode}, "
        + "apply_scope = #{applyScope}, profile_json = #{profileJson}, enabled = #{enabled}, "
        + "is_default = #{isDefault}, version = #{version}, updated_at = #{updatedAt} "
        + "WHERE profile_code = #{profileCode}")
    int updateByCode(EngineRuntimeProfileDO profileDO);
}
