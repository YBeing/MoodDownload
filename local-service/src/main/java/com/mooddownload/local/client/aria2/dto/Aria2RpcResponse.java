package com.mooddownload.local.client.aria2.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * aria2 JSON-RPC 响应体。
 */
public class Aria2RpcResponse {

    /** JSON-RPC 协议版本 */
    private String jsonrpc;

    /** 请求 ID */
    private String id;

    /** 正常结果 */
    private JsonNode result;

    /** 错误信息 */
    private Aria2RpcError error;

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

    public JsonNode getResult() {
        return result;
    }

    public void setResult(JsonNode result) {
        this.result = result;
    }

    public Aria2RpcError getError() {
        return error;
    }

    public void setError(Aria2RpcError error) {
        this.error = error;
    }
}
