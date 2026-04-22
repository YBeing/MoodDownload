package com.mooddownload.local.mapper.config;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 下载配置 Mapper。
 */
public interface DownloadConfigMapper {

    String BASE_COLUMNS = "id, default_save_dir, max_concurrent_downloads, max_global_download_speed, "
        + "max_global_upload_speed, browser_capture_enabled, clipboard_monitor_enabled, auto_start_enabled, "
        + "active_engine_profile_code, delete_to_recycle_bin_enabled, local_api_token, created_at, updated_at";

    /**
     * 按主键查询下载配置。
     *
     * @param id 配置主键
     * @return 下载配置持久化对象
     */
    @Select("SELECT " + BASE_COLUMNS + " FROM t_download_config WHERE id = #{id}")
    DownloadConfigDO selectById(@Param("id") Integer id);

    /**
     * 新增下载配置。
     *
     * @param downloadConfigDO 下载配置持久化对象
     * @return 影响行数
     */
    @Insert("INSERT INTO t_download_config ("
        + "id, default_save_dir, max_concurrent_downloads, max_global_download_speed, max_global_upload_speed, "
        + "browser_capture_enabled, clipboard_monitor_enabled, auto_start_enabled, active_engine_profile_code, "
        + "delete_to_recycle_bin_enabled, local_api_token, created_at, updated_at"
        + ") VALUES ("
        + "#{id}, #{defaultSaveDir}, #{maxConcurrentDownloads}, #{maxGlobalDownloadSpeed}, "
        + "#{maxGlobalUploadSpeed}, #{browserCaptureEnabled}, #{clipboardMonitorEnabled}, #{autoStartEnabled}, "
        + "#{activeEngineProfileCode}, #{deleteToRecycleBinEnabled}, #{localApiToken}, #{createdAt}, #{updatedAt})")
    int insert(DownloadConfigDO downloadConfigDO);

    /**
     * 更新单例下载配置。
     *
     * @param downloadConfigDO 下载配置持久化对象
     * @return 影响行数
     */
    @Update("UPDATE t_download_config SET "
        + "default_save_dir = #{defaultSaveDir}, "
        + "max_concurrent_downloads = #{maxConcurrentDownloads}, "
        + "max_global_download_speed = #{maxGlobalDownloadSpeed}, "
        + "max_global_upload_speed = #{maxGlobalUploadSpeed}, "
        + "browser_capture_enabled = #{browserCaptureEnabled}, "
        + "clipboard_monitor_enabled = #{clipboardMonitorEnabled}, "
        + "auto_start_enabled = #{autoStartEnabled}, "
        + "active_engine_profile_code = #{activeEngineProfileCode}, "
        + "delete_to_recycle_bin_enabled = #{deleteToRecycleBinEnabled}, "
        + "local_api_token = #{localApiToken}, "
        + "updated_at = #{updatedAt} "
        + "WHERE id = #{id}")
    int updateById(DownloadConfigDO downloadConfigDO);
}
