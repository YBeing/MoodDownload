package com.mooddownload.local.mapper.provider;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 外部 Provider 会话 Mapper。
 */
public interface ExternalProviderSessionMapper {

    String BASE_COLUMNS = "id, provider_code, session_key, session_status, auth_context_json, risk_flags_json, "
        + "expires_at, created_at, updated_at";

    /**
     * 按会话键查询 Provider 会话。
     *
     * @param sessionKey 会话键
     * @return 持久化对象
     */
    @Select("SELECT " + BASE_COLUMNS + " FROM t_external_provider_session WHERE session_key = #{sessionKey}")
    ExternalProviderSessionDO selectBySessionKey(@Param("sessionKey") String sessionKey);

    /**
     * 按 Provider 查询最近会话。
     *
     * @param providerCode Provider 编码
     * @return 会话列表
     */
    @Select("SELECT " + BASE_COLUMNS + " FROM t_external_provider_session WHERE provider_code = #{providerCode} "
        + "ORDER BY updated_at DESC, id DESC")
    List<ExternalProviderSessionDO> selectByProviderCode(@Param("providerCode") String providerCode);

    /**
     * 新增 Provider 会话。
     *
     * @param sessionDO 持久化对象
     * @return 影响行数
     */
    @Insert("INSERT INTO t_external_provider_session (provider_code, session_key, session_status, "
        + "auth_context_json, risk_flags_json, expires_at, created_at, updated_at) VALUES (#{providerCode}, "
        + "#{sessionKey}, #{sessionStatus}, #{authContextJson}, #{riskFlagsJson}, #{expiresAt}, "
        + "#{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ExternalProviderSessionDO sessionDO);

    /**
     * 更新 Provider 会话。
     *
     * @param sessionDO 持久化对象
     * @return 影响行数
     */
    @Update("UPDATE t_external_provider_session SET session_status = #{sessionStatus}, "
        + "auth_context_json = #{authContextJson}, risk_flags_json = #{riskFlagsJson}, expires_at = #{expiresAt}, "
        + "updated_at = #{updatedAt} WHERE session_key = #{sessionKey}")
    int updateBySessionKey(ExternalProviderSessionDO sessionDO);
}
