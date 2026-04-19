package com.mooddownload.local.service.task.model;

/**
 * 任务分页查询条件。
 */
public class TaskPageQuery {

    /** 领域状态过滤 */
    private String status;

    /** 关键字 */
    private String keyword;

    /** 页码 */
    private Integer pageNo;

    /** 每页条数 */
    private Integer pageSize;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

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
}
