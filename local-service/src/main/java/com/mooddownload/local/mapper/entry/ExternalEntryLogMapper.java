package com.mooddownload.local.mapper.entry;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 外部入口审计 Mapper。
 */
public interface ExternalEntryLogMapper {

    /**
     * 新增外部入口审计记录。
     *
     * @param externalEntryLogDO 持久化对象
     * @return 影响行数
     */
    @Insert("INSERT INTO t_external_entry_log (client_request_id, entry_type, browser_code, source_type, tab_url, "
        + "source_uri, matched_rule_id, result_status, remark, created_at) VALUES (#{clientRequestId}, "
        + "#{entryType}, #{browserCode}, #{sourceType}, #{tabUrl}, #{sourceUri}, #{matchedRuleId}, "
        + "#{resultStatus}, #{remark}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ExternalEntryLogDO externalEntryLogDO);

    /**
     * 按幂等键查询最近一条审计记录。
     *
     * @param clientRequestId 幂等键
     * @return 持久化对象
     */
    @Select("SELECT id, client_request_id, entry_type, browser_code, source_type, tab_url, source_uri, "
        + "matched_rule_id, result_status, remark, created_at FROM t_external_entry_log "
        + "WHERE client_request_id = #{clientRequestId} ORDER BY id DESC LIMIT 1")
    ExternalEntryLogDO selectLatestByClientRequestId(@Param("clientRequestId") String clientRequestId);
}
