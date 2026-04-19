package com.mooddownload.local.client.aria2;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * aria2 RPC 配置。
 */
@ConfigurationProperties(prefix = "mooddownload.aria2")
public class Aria2Properties {

    /** 是否启用 aria2 RPC 调用 */
    private boolean enabled = true;

    /** aria2 JSON-RPC 地址 */
    private String rpcUrl = "http://127.0.0.1:6800/jsonrpc";

    /** aria2 RPC 密钥 */
    private String rpcSecret;

    /** 连接超时时间，毫秒 */
    private int connectTimeoutMillis = 2000;

    /** 读取超时时间，毫秒 */
    private int readTimeoutMillis = 5000;

    /** 单次调度批量大小 */
    private int dispatchBatchSize = 10;

    /** 单次同步批量大小 */
    private int syncBatchSize = 100;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRpcUrl() {
        return rpcUrl;
    }

    public void setRpcUrl(String rpcUrl) {
        this.rpcUrl = rpcUrl;
    }

    public String getRpcSecret() {
        return rpcSecret;
    }

    public void setRpcSecret(String rpcSecret) {
        this.rpcSecret = rpcSecret;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public int getDispatchBatchSize() {
        return dispatchBatchSize;
    }

    public void setDispatchBatchSize(int dispatchBatchSize) {
        this.dispatchBatchSize = dispatchBatchSize;
    }

    public int getSyncBatchSize() {
        return syncBatchSize;
    }

    public void setSyncBatchSize(int syncBatchSize) {
        this.syncBatchSize = syncBatchSize;
    }
}
