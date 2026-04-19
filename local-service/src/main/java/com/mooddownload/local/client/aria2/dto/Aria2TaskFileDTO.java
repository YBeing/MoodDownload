package com.mooddownload.local.client.aria2.dto;

/**
 * aria2 任务文件明细 DTO。
 */
public class Aria2TaskFileDTO {

    /** 文件绝对路径 */
    private String path;

    /** 文件大小字符串 */
    private String length;

    /** 文件索引 */
    private String index;

    /** 是否被选中下载 */
    private String selected;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = length;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getSelected() {
        return selected;
    }

    public void setSelected(String selected) {
        this.selected = selected;
    }
}
