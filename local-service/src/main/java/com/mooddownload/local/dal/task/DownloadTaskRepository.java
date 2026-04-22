package com.mooddownload.local.dal.task;

import com.mooddownload.local.mapper.task.DownloadTaskDO;
import com.mooddownload.local.mapper.task.DownloadTaskMapper;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 * 下载任务 Repository。
 */
@Repository
public class DownloadTaskRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadTaskRepository.class);

    private static final String DEFAULT_ENTRY_TYPE = "MANUAL";

    private static final String DEFAULT_SOURCE_PROVIDER = "GENERIC";

    private static final String DEFAULT_ENGINE_PROFILE_CODE = "default";

    private final DownloadTaskMapper downloadTaskMapper;

    public DownloadTaskRepository(DownloadTaskMapper downloadTaskMapper) {
        this.downloadTaskMapper = downloadTaskMapper;
    }

    /**
     * 保存下载任务。
     *
     * @param downloadTaskDO 下载任务持久化对象
     * @return 任务主键
     */
    public Long save(DownloadTaskDO downloadTaskDO) {
        normalizeBeforePersist(downloadTaskDO);
        int affectedRows = downloadTaskMapper.insert(downloadTaskDO);
        if (affectedRows != 1 || downloadTaskDO.getId() == null) {
            LOGGER.error("写入下载任务失败: taskCode={}", downloadTaskDO.getTaskCode());
            throw new IllegalStateException("写入下载任务失败");
        }
        LOGGER.info("写入下载任务成功: taskId={}, taskCode={}", downloadTaskDO.getId(), downloadTaskDO.getTaskCode());
        return downloadTaskDO.getId();
    }

    /**
     * 按主键查询下载任务。
     *
     * @param id 主键 ID
     * @return 下载任务持久化对象
     */
    public Optional<DownloadTaskDO> findById(Long id) {
        return Optional.ofNullable(downloadTaskMapper.selectById(id));
    }

    /**
     * 按幂等键查询下载任务。
     *
     * @param clientRequestId 幂等键
     * @return 下载任务持久化对象
     */
    public Optional<DownloadTaskDO> findByClientRequestId(String clientRequestId) {
        return Optional.ofNullable(downloadTaskMapper.selectByClientRequestId(clientRequestId));
    }

    /**
     * 按来源哈希查询可复用任务。
     *
     * @param sourceHash 来源哈希
     * @return 下载任务持久化对象
     */
    public Optional<DownloadTaskDO> findReusableBySourceHash(String sourceHash) {
        return Optional.ofNullable(downloadTaskMapper.selectReusableBySourceHash(sourceHash));
    }

    /**
     * 按 aria2 gid 查询下载任务。
     *
     * @param engineGid aria2 gid
     * @return 下载任务持久化对象
     */
    public Optional<DownloadTaskDO> findByEngineGid(String engineGid) {
        return Optional.ofNullable(downloadTaskMapper.selectByEngineGid(engineGid));
    }

    /**
     * 按领域状态查询任务列表。
     *
     * @param domainStatus 领域状态
     * @param limit 最大条数
     * @return 任务列表
     */
    public List<DownloadTaskDO> listByDomainStatus(String domainStatus, Integer limit) {
        List<DownloadTaskDO> downloadTaskDOList = downloadTaskMapper.selectByDomainStatus(domainStatus, limit);
        LOGGER.info("按领域状态查询任务成功: domainStatus={}, limit={}, size={}",
            domainStatus, limit, downloadTaskDOList.size());
        return downloadTaskDOList.stream().collect(Collectors.toList());
    }

    /**
     * 按条件分页查询任务。
     *
     * @param status 状态过滤
     * @param keyword 关键字
     * @param offset 偏移量
     * @param limit 最大条数
     * @return 任务列表
     */
    public List<DownloadTaskDO> searchPage(String status, String keyword, Integer offset, Integer limit) {
        List<DownloadTaskDO> downloadTaskDOList = downloadTaskMapper.selectPageByCondition(status, keyword, offset, limit);
        LOGGER.info("分页查询任务成功: status={}, keyword={}, offset={}, limit={}, size={}",
            status, keyword, offset, limit, downloadTaskDOList.size());
        return downloadTaskDOList.stream().collect(Collectors.toList());
    }

    /**
     * 查询全部未删除任务。
     *
     * @return 任务列表
     */
    public List<DownloadTaskDO> listAllActiveTasks() {
        List<DownloadTaskDO> downloadTaskDOList = downloadTaskMapper.selectAllActiveTasks();
        LOGGER.info("查询全部未删除任务成功: size={}", downloadTaskDOList.size());
        return downloadTaskDOList.stream().collect(Collectors.toList());
    }

    /**
     * 按条件统计任务总数。
     *
     * @param status 状态过滤
     * @param keyword 关键字
     * @return 总数
     */
    public long countByCondition(String status, String keyword) {
        long total = downloadTaskMapper.countByCondition(status, keyword);
        LOGGER.info("统计任务总数成功: status={}, keyword={}, total={}", status, keyword, total);
        return total;
    }

    /**
     * 更新任务运行快照。
     *
     * @param downloadTaskDO 下载任务持久化对象
     */
    public void updateCoreSnapshot(DownloadTaskDO downloadTaskDO) {
        normalizeBeforePersist(downloadTaskDO);
        int affectedRows = downloadTaskMapper.updateCoreSnapshot(downloadTaskDO);
        if (affectedRows != 1) {
            LOGGER.error("更新下载任务快照失败: taskId={}, taskCode={}",
                downloadTaskDO.getId(), downloadTaskDO.getTaskCode());
            throw new IllegalStateException("更新下载任务快照失败");
        }
        LOGGER.info("更新下载任务快照成功: taskId={}, taskCode={}, domainStatus={}",
            downloadTaskDO.getId(), downloadTaskDO.getTaskCode(), downloadTaskDO.getDomainStatus());
    }

    private void normalizeBeforePersist(DownloadTaskDO downloadTaskDO) {
        if (downloadTaskDO == null) {
            throw new IllegalArgumentException("downloadTaskDO 不能为空");
        }
        if (downloadTaskDO.getEntryType() == null || downloadTaskDO.getEntryType().trim().isEmpty()) {
            downloadTaskDO.setEntryType(DEFAULT_ENTRY_TYPE);
        }
        if (downloadTaskDO.getSourceProvider() == null || downloadTaskDO.getSourceProvider().trim().isEmpty()) {
            downloadTaskDO.setSourceProvider(DEFAULT_SOURCE_PROVIDER);
        }
        if (downloadTaskDO.getEngineProfileCode() == null || downloadTaskDO.getEngineProfileCode().trim().isEmpty()) {
            downloadTaskDO.setEngineProfileCode(DEFAULT_ENGINE_PROFILE_CODE);
        }
    }
}
