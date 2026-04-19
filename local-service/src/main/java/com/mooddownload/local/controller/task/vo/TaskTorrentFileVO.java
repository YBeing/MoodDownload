package com.mooddownload.local.controller.task.vo;

/**
 * BT 文件列表项响应。
 */
public class TaskTorrentFileVO {

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
}
