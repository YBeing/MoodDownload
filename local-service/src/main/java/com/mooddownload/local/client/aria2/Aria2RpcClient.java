package com.mooddownload.local.client.aria2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooddownload.local.client.aria2.dto.Aria2TaskFileDTO;
import com.mooddownload.local.client.aria2.dto.Aria2RpcRequest;
import com.mooddownload.local.client.aria2.dto.Aria2RpcResponse;
import com.mooddownload.local.client.aria2.dto.Aria2TaskStatusDTO;
import com.mooddownload.local.common.enums.ErrorCode;
import com.mooddownload.local.common.exception.BizException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

/**
 * aria2 JSON-RPC 客户端。
 */
@Component
public class Aria2RpcClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(Aria2RpcClient.class);
    private static final MediaType APPLICATION_JSON_RPC = MediaType.valueOf("application/json-rpc");
    private static final List<String> TASK_STATUS_KEYS = Arrays.asList(
        "gid",
        "status",
        "totalLength",
        "completedLength",
        "downloadSpeed",
        "uploadSpeed",
        "errorCode",
        "errorMessage",
        "followedBy",
        "belongsTo"
    );

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper;

    private final Aria2Properties aria2Properties;

    public Aria2RpcClient(
        @Qualifier("aria2RestTemplate") RestTemplate restTemplate,
        ObjectMapper objectMapper,
        Aria2Properties aria2Properties
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.aria2Properties = aria2Properties;
    }

    /**
     * 调用 aria2 创建 URI 下载任务。
     *
     * @param sourceUri 下载地址
     * @param saveDir 保存目录
     * @param outFile 输出文件名
     * @return aria2 gid
     */
    public String addUri(String sourceUri, String saveDir, String outFile) {
        validateEnabled();
        if (!StringUtils.hasText(sourceUri)) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "sourceUri 不能为空");
        }
        Map<String, Object> options = buildOptions(saveDir, outFile);
        List<Object> params = new ArrayList<>();
        params.add(Collections.singletonList(sourceUri));
        params.add(options);
        JsonNode result = invoke("aria2.addUri", params);
        String gid = result.asText();
        LOGGER.info("aria2 addUri 调用成功: gid={}, saveDir={}", gid, saveDir);
        return gid;
    }

    /**
     * 调用 aria2 创建种子下载任务。
     *
     * @param torrentFilePath 种子文件路径
     * @param saveDir 保存目录
     * @param outFile 输出文件名
     * @return aria2 gid
     */
    public String addTorrent(String torrentFilePath, String saveDir, String outFile) {
        validateEnabled();
        if (!StringUtils.hasText(torrentFilePath)) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "torrentFilePath 不能为空");
        }
        String encodedTorrent = encodeTorrentFile(torrentFilePath);
        Map<String, Object> options = buildOptions(saveDir, outFile);
        List<Object> params = new ArrayList<>();
        params.add(encodedTorrent);
        params.add(Collections.emptyList());
        params.add(options);
        JsonNode result = invoke("aria2.addTorrent", params);
        String gid = result.asText();
        LOGGER.info("aria2 addTorrent 调用成功: gid={}, torrentFilePath={}", gid, torrentFilePath);
        return gid;
    }

    /**
     * 查询单个 aria2 任务状态。
     *
     * @param gid aria2 gid
     * @return 任务状态 DTO
     */
    public Aria2TaskStatusDTO tellStatus(String gid) {
        validateEnabled();
        if (!StringUtils.hasText(gid)) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "gid 不能为空");
        }
        List<Object> params = new ArrayList<>();
        params.add(gid);
        params.add(TASK_STATUS_KEYS);
        JsonNode result = invoke("aria2.tellStatus", params);
        return objectMapper.convertValue(result, Aria2TaskStatusDTO.class);
    }

    /**
     * 查询任务内文件列表。
     *
     * @param gid aria2 gid
     * @return 文件列表
     */
    public List<Aria2TaskFileDTO> getFiles(String gid) {
        validateEnabled();
        if (!StringUtils.hasText(gid)) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "gid 不能为空");
        }
        JsonNode result = invoke("aria2.getFiles", Collections.<Object>singletonList(gid));
        List<Aria2TaskFileDTO> taskFileDTOList = new ArrayList<>();
        if (result == null || !result.isArray()) {
            return taskFileDTOList;
        }
        for (JsonNode itemNode : result) {
            taskFileDTOList.add(objectMapper.convertValue(itemNode, Aria2TaskFileDTO.class));
        }
        return taskFileDTOList;
    }

    /**
     * 从 aria2 中移除任务。
     *
     * @param gid aria2 gid
     * @return 被移除的 gid
     */
    public String remove(String gid) {
        validateEnabled();
        if (!StringUtils.hasText(gid)) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "gid 不能为空");
        }
        JsonNode result = invoke("aria2.remove", Collections.<Object>singletonList(gid));
        String removedGid = result.asText();
        LOGGER.info("aria2 remove 调用成功: gid={}", removedGid);
        return removedGid;
    }

    /**
     * 强制从 aria2 中移除任务。
     *
     * @param gid aria2 gid
     * @return 被移除的 gid
     */
    public String forceRemove(String gid) {
        validateEnabled();
        if (!StringUtils.hasText(gid)) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "gid 不能为空");
        }
        JsonNode result = invoke("aria2.forceRemove", Collections.<Object>singletonList(gid));
        String removedGid = result.asText();
        LOGGER.info("aria2 forceRemove 调用成功: gid={}", removedGid);
        return removedGid;
    }

    /**
     * 从 aria2 内存结果集中移除已完成/失败/已删除任务。
     *
     * @param gid aria2 gid
     * @return RPC 返回结果
     */
    public String removeDownloadResult(String gid) {
        validateEnabled();
        if (!StringUtils.hasText(gid)) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "gid 不能为空");
        }
        JsonNode result = invoke("aria2.removeDownloadResult", Collections.<Object>singletonList(gid));
        String rpcResult = result.asText();
        LOGGER.info("aria2 removeDownloadResult 调用成功: gid={}, result={}", gid, rpcResult);
        return rpcResult;
    }

    /**
     * 暂停 aria2 中的下载任务。
     *
     * @param gid aria2 gid
     * @return 被暂停的 gid
     */
    public String pause(String gid) {
        validateEnabled();
        if (!StringUtils.hasText(gid)) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "gid 不能为空");
        }
        JsonNode result = invoke("aria2.pause", Collections.<Object>singletonList(gid));
        String pausedGid = result.asText();
        LOGGER.info("aria2 pause 调用成功: gid={}", pausedGid);
        return pausedGid;
    }

    /**
     * 强制暂停 aria2 中的下载任务。
     *
     * @param gid aria2 gid
     * @return 被暂停的 gid
     */
    public String forcePause(String gid) {
        validateEnabled();
        if (!StringUtils.hasText(gid)) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "gid 不能为空");
        }
        JsonNode result = invoke("aria2.forcePause", Collections.<Object>singletonList(gid));
        String pausedGid = result.asText();
        LOGGER.info("aria2 forcePause 调用成功: gid={}", pausedGid);
        return pausedGid;
    }

    /**
     * 恢复 aria2 中已暂停的下载任务。
     *
     * @param gid aria2 gid
     * @return 被恢复的 gid
     */
    public String unpause(String gid) {
        validateEnabled();
        if (!StringUtils.hasText(gid)) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "gid 不能为空");
        }
        JsonNode result = invoke("aria2.unpause", Collections.<Object>singletonList(gid));
        String unpausedGid = result.asText();
        LOGGER.info("aria2 unpause 调用成功: gid={}", unpausedGid);
        return unpausedGid;
    }

    /**
     * 更新 aria2 全局配置。
     *
     * @param options 全局配置项
     * @return RPC 返回结果
     */
    public String changeGlobalOption(Map<String, String> options) {
        validateEnabled();
        if (options == null || options.isEmpty()) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "options 不能为空");
        }
        JsonNode result = invoke("aria2.changeGlobalOption", Collections.<Object>singletonList(options));
        String rpcResult = result.asText();
        LOGGER.info("aria2 changeGlobalOption 调用成功: optionSize={}, result={}", options.size(), rpcResult);
        return rpcResult;
    }

    /**
     * 查询活跃任务列表。
     *
     * @return 任务状态列表
     */
    public List<Aria2TaskStatusDTO> tellActive() {
        validateEnabled();
        return readTaskList("aria2.tellActive", Collections.<Object>singletonList(TASK_STATUS_KEYS));
    }

    /**
     * 查询等待中的任务列表。
     *
     * @param offset 偏移量
     * @param maxResults 最大条数
     * @return 任务状态列表
     */
    public List<Aria2TaskStatusDTO> tellWaiting(int offset, int maxResults) {
        validateEnabled();
        List<Object> params = new ArrayList<>();
        params.add(offset);
        params.add(maxResults);
        params.add(TASK_STATUS_KEYS);
        return readTaskList("aria2.tellWaiting", params);
    }

    /**
     * 查询已停止任务列表。
     *
     * @param offset 偏移量
     * @param maxResults 最大条数
     * @return 任务状态列表
     */
    public List<Aria2TaskStatusDTO> tellStopped(int offset, int maxResults) {
        validateEnabled();
        List<Object> params = new ArrayList<>();
        params.add(offset);
        params.add(maxResults);
        params.add(TASK_STATUS_KEYS);
        return readTaskList("aria2.tellStopped", params);
    }

    private List<Aria2TaskStatusDTO> readTaskList(String method, List<Object> params) {
        JsonNode result = invoke(method, params);
        List<Aria2TaskStatusDTO> taskStatusDTOList = new ArrayList<>();
        if (result == null || !result.isArray()) {
            return taskStatusDTOList;
        }
        for (JsonNode itemNode : result) {
            taskStatusDTOList.add(objectMapper.convertValue(itemNode, Aria2TaskStatusDTO.class));
        }
        return taskStatusDTOList;
    }

    private JsonNode invoke(String method, List<Object> originalParams) {
        Aria2RpcRequest rpcRequest = new Aria2RpcRequest();
        rpcRequest.setId(UUID.randomUUID().toString());
        rpcRequest.setMethod(method);
        rpcRequest.setParams(buildParams(originalParams));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(APPLICATION_JSON_RPC, MediaType.APPLICATION_JSON));
        HttpEntity<Aria2RpcRequest> requestEntity = new HttpEntity<>(rpcRequest, headers);
        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                aria2Properties.getRpcUrl(),
                requestEntity,
                String.class
            );
            Aria2RpcResponse rpcResponse = parseRpcResponse(method, responseEntity.getBody());
            if (rpcResponse == null) {
                throw new BizException(ErrorCode.EXTERNAL_ENGINE_ERROR, "aria2 RPC 未返回响应");
            }
            if (rpcResponse.getError() != null) {
                LOGGER.warn("aria2 RPC 返回错误: method={}, code={}, message={}",
                    method,
                    rpcResponse.getError().getCode(),
                    rpcResponse.getError().getMessage());
                throw new BizException(
                    ErrorCode.EXTERNAL_ENGINE_ERROR,
                    "aria2 RPC 调用失败: " + rpcResponse.getError().getMessage()
                );
            }
            return rpcResponse.getResult();
        } catch (RestClientResponseException exception) {
            Aria2RpcResponse rpcResponse = parseRpcResponse(method, exception.getResponseBodyAsString());
            if (rpcResponse != null && rpcResponse.getError() != null) {
                LOGGER.warn("aria2 RPC HTTP 错误响应: method={}, statusCode={}, code={}, message={}",
                    method,
                    exception.getRawStatusCode(),
                    rpcResponse.getError().getCode(),
                    rpcResponse.getError().getMessage());
                throw new BizException(
                    ErrorCode.EXTERNAL_ENGINE_ERROR,
                    "aria2 RPC 调用失败: " + rpcResponse.getError().getMessage()
                );
            }
            LOGGER.error("aria2 RPC HTTP 调用异常: method={}, statusCode={}",
                method, exception.getRawStatusCode(), exception);
            throw new BizException(ErrorCode.EXTERNAL_ENGINE_ERROR, "aria2 RPC 调用异常");
        } catch (RestClientException exception) {
            LOGGER.error("aria2 RPC 调用异常: method={}", method, exception);
            throw new BizException(ErrorCode.EXTERNAL_ENGINE_ERROR, "aria2 RPC 调用异常");
        }
    }

    /**
     * 解析 aria2 JSON-RPC 响应，兼容 application/json-rpc 等非标准媒体类型。
     *
     * @param method RPC 方法名
     * @param responseBody 响应体
     * @return 解析后的 RPC 响应
     */
    private Aria2RpcResponse parseRpcResponse(String method, String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return null;
        }
        try {
            return objectMapper.readValue(responseBody, Aria2RpcResponse.class);
        } catch (IOException exception) {
            LOGGER.error("aria2 RPC 响应解析异常: method={}", method, exception);
            throw new BizException(ErrorCode.EXTERNAL_ENGINE_ERROR, "aria2 RPC 响应解析异常");
        }
    }

    private List<Object> buildParams(List<Object> originalParams) {
        List<Object> params = new ArrayList<>();
        if (StringUtils.hasText(aria2Properties.getRpcSecret())) {
            params.add("token:" + aria2Properties.getRpcSecret().trim());
        }
        if (originalParams != null) {
            params.addAll(originalParams);
        }
        return params;
    }

    private Map<String, Object> buildOptions(String saveDir, String outFile) {
        Map<String, Object> options = new LinkedHashMap<>();
        if (StringUtils.hasText(saveDir)) {
            options.put("dir", saveDir.trim());
        }
        if (StringUtils.hasText(outFile)) {
            options.put("out", outFile.trim());
        }
        return options;
    }

    private String encodeTorrentFile(String torrentFilePath) {
        try {
            byte[] torrentContent = Files.readAllBytes(Paths.get(torrentFilePath));
            return Base64.getEncoder().encodeToString(torrentContent);
        } catch (IOException exception) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "无法读取种子文件: " + torrentFilePath);
        }
    }

    private void validateEnabled() {
        if (!aria2Properties.isEnabled()) {
            throw new BizException(ErrorCode.EXTERNAL_ENGINE_ERROR, "aria2 RPC 未启用");
        }
    }
}
