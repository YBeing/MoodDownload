package com.mooddownload.local.service.task;

import com.mooddownload.local.client.aria2.dto.Aria2TaskFileDTO;
import com.mooddownload.local.common.enums.ErrorCode;
import com.mooddownload.local.common.exception.BizException;
import com.mooddownload.local.service.engine.Aria2CommandService;
import com.mooddownload.local.service.task.model.TorrentFileItem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * BT 文件列表服务，负责读取并规范化 aria2 返回的种子文件清单。
 */
@Service
public class TorrentFileListService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TorrentFileListService.class);

    private final Aria2CommandService aria2CommandService;

    public TorrentFileListService(Aria2CommandService aria2CommandService) {
        this.aria2CommandService = aria2CommandService;
    }

    /**
     * 按引擎 gid 读取 BT 文件列表。
     *
     * @param engineGid aria2 gid
     * @return 规范化后的文件列表
     */
    public List<TorrentFileItem> fetchTorrentFiles(String engineGid) {
        return fetchTorrentFiles(engineGid, false);
    }

    /**
     * 按引擎 gid 读取 BT 文件列表。
     *
     * @param engineGid aria2 gid
     * @param preferActualFileSize 是否优先读取磁盘真实文件大小
     * @return 规范化后的文件列表
     */
    public List<TorrentFileItem> fetchTorrentFiles(String engineGid, boolean preferActualFileSize) {
        if (!StringUtils.hasText(engineGid)) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "engineGid 不能为空");
        }
        List<Aria2TaskFileDTO> taskFileDTOList = aria2CommandService.getFiles(engineGid.trim());
        List<TorrentFileItem> torrentFileItemList = new ArrayList<>();
        for (Aria2TaskFileDTO taskFileDTO : taskFileDTOList) {
            TorrentFileItem torrentFileItem = new TorrentFileItem();
            String normalizedPath = normalizePath(taskFileDTO.getPath());
            torrentFileItem.setFileIndex(parseInteger(taskFileDTO.getIndex()));
            torrentFileItem.setFilePath(normalizedPath);
            torrentFileItem.setFileSizeBytes(resolveFileSizeBytes(
                normalizedPath,
                parseLong(taskFileDTO.getLength()),
                preferActualFileSize
            ));
            torrentFileItem.setSelected("true".equalsIgnoreCase(taskFileDTO.getSelected()));
            torrentFileItemList.add(torrentFileItem);
        }
        LOGGER.info("读取 BT 文件列表成功: gid={}, size={}", engineGid, torrentFileItemList.size());
        return torrentFileItemList;
    }

    private Integer parseInteger(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return 0L;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    private String normalizePath(String path) {
        return StringUtils.hasText(path) ? path.trim() : null;
    }

    private Long resolveFileSizeBytes(String filePath, Long fallbackSizeBytes, boolean preferActualFileSize) {
        if (!preferActualFileSize || !StringUtils.hasText(filePath)) {
            return fallbackSizeBytes;
        }
        try {
            Path path = Paths.get(filePath);
            if (Files.isRegularFile(path)) {
                return Files.size(path);
            }
        } catch (RuntimeException exception) {
            LOGGER.debug("解析 BT 文件真实大小失败，将回退到 aria2 返回值: path={}", filePath, exception);
            return fallbackSizeBytes;
        } catch (Exception exception) {
            LOGGER.debug("读取 BT 文件真实大小失败，将回退到 aria2 返回值: path={}", filePath, exception);
            return fallbackSizeBytes;
        }
        return fallbackSizeBytes;
    }
}
