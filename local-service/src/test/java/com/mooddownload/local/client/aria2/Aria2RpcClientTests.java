package com.mooddownload.local.client.aria2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooddownload.local.common.exception.BizException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

/**
 * aria2 RPC 客户端单元测试。
 */
class Aria2RpcClientTests {

    private static final MediaType APPLICATION_JSON_RPC = MediaType.valueOf("application/json-rpc");

    @Test
    void shouldCallAddUriWithJsonRpcPayload() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build();
        Aria2RpcClient aria2RpcClient = new Aria2RpcClient(restTemplate, new ObjectMapper(), buildProperties());

        mockRestServiceServer.expect(requestTo("http://127.0.0.1:6800/jsonrpc"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(allOf(
                containsString("\"method\":\"aria2.addUri\""),
                containsString("\"token:test-secret\""),
                containsString("\"https://example.com/ubuntu.iso\""),
                containsString("\"dir\":\"./downloads\""),
                containsString("\"out\":\"ubuntu.iso\"")
            )))
            .andRespond(withSuccess("{\"jsonrpc\":\"2.0\",\"id\":\"req-1\",\"result\":\"gid-001\"}",
                APPLICATION_JSON_RPC));

        String engineGid = aria2RpcClient.addUri("https://example.com/ubuntu.iso", "./downloads", "ubuntu.iso");

        assertThat(engineGid).isEqualTo("gid-001");
        mockRestServiceServer.verify();
    }

    @Test
    void shouldReadTorrentFileAndCallAddTorrent() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build();
        Aria2RpcClient aria2RpcClient = new Aria2RpcClient(restTemplate, new ObjectMapper(), buildProperties());
        Path torrentFile = Files.createTempFile("aria2-rpc-test", ".torrent");
        Files.write(torrentFile, "torrent-body".getBytes(StandardCharsets.UTF_8));

        mockRestServiceServer.expect(requestTo("http://127.0.0.1:6800/jsonrpc"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(allOf(
                containsString("\"method\":\"aria2.addTorrent\""),
                containsString("\"dG9ycmVudC1ib2R5\""),
                containsString("\"dir\":\"./downloads\"")
            )))
            .andRespond(withSuccess("{\"jsonrpc\":\"2.0\",\"id\":\"req-2\",\"result\":\"gid-002\"}",
                APPLICATION_JSON_RPC));

        String engineGid = aria2RpcClient.addTorrent(torrentFile.toString(), "./downloads", "ubuntu.iso");

        assertThat(engineGid).isEqualTo("gid-002");
        mockRestServiceServer.verify();
    }

    @Test
    void shouldParseTellStatusResponse() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build();
        Aria2RpcClient aria2RpcClient = new Aria2RpcClient(restTemplate, new ObjectMapper(), buildProperties());

        mockRestServiceServer.expect(requestTo("http://127.0.0.1:6800/jsonrpc"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(allOf(
                containsString("\"method\":\"aria2.tellStatus\""),
                containsString("\"followedBy\""),
                containsString("\"belongsTo\"")
            )))
            .andRespond(withSuccess("{\"jsonrpc\":\"2.0\",\"id\":\"req-3\",\"result\":"
                    + "{\"gid\":\"gid-003\",\"status\":\"active\",\"totalLength\":\"1024\","
                    + "\"completedLength\":\"512\",\"downloadSpeed\":\"128\",\"uploadSpeed\":\"16\","
                    + "\"followedBy\":[\"gid-004\"],\"belongsTo\":\"gid-001\"}}",
                APPLICATION_JSON_RPC));

        com.mooddownload.local.client.aria2.dto.Aria2TaskStatusDTO taskStatusDTO = aria2RpcClient.tellStatus("gid-003");
        assertThat(taskStatusDTO.getStatus()).isEqualTo("active");
        assertThat(taskStatusDTO.getFollowedBy()).containsExactly("gid-004");
        mockRestServiceServer.verify();
    }

    @Test
    void shouldRequestRelationshipKeysWhenReadingActiveTasks() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build();
        Aria2RpcClient aria2RpcClient = new Aria2RpcClient(restTemplate, new ObjectMapper(), buildProperties());

        mockRestServiceServer.expect(requestTo("http://127.0.0.1:6800/jsonrpc"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(allOf(
                containsString("\"method\":\"aria2.tellActive\""),
                containsString("\"followedBy\""),
                containsString("\"belongsTo\"")
            )))
            .andRespond(withSuccess("{\"jsonrpc\":\"2.0\",\"id\":\"req-active\",\"result\":["
                    + "{\"gid\":\"gid-010\",\"status\":\"active\",\"totalLength\":\"2048\","
                    + "\"completedLength\":\"1024\",\"downloadSpeed\":\"64\",\"uploadSpeed\":\"0\","
                    + "\"belongsTo\":\"gid-meta\"}]}",
                APPLICATION_JSON_RPC));

        java.util.List<com.mooddownload.local.client.aria2.dto.Aria2TaskStatusDTO> activeTasks =
            aria2RpcClient.tellActive();
        assertThat(activeTasks).hasSize(1);
        assertThat(activeTasks.get(0).getBelongsTo()).isEqualTo("gid-meta");
        mockRestServiceServer.verify();
    }

    @Test
    void shouldParseGetFilesResponse() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build();
        Aria2RpcClient aria2RpcClient = new Aria2RpcClient(restTemplate, new ObjectMapper(), buildProperties());

        mockRestServiceServer.expect(requestTo("http://127.0.0.1:6800/jsonrpc"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(containsString("\"method\":\"aria2.getFiles\"")))
            .andRespond(withSuccess("{\"jsonrpc\":\"2.0\",\"id\":\"req-files\",\"result\":["
                    + "{\"index\":\"1\",\"path\":\"/downloads/demo/file-1.mkv\",\"length\":\"1024\",\"selected\":\"true\"},"
                    + "{\"index\":\"2\",\"path\":\"/downloads/demo/file-2.srt\",\"length\":\"128\",\"selected\":\"false\"}"
                    + "]}",
                APPLICATION_JSON_RPC));

        java.util.List<com.mooddownload.local.client.aria2.dto.Aria2TaskFileDTO> taskFileDTOList =
            aria2RpcClient.getFiles("gid-files");
        assertThat(taskFileDTOList).hasSize(2);
        assertThat(taskFileDTOList.get(0).getPath()).isEqualTo("/downloads/demo/file-1.mkv");
        mockRestServiceServer.verify();
    }

    @Test
    void shouldCallRemoveWithJsonRpcPayload() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build();
        Aria2RpcClient aria2RpcClient = new Aria2RpcClient(restTemplate, new ObjectMapper(), buildProperties());

        mockRestServiceServer.expect(requestTo("http://127.0.0.1:6800/jsonrpc"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(allOf(
                containsString("\"method\":\"aria2.remove\""),
                containsString("\"token:test-secret\""),
                containsString("\"gid-remove-1\"")
            )))
            .andRespond(withSuccess("{\"jsonrpc\":\"2.0\",\"id\":\"req-4\",\"result\":\"gid-remove-1\"}",
                APPLICATION_JSON_RPC));

        assertThat(aria2RpcClient.remove("gid-remove-1")).isEqualTo("gid-remove-1");
        mockRestServiceServer.verify();
    }

    @Test
    void shouldCallForceRemoveWithJsonRpcPayload() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build();
        Aria2RpcClient aria2RpcClient = new Aria2RpcClient(restTemplate, new ObjectMapper(), buildProperties());

        mockRestServiceServer.expect(requestTo("http://127.0.0.1:6800/jsonrpc"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(containsString("\"method\":\"aria2.forceRemove\"")))
            .andRespond(withSuccess("{\"jsonrpc\":\"2.0\",\"id\":\"req-5\",\"result\":\"gid-force-1\"}",
                APPLICATION_JSON_RPC));

        assertThat(aria2RpcClient.forceRemove("gid-force-1")).isEqualTo("gid-force-1");
        mockRestServiceServer.verify();
    }

    @Test
    void shouldCallRemoveDownloadResultWithJsonRpcPayload() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build();
        Aria2RpcClient aria2RpcClient = new Aria2RpcClient(restTemplate, new ObjectMapper(), buildProperties());

        mockRestServiceServer.expect(requestTo("http://127.0.0.1:6800/jsonrpc"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(containsString("\"method\":\"aria2.removeDownloadResult\"")))
            .andRespond(withSuccess("{\"jsonrpc\":\"2.0\",\"id\":\"req-6\",\"result\":\"OK\"}",
                APPLICATION_JSON_RPC));

        assertThat(aria2RpcClient.removeDownloadResult("gid-result-1")).isEqualTo("OK");
        mockRestServiceServer.verify();
    }

    @Test
    void shouldExtractRpcErrorMessageFromHttpErrorResponse() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build();
        Aria2RpcClient aria2RpcClient = new Aria2RpcClient(restTemplate, new ObjectMapper(), buildProperties());

        mockRestServiceServer.expect(requestTo("http://127.0.0.1:6800/jsonrpc"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(MockRestResponseCreators.withStatus(HttpStatus.BAD_REQUEST)
                .contentType(APPLICATION_JSON_RPC)
                .body("{\"jsonrpc\":\"2.0\",\"id\":\"req-7\",\"error\":{\"code\":1,\"message\":\"GID not found\"}}"));

        assertThatThrownBy(() -> aria2RpcClient.remove("gid-missing"))
            .isInstanceOf(BizException.class)
            .hasMessage("aria2 RPC 调用失败: GID not found");
        mockRestServiceServer.verify();
    }

    private Aria2Properties buildProperties() {
        Aria2Properties aria2Properties = new Aria2Properties();
        aria2Properties.setEnabled(true);
        aria2Properties.setRpcUrl("http://127.0.0.1:6800/jsonrpc");
        aria2Properties.setRpcSecret("test-secret");
        return aria2Properties;
    }
}
