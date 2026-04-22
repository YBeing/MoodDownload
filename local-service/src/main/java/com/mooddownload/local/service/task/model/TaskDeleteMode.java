package com.mooddownload.local.service.task.model;

import java.util.Arrays;

/**
 * 任务删除模式枚举。
 */
public enum TaskDeleteMode {

    /** 仅删除任务记录 */
    TASK_ONLY,

    /** 删除任务记录和输出文件 */
    TASK_AND_OUTPUT,

    /** 删除任务记录和全部关联工件 */
    TASK_AND_ALL_ARTIFACTS;

    /**
     * 解析删除模式。
     *
     * @param code 模式编码
     * @return 删除模式
     */
    public static TaskDeleteMode fromCode(String code) {
        return Arrays.stream(values())
            .filter(item -> item.name().equalsIgnoreCase(code))
            .findFirst()
            .orElse(null);
    }
}
