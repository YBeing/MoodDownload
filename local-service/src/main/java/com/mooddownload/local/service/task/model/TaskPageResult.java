package com.mooddownload.local.service.task.model;

import java.util.List;

/**
 * 任务分页查询结果。
 */
public class TaskPageResult {

    /** 页码 */
    private Integer pageNo;

    /** 每页条数 */
    private Integer pageSize;

    /** 总记录数 */
    private Long total;

    /** 当前页任务 */
    private List<DownloadTaskModel> items;

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

    public List<DownloadTaskModel> getItems() {
        return items;
    }

    public void setItems(List<DownloadTaskModel> items) {
        this.items = items;
    }
}
