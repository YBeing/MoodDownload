package com.mooddownload.local.dal.config;

import com.mooddownload.local.mapper.config.DownloadConfigDO;
import com.mooddownload.local.mapper.config.DownloadConfigMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 * 下载配置 Repository。
 */
@Repository
public class DownloadConfigRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadConfigRepository.class);

    private static final int SINGLETON_CONFIG_ID = 1;

    private final DownloadConfigMapper downloadConfigMapper;

    public DownloadConfigRepository(DownloadConfigMapper downloadConfigMapper) {
        this.downloadConfigMapper = downloadConfigMapper;
    }

    /**
     * 查询单例下载配置。
     *
     * @return 下载配置持久化对象
     */
    public Optional<DownloadConfigDO> findSingleton() {
        return Optional.ofNullable(downloadConfigMapper.selectById(SINGLETON_CONFIG_ID));
    }

    /**
     * 保存或更新单例下载配置。
     *
     * @param downloadConfigDO 下载配置持久化对象
     */
    public void saveOrUpdate(DownloadConfigDO downloadConfigDO) {
        if (downloadConfigDO.getId() == null) {
            downloadConfigDO.setId(SINGLETON_CONFIG_ID);
        }

        DownloadConfigDO existingConfig = downloadConfigMapper.selectById(downloadConfigDO.getId());
        int affectedRows = existingConfig == null
            ? downloadConfigMapper.insert(downloadConfigDO)
            : downloadConfigMapper.updateById(downloadConfigDO);
        if (affectedRows != 1) {
            LOGGER.error("保存下载配置失败: configId={}", downloadConfigDO.getId());
            throw new IllegalStateException("保存下载配置失败");
        }
        LOGGER.info("保存下载配置成功: configId={}, defaultSaveDir={}",
            downloadConfigDO.getId(), downloadConfigDO.getDefaultSaveDir());
    }
}
