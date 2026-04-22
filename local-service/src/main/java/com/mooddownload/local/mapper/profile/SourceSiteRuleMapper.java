package com.mooddownload.local.mapper.profile;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 站点规则 Mapper。
 */
public interface SourceSiteRuleMapper {

    String BASE_COLUMNS = "id, host_pattern, source_type, browser_code, profile_code, tracker_set_code, "
        + "require_header_snapshot, enabled, priority, created_at, updated_at";

    /**
     * 按主键查询站点规则。
     *
     * @param id 主键 ID
     * @return 持久化对象
     */
    @Select("SELECT " + BASE_COLUMNS + " FROM t_source_site_rule WHERE id = #{id}")
    SourceSiteRuleDO selectById(@Param("id") Long id);

    /**
     * 查询全部站点规则。
     *
     * @return 规则列表
     */
    @Select("SELECT " + BASE_COLUMNS + " FROM t_source_site_rule ORDER BY priority ASC, id ASC")
    List<SourceSiteRuleDO> selectAll();

    /**
     * 新增站点规则。
     *
     * @param sourceSiteRuleDO 持久化对象
     * @return 影响行数
     */
    @Insert("INSERT INTO t_source_site_rule (host_pattern, source_type, browser_code, profile_code, "
        + "tracker_set_code, require_header_snapshot, enabled, priority, created_at, updated_at) VALUES ("
        + "#{hostPattern}, #{sourceType}, #{browserCode}, #{profileCode}, #{trackerSetCode}, "
        + "#{requireHeaderSnapshot}, #{enabled}, #{priority}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SourceSiteRuleDO sourceSiteRuleDO);

    /**
     * 更新站点规则。
     *
     * @param sourceSiteRuleDO 持久化对象
     * @return 影响行数
     */
    @Update("UPDATE t_source_site_rule SET host_pattern = #{hostPattern}, source_type = #{sourceType}, "
        + "browser_code = #{browserCode}, profile_code = #{profileCode}, tracker_set_code = #{trackerSetCode}, "
        + "require_header_snapshot = #{requireHeaderSnapshot}, enabled = #{enabled}, priority = #{priority}, "
        + "updated_at = #{updatedAt} WHERE id = #{id}")
    int updateById(SourceSiteRuleDO sourceSiteRuleDO);
}
