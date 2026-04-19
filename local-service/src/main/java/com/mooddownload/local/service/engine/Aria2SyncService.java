package com.mooddownload.local.service.engine;

import com.mooddownload.local.client.aria2.Aria2Properties;
import com.mooddownload.local.client.aria2.Aria2RpcClient;
import com.mooddownload.local.client.aria2.dto.Aria2TaskStatusDTO;
import com.mooddownload.local.service.engine.convert.Aria2TaskStatusConverter;
import com.mooddownload.local.service.engine.model.EngineSyncSnapshot;
import com.mooddownload.local.service.engine.model.EngineTaskSnapshot;
import com.mooddownload.local.service.task.state.DownloadTaskStatus;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * aria2 同步服务，负责轮询引擎快照和状态映射。
 */
@Service
public class Aria2SyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(Aria2SyncService.class);

    private final Aria2RpcClient aria2RpcClient;

    private final Aria2TaskStatusConverter aria2TaskStatusConverter;

    private final Aria2Properties aria2Properties;

    public Aria2SyncService(
        Aria2RpcClient aria2RpcClient,
        Aria2TaskStatusConverter aria2TaskStatusConverter,
        Aria2Properties aria2Properties
    ) {
        this.aria2RpcClient = aria2RpcClient;
        this.aria2TaskStatusConverter = aria2TaskStatusConverter;
        this.aria2Properties = aria2Properties;
    }

    /**
     * 拉取 aria2 当前活跃、等待、停止任务快照。
     *
     * @return 同步批次快照
     */
    public EngineSyncSnapshot pullCurrentSnapshot() {
        List<EngineTaskSnapshot> activeTasks = convertAll(aria2RpcClient.tellActive());
        List<EngineTaskSnapshot> waitingTasks = convertAll(
            aria2RpcClient.tellWaiting(0, aria2Properties.getSyncBatchSize())
        );
        List<EngineTaskSnapshot> stoppedTasks = convertAll(
            aria2RpcClient.tellStopped(0, aria2Properties.getSyncBatchSize())
        );
        LOGGER.debug("拉取 aria2 同步快照成功: active={}, waiting={}, stopped={}",
            activeTasks.size(), waitingTasks.size(), stoppedTasks.size());
        return new EngineSyncSnapshot(activeTasks, waitingTasks, stoppedTasks);
    }

    /**
     * 将引擎快照状态映射为领域状态。
     *
     * @param engineTaskSnapshot 引擎快照
     * @return 领域状态
     */
    public DownloadTaskStatus mapToDomainStatus(EngineTaskSnapshot engineTaskSnapshot) {
        return aria2TaskStatusConverter.toDomainStatus(engineTaskSnapshot.getEngineStatus());
    }

    private List<EngineTaskSnapshot> convertAll(List<Aria2TaskStatusDTO> taskStatusDTOList) {
        List<EngineTaskSnapshot> engineTaskSnapshotList = new ArrayList<>();
        if (taskStatusDTOList == null || taskStatusDTOList.isEmpty()) {
            return engineTaskSnapshotList;
        }
        for (Aria2TaskStatusDTO taskStatusDTO : taskStatusDTOList) {
            engineTaskSnapshotList.add(aria2TaskStatusConverter.toSnapshot(taskStatusDTO));
        }
        return engineTaskSnapshotList;
    }
}
