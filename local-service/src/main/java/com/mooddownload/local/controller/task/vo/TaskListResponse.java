package com.mooddownload.local.controller.task.vo;

import java.util.List;

/**
 * 任务列表响应。
 */
public class TaskListResponse {

    /** 页码 */
    private Integer pageNo;

    /** 每页条数 */
    private Integer pageSize;

    /** 总记录数 */
    private Long total;

    /** 当前页数据 */
    private List<TaskListItemVO> items;

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<TaskListItemVO> getItems() {
        return items;
    }

    public void setItems(List<TaskListItemVO> items) {
        this.items = items;
    }
}
