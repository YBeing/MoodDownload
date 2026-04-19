package com.mooddownload.local.client.aria2.dto;

/**
 * aria2 JSON-RPC 错误体。
 */
public class Aria2RpcError {

    /** 错误码 */
    private Integer code;

    /** 错误消息 */
    private String message;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
