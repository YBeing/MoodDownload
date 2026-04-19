package com.mooddownload.local.service.task.state;

import java.util.Arrays;

/**
 * 任务来源类型枚举。
 */
public enum TaskSourceType {

    /** HTTP 下载 */
    HTTP,

    /** HTTPS 下载 */
    HTTPS,

    /** BT 下载 */
    BT,

    /** 磁力链接 */
    MAGNET,

    /** 种子文件 */
    TORRENT;

    /**
     * 按字符串解析来源类型。
     *
     * @param code 类型编码
     * @return 来源类型
     */
    public static TaskSourceType fromCode(String code) {
        return Arrays.stream(values())
            .filter(value -> value.name().equalsIgnoreCase(code))
            .findFirst()
            .orElse(null);
    }
}
