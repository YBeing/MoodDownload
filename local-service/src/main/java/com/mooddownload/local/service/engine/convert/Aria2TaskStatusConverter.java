package com.mooddownload.local.service.engine.convert;

import com.mooddownload.local.client.aria2.dto.Aria2TaskStatusDTO;
import com.mooddownload.local.service.engine.model.EngineTaskSnapshot;
import com.mooddownload.local.service.task.state.DownloadTaskStatus;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * aria2 任务状态转换器。
 */
@Component
public class Aria2TaskStatusConverter {

    /**
     * 将 aria2 DTO 转换为引擎快照。
     *
     * @param aria2TaskStatusDTO aria2 任务状态 DTO
     * @return 引擎快照
     */
    public EngineTaskSnapshot toSnapshot(Aria2TaskStatusDTO aria2TaskStatusDTO) {
        EngineTaskSnapshot engineTaskSnapshot = new EngineTaskSnapshot();
        engineTaskSnapshot.setEngineGid(aria2TaskStatusDTO.getGid());
        engineTaskSnapshot.setEngineStatus(aria2TaskStatusDTO.getStatus());
        engineTaskSnapshot.setTotalSizeBytes(parseLong(aria2TaskStatusDTO.getTotalLength()));
        engineTaskSnapshot.setCompletedSizeBytes(parseLong(aria2TaskStatusDTO.getCompletedLength()));
        engineTaskSnapshot.setDownloadSpeedBps(parseLong(aria2TaskStatusDTO.getDownloadSpeed()));
        engineTaskSnapshot.setUploadSpeedBps(parseLong(aria2TaskStatusDTO.getUploadSpeed()));
        engineTaskSnapshot.setErrorCode(aria2TaskStatusDTO.getErrorCode());
        engineTaskSnapshot.setErrorMessage(aria2TaskStatusDTO.getErrorMessage());
        engineTaskSnapshot.setFollowedBy(aria2TaskStatusDTO.getFollowedBy());
        engineTaskSnapshot.setBelongsTo(aria2TaskStatusDTO.getBelongsTo());
        return engineTaskSnapshot;
    }

    /**
     * 将 aria2 状态映射为领域状态。
     *
     * @param engineStatus aria2 状态
     * @return 领域状态
     */
    public DownloadTaskStatus toDomainStatus(String engineStatus) {
        if (!StringUtils.hasText(engineStatus)) {
            return DownloadTaskStatus.RECONCILING;
        }
        switch (engineStatus.trim().toLowerCase(Locale.ROOT)) {
            case "active":
                return DownloadTaskStatus.RUNNING;
            case "waiting":
                return DownloadTaskStatus.PENDING;
            case "paused":
                return DownloadTaskStatus.PAUSED;
            case "error":
                return DownloadTaskStatus.FAILED;
            case "complete":
                return DownloadTaskStatus.COMPLETED;
            case "removed":
                return DownloadTaskStatus.CANCELLED;
            default:
                return DownloadTaskStatus.RECONCILING;
        }
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }
}
