package com.mooddownload.local.service.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mooddownload.local.client.aria2.dto.Aria2TaskFileDTO;
import com.mooddownload.local.common.enums.ErrorCode;
import com.mooddownload.local.common.exception.BizException;
import com.mooddownload.local.client.aria2.Aria2RpcClient;
import com.mooddownload.local.service.engine.convert.Aria2TaskStatusConverter;
import com.mooddownload.local.service.task.model.DownloadTaskModel;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * aria2 命令服务单元测试。
 */
class Aria2CommandServiceTests {

    private final Aria2RpcClient aria2RpcClient = Mockito.mock(Aria2RpcClient.class);

    private final Aria2CommandService aria2CommandService = new Aria2CommandService(
        aria2RpcClient,
        new Aria2TaskStatusConverter()
    );

    @Test
    void shouldCreateHttpDownloadViaAddUri() {
        DownloadTaskModel downloadTaskModel = buildHttpTask();
        when(aria2RpcClient.addUri("https://example.com/file.iso", "./downloads", null))
            .thenReturn("gid-http");

        assertThat(aria2CommandService.createDownload(downloadTaskModel).getEngineGid()).isEqualTo("gid-http");
        verify(aria2RpcClient).addUri("https://example.com/file.iso", "./downloads", null);
    }

    @Test
    void shouldCreateTorrentDownloadViaAddTorrent() {
        DownloadTaskModel downloadTaskModel = buildTorrentTask();
        when(aria2RpcClient.addTorrent("/tmp/file.torrent", "./downloads", "file.iso"))
            .thenReturn("gid-torrent");

        assertThat(aria2CommandService.createDownload(downloadTaskModel).getEngineGid()).isEqualTo("gid-torrent");
        verify(aria2RpcClient).addTorrent("/tmp/file.torrent", "./downloads", "file.iso");
    }

    @Test
    void shouldIgnoreDisplayNameForHttpsOutFile() {
        DownloadTaskModel downloadTaskModel = buildHttpTask();
        downloadTaskModel.setDisplayName("自定义安装包");
        when(aria2RpcClient.addUri("https://example.com/file.iso", "./downloads", null))
            .thenReturn("gid-http-custom");

        assertThat(aria2CommandService.createDownload(downloadTaskModel).getEngineGid()).isEqualTo("gid-http-custom");
        verify(aria2RpcClient).addUri("https://example.com/file.iso", "./downloads", null);
    }

    @Test
    void shouldRemoveDownloadViaRpcClient() {
        when(aria2RpcClient.remove("gid-remove-1")).thenReturn("gid-remove-1");

        assertThat(aria2CommandService.removeDownload("gid-remove-1")).isEqualTo("gid-remove-1");
        verify(aria2RpcClient).remove("gid-remove-1");
    }

    @Test
    void shouldFallbackToForceRemoveWhenRemoveFails() {
        when(aria2RpcClient.remove("gid-remove-2"))
            .thenThrow(new BizException(ErrorCode.EXTERNAL_ENGINE_ERROR, "aria2 RPC 调用失败: active task only"));
        when(aria2RpcClient.forceRemove("gid-remove-2")).thenReturn("gid-remove-2");

        assertThat(aria2CommandService.removeDownload("gid-remove-2")).isEqualTo("gid-remove-2");
        verify(aria2RpcClient).remove("gid-remove-2");
        verify(aria2RpcClient).forceRemove("gid-remove-2");
    }

    @Test
    void shouldFallbackToRemoveDownloadResultWhenQueueRemovalFails() {
        when(aria2RpcClient.remove("gid-remove-3"))
            .thenThrow(new BizException(ErrorCode.EXTERNAL_ENGINE_ERROR, "aria2 RPC 调用失败: active task not found"));
        when(aria2RpcClient.forceRemove("gid-remove-3"))
            .thenThrow(new BizException(ErrorCode.EXTERNAL_ENGINE_ERROR, "aria2 RPC 调用失败: active task not found"));
        when(aria2RpcClient.removeDownloadResult("gid-remove-3")).thenReturn("OK");

        assertThat(aria2CommandService.removeDownload("gid-remove-3")).isEqualTo("gid-remove-3");
        verify(aria2RpcClient).remove("gid-remove-3");
        verify(aria2RpcClient).forceRemove("gid-remove-3");
        verify(aria2RpcClient).removeDownloadResult("gid-remove-3");
    }

    @Test
    void shouldTreatCannotBePausedNowAsIdempotentPause() {
        when(aria2RpcClient.pause("gid-complete-metadata"))
            .thenThrow(new BizException(
                ErrorCode.EXTERNAL_ENGINE_ERROR,
                "aria2 RPC 调用失败: GID#gid-complete-metadata cannot be paused now"
            ));
        when(aria2RpcClient.forcePause("gid-complete-metadata"))
            .thenThrow(new BizException(
                ErrorCode.EXTERNAL_ENGINE_ERROR,
                "aria2 RPC 调用失败: GID#gid-complete-metadata cannot be paused now"
            ));

        assertThat(aria2CommandService.pauseDownload("gid-complete-metadata")).isEqualTo("gid-complete-metadata");
        verify(aria2RpcClient).pause("gid-complete-metadata");
        verify(aria2RpcClient).forcePause("gid-complete-metadata");
    }

    @Test
    void shouldReadTorrentFilesViaRpcClient() {
        Aria2TaskFileDTO aria2TaskFileDTO = new Aria2TaskFileDTO();
        aria2TaskFileDTO.setPath("/downloads/demo/file-1.mkv");
        when(aria2RpcClient.getFiles("gid-files")).thenReturn(Collections.singletonList(aria2TaskFileDTO));

        assertThat(aria2CommandService.getFiles("gid-files")).hasSize(1);
        verify(aria2RpcClient).getFiles("gid-files");
    }

    private DownloadTaskModel buildHttpTask() {
        DownloadTaskModel downloadTaskModel = new DownloadTaskModel();
        downloadTaskModel.setId(1L);
        downloadTaskModel.setTaskCode("TASK-HTTP");
        downloadTaskModel.setSourceType("HTTP");
        downloadTaskModel.setSourceUri("https://example.com/file.iso");
        downloadTaskModel.setDisplayName("file.iso");
        downloadTaskModel.setSaveDir("./downloads");
        return downloadTaskModel;
    }

    private DownloadTaskModel buildTorrentTask() {
        DownloadTaskModel downloadTaskModel = buildHttpTask();
        downloadTaskModel.setSourceType("TORRENT");
        downloadTaskModel.setTorrentFilePath("/tmp/file.torrent");
        return downloadTaskModel;
    }
}
