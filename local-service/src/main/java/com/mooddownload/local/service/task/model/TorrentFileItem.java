package com.mooddownload.local.service.task.model;

import java.util.Objects;

/**
 * BT 种子内文件项。
 */
public class TorrentFileItem {

    /** 文件索引 */
    private Integer fileIndex;

    /** 文件路径 */
    private String filePath;

    /** 文件大小，字节 */
    private Long fileSizeBytes;

    /** 是否被选中下载 */
    private Boolean selected;

    public Integer getFileIndex() {
        return fileIndex;
    }

    public void setFileIndex(Integer fileIndex) {
        this.fileIndex = fileIndex;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof TorrentFileItem)) {
            return false;
        }
        TorrentFileItem that = (TorrentFileItem) object;
        return Objects.equals(fileIndex, that.fileIndex)
            && Objects.equals(filePath, that.filePath)
            && Objects.equals(fileSizeBytes, that.fileSizeBytes)
            && Objects.equals(selected, that.selected);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileIndex, filePath, fileSizeBytes, selected);
    }
}
