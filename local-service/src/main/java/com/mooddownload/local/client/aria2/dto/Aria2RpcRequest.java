package com.mooddownload.local.client.aria2.dto;

import java.util.List;

/**
 * aria2 JSON-RPC 请求体。
 */
public class Aria2RpcRequest {

    /** JSON-RPC 协议版本 */
    private String jsonrpc = "2.0";

    /** 请求 ID */
    private String id;

    /** 调用方法名 */
    private String method;

    /** 调用参数 */
    private List<Object> params;

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public List<Object> getParams() {
        return params;
    }

    public void setParams(List<Object> params) {
        this.params = params;
    }
}
