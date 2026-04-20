package com.mooddownload.local.service.capture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.mooddownload.local.bootstrap.LocalServiceApplication;
import com.mooddownload.local.client.aria2.Aria2RpcClient;
import com.mooddownload.local.common.enums.ErrorCode;
import com.mooddownload.local.common.exception.BizException;
import com.mooddownload.local.dal.config.DownloadConfigRepository;
import com.mooddownload.local.dal.task.DownloadTaskRepository;
import com.mooddownload.local.mapper.config.DownloadConfigDO;
import com.mooddownload.local.mapper.task.DownloadTaskDO;
import com.mooddownload.local.service.capture.model.CaptureTaskResult;
import com.mooddownload.local.service.capture.model.ClipboardCaptureCommand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

/**
 * B6 阶段剪贴板接入服务集成测试。
 */
@SpringBootTest(classes = LocalServiceApplication.class)
@ActiveProfiles("test")
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ClipboardCaptureServiceIntegrationTests {

    @Autowired
    private ClipboardCaptureService clipboardCaptureService;

    @Autowired
    private DownloadTaskRepository downloadTaskRepository;

    @Autowired
    private DownloadConfigRepository downloadConfigRepository;

    @MockBean
    private Aria2RpcClient aria2RpcClient;

    @Test
    void shouldCreateTaskFromClipboardText() {
        when(aria2RpcClient.addUri(
            "https://example.com/archive/demo.zip",
            "./downloads",
            null
        )).thenReturn("gid-clipboard-1");

        ClipboardCaptureCommand clipboardCaptureCommand = new ClipboardCaptureCommand();
        clipboardCaptureCommand.setClientRequestId("request-clipboard-1");
        clipboardCaptureCommand.setClipboardText("请帮我下载这个文件：https://example.com/archive/demo.zip.");
        clipboardCaptureCommand.setSuggestedName("demo-from-clipboard.zip");

        CaptureTaskResult captureTaskResult = clipboardCaptureService.capture(clipboardCaptureCommand);

        assertThat(captureTaskResult.getAccepted()).isTrue();
        assertThat(captureTaskResult.getDomainStatus()).isEqualTo("RUNNING");

        DownloadTaskDO downloadTaskDO = downloadTaskRepository.findByClientRequestId("request-clipboard-1")
            .orElseThrow(() -> new IllegalStateException("未查询到剪贴板接入创建的任务"));
        assertThat(downloadTaskDO.getSourceType()).isEqualTo("HTTPS");
        assertThat(downloadTaskDO.getSourceUri()).isEqualTo("https://example.com/archive/demo.zip");
        assertThat(downloadTaskDO.getDisplayName()).isEqualTo("demo-from-clipboard.zip");
        assertThat(downloadTaskDO.getSaveDir()).isEqualTo("./downloads");
        assertThat(downloadTaskDO.getEngineGid()).isEqualTo("gid-clipboard-1");
    }

    @Test
    void shouldRejectClipboardCaptureWhenMonitorDisabled() {
        DownloadConfigDO downloadConfigDO = downloadConfigRepository.findSingleton()
            .orElseThrow(() -> new IllegalStateException("未查询到默认配置"));
        downloadConfigDO.setClipboardMonitorEnabled(0);
        downloadConfigDO.setUpdatedAt(System.currentTimeMillis());
        downloadConfigRepository.saveOrUpdate(downloadConfigDO);

        ClipboardCaptureCommand clipboardCaptureCommand = new ClipboardCaptureCommand();
        clipboardCaptureCommand.setClientRequestId("request-clipboard-2");
        clipboardCaptureCommand.setClipboardText("magnet:?xt=urn:btih:ABCDEF123456");

        assertThatThrownBy(() -> clipboardCaptureService.capture(clipboardCaptureCommand))
            .isInstanceOf(BizException.class)
            .satisfies(throwable -> {
                BizException bizException = (BizException) throwable;
                assertThat(bizException.getErrorCode()).isEqualTo(ErrorCode.CAPTURE_DISABLED);
                assertThat(bizException.getMessage()).isEqualTo("剪贴板监听未开启");
            });
    }

    @Test
    void shouldRejectUnsupportedClipboardContent() {
        ClipboardCaptureCommand clipboardCaptureCommand = new ClipboardCaptureCommand();
        clipboardCaptureCommand.setClientRequestId("request-clipboard-3");
        clipboardCaptureCommand.setClipboardText("这里只是一段普通文本，没有下载链接");

        assertThatThrownBy(() -> clipboardCaptureService.capture(clipboardCaptureCommand))
            .isInstanceOf(BizException.class)
            .satisfies(throwable -> {
                BizException bizException = (BizException) throwable;
                assertThat(bizException.getErrorCode()).isEqualTo(ErrorCode.COMMON_PARAM_INVALID);
                assertThat(bizException.getMessage()).isEqualTo("剪贴板内容中未识别到受支持的下载地址");
            });
    }
}
