package com.mooddownload.local.service.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.mooddownload.local.client.aria2.Aria2Properties;
import com.mooddownload.local.client.aria2.Aria2RpcClient;
import com.mooddownload.local.client.aria2.dto.Aria2TaskStatusDTO;
import com.mooddownload.local.service.engine.convert.Aria2TaskStatusConverter;
import com.mooddownload.local.service.engine.model.EngineSyncSnapshot;
import com.mooddownload.local.service.engine.model.EngineTaskSnapshot;
import com.mooddownload.local.service.task.state.DownloadTaskStatus;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * aria2 同步服务单元测试。
 */
class Aria2SyncServiceTests {

    private final Aria2RpcClient aria2RpcClient = Mockito.mock(Aria2RpcClient.class);

    private final Aria2SyncService aria2SyncService = new Aria2SyncService(
        aria2RpcClient,
        new Aria2TaskStatusConverter(),
        buildProperties()
    );

    @Test
    void shouldPullCurrentSnapshotFromThreeQueues() {
        when(aria2RpcClient.tellActive()).thenReturn(Collections.singletonList(buildStatus("gid-a", "active")));
        when(aria2RpcClient.tellWaiting(0, 100)).thenReturn(Collections.singletonList(buildStatus("gid-w", "waiting")));
        when(aria2RpcClient.tellStopped(0, 100)).thenReturn(Collections.singletonList(buildStatus("gid-s", "complete")));

        EngineSyncSnapshot engineSyncSnapshot = aria2SyncService.pullCurrentSnapshot();

        assertThat(engineSyncSnapshot.getActiveTasks()).hasSize(1);
        assertThat(engineSyncSnapshot.getWaitingTasks()).hasSize(1);
        assertThat(engineSyncSnapshot.getStoppedTasks()).hasSize(1);
        assertThat(engineSyncSnapshot.getStoppedTasks().get(0).getEngineStatus()).isEqualTo("complete");
    }

    @Test
    void shouldMapAria2StatusToDomainStatus() {
        EngineTaskSnapshot engineTaskSnapshot = new EngineTaskSnapshot();
        engineTaskSnapshot.setEngineStatus("paused");

        assertThat(aria2SyncService.mapToDomainStatus(engineTaskSnapshot)).isEqualTo(DownloadTaskStatus.PAUSED);

        engineTaskSnapshot.setEngineStatus("error");
        assertThat(aria2SyncService.mapToDomainStatus(engineTaskSnapshot)).isEqualTo(DownloadTaskStatus.FAILED);
    }

    private Aria2Properties buildProperties() {
        Aria2Properties aria2Properties = new Aria2Properties();
        aria2Properties.setSyncBatchSize(100);
        return aria2Properties;
    }

    private Aria2TaskStatusDTO buildStatus(String gid, String status) {
        Aria2TaskStatusDTO aria2TaskStatusDTO = new Aria2TaskStatusDTO();
        aria2TaskStatusDTO.setGid(gid);
        aria2TaskStatusDTO.setStatus(status);
        aria2TaskStatusDTO.setTotalLength("1000");
        aria2TaskStatusDTO.setCompletedLength("500");
        aria2TaskStatusDTO.setDownloadSpeed("128");
        aria2TaskStatusDTO.setUploadSpeed("16");
        return aria2TaskStatusDTO;
    }
}
