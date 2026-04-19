package com.mooddownload.local.service.task;

import com.mooddownload.local.service.engine.Aria2SyncService;
import com.mooddownload.local.service.engine.model.EngineSyncSnapshot;
import com.mooddownload.local.service.engine.model.EngineTaskSnapshot;
import com.mooddownload.local.service.task.model.BtTaskAggregateSnapshot;
import com.mooddownload.local.service.task.model.DownloadEngineTaskModel;
import com.mooddownload.local.service.task.model.DownloadTaskModel;
import com.mooddownload.local.service.task.model.TaskOperationResult;
import com.mooddownload.local.service.task.model.TorrentFileItem;
import com.mooddownload.local.service.task.state.DownloadTaskStatus;
import com.mooddownload.local.service.task.state.TaskSourceType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 任务与 aria2 引擎快照对账服务。
 */
@Service
public class TaskSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskSyncService.class);

    private static final Comparator<EngineTaskSnapshot> ENGINE_SNAPSHOT_COMPARATOR = Comparator
        .comparing(TaskSyncService::isMetadataOnly)
        .thenComparing((EngineTaskSnapshot snapshot) -> safeLong(snapshot.getTotalSizeBytes()), Comparator.reverseOrder())
        .thenComparing(snapshot -> normalize(snapshot.getEngineGid()), Comparator.nullsLast(String::compareTo));

    private final Aria2SyncService aria2SyncService;

    private final TaskQueryService taskQueryService;

    private final TaskCommandService taskCommandService;

    private final TaskEventPublisher taskEventPublisher;

    private final TorrentFileListService torrentFileListService;

    public TaskSyncService(
        Aria2SyncService aria2SyncService,
        TaskQueryService taskQueryService,
        TaskCommandService taskCommandService,
        TaskEventPublisher taskEventPublisher,
        TorrentFileListService torrentFileListService
    ) {
        this.aria2SyncService = aria2SyncService;
        this.taskQueryService = taskQueryService;
        this.taskCommandService = taskCommandService;
        this.taskEventPublisher = taskEventPublisher;
        this.torrentFileListService = torrentFileListService;
    }

    /**
     * 拉取 aria2 快照并回写本地任务状态。
     */
    public void synchronizeTasks() {
        EngineSyncSnapshot engineSyncSnapshot = aria2SyncService.pullCurrentSnapshot();
        List<EngineTaskSnapshot> allSnapshots = flattenSnapshots(engineSyncSnapshot);
        SyncGraph syncGraph = buildSyncGraph(allSnapshots);
        Set<Long> processedTaskIds = new LinkedHashSet<>();
        Set<String> processedEngineGids = new LinkedHashSet<>();

        for (DownloadTaskModel downloadTaskModel : taskQueryService.listAllActiveTasks()) {
            if (!isBtTask(downloadTaskModel)) {
                continue;
            }
            List<EngineTaskSnapshot> groupedSnapshots = resolveBtGroupSnapshots(downloadTaskModel, syncGraph);
            if (groupedSnapshots.isEmpty()) {
                continue;
            }
            try {
                synchronizeBtTaskGroup(downloadTaskModel, groupedSnapshots);
                processedTaskIds.add(downloadTaskModel.getId());
                groupedSnapshots.stream()
                    .map(EngineTaskSnapshot::getEngineGid)
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .forEach(processedEngineGids::add);
            } catch (RuntimeException exception) {
                LOGGER.error("同步 BT 聚合任务失败: taskId={}, engineGids={}",
                    downloadTaskModel.getId(),
                    groupedSnapshots.stream().map(EngineTaskSnapshot::getEngineGid).collect(Collectors.toList()),
                    exception);
            }
        }

        for (EngineTaskSnapshot engineTaskSnapshot : allSnapshots) {
            String engineGid = normalize(engineTaskSnapshot.getEngineGid());
            if (!StringUtils.hasText(engineGid) || processedEngineGids.contains(engineGid)) {
                continue;
            }
            try {
                DownloadTaskModel relatedTask = taskQueryService.findTaskByEngineGid(engineGid);
                if (relatedTask != null && isBtTask(relatedTask)) {
                    if (processedTaskIds.contains(relatedTask.getId())) {
                        continue;
                    }
                    synchronizeBtTaskGroup(relatedTask, resolveBtGroupSnapshots(relatedTask, syncGraph));
                    processedTaskIds.add(relatedTask.getId());
                    continue;
                }
                synchronizeSingleTask(engineTaskSnapshot);
            } catch (RuntimeException exception) {
                LOGGER.error("同步单个 aria2 任务失败: engineGid={}, engineStatus={}",
                    engineTaskSnapshot.getEngineGid(),
                    engineTaskSnapshot.getEngineStatus(),
                    exception);
            }
        }
    }

    /**
     * 同步单个引擎任务，未知 gid 或已删除任务会被安全跳过。
     *
     * @param engineTaskSnapshot 引擎快照
     */
    public void synchronizeSingleTask(EngineTaskSnapshot engineTaskSnapshot) {
        if (engineTaskSnapshot == null || !StringUtils.hasText(engineTaskSnapshot.getEngineGid())) {
            LOGGER.warn("跳过无效的 aria2 同步快照: engineGid={}",
                engineTaskSnapshot == null ? null : engineTaskSnapshot.getEngineGid());
            return;
        }
        DownloadTaskModel downloadTaskModel = taskQueryService.findTaskByEngineGid(engineTaskSnapshot.getEngineGid().trim());
        if (downloadTaskModel == null) {
            if (isSupersededMetadataSnapshot(engineTaskSnapshot)) {
                LOGGER.info("跳过已被真实下载任务接管的 magnet metadata 快照: engineGid={}, followedBy={}",
                    engineTaskSnapshot.getEngineGid(), engineTaskSnapshot.getFollowedBy());
                return;
            }
            LOGGER.warn("aria2 快照未找到本地任务，已忽略: engineGid={}, engineStatus={}",
                engineTaskSnapshot.getEngineGid(), engineTaskSnapshot.getEngineStatus());
            return;
        }
        TaskOperationResult taskOperationResult = taskCommandService.syncTaskSnapshot(
            downloadTaskModel.getId(),
            engineTaskSnapshot,
            aria2SyncService.mapToDomainStatus(engineTaskSnapshot)
        );
        if (!taskOperationResult.isIdempotent() && taskOperationResult.getTaskModel() != null) {
            taskEventPublisher.publishTaskUpdated(taskOperationResult.getTaskModel());
        }
        refreshTorrentFilesIfNecessary(taskOperationResult.getTaskModel());
    }

    /**
     * 同步一个业务 BT 任务下的一组 aria2 子任务。
     *
     * @param downloadTaskModel 主任务
     * @param groupedSnapshots 当前同步批次内命中的子任务快照
     */
    private void synchronizeBtTaskGroup(
        DownloadTaskModel downloadTaskModel,
        List<EngineTaskSnapshot> groupedSnapshots
    ) {
        if (downloadTaskModel == null || groupedSnapshots == null || groupedSnapshots.isEmpty()) {
            return;
        }
        List<EngineTaskSnapshot> orderedSnapshots = groupedSnapshots.stream()
            .sorted(ENGINE_SNAPSHOT_COMPARATOR)
            .collect(Collectors.toList());
        List<DownloadEngineTaskModel> engineTaskModels = new ArrayList<>();
        Map<String, TorrentFileItem> mergedTorrentFileMap = new LinkedHashMap<>();
        List<EngineTaskSnapshot> realSnapshots = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (EngineTaskSnapshot engineTaskSnapshot : orderedSnapshots) {
            boolean metadataOnly = isMetadataOnly(engineTaskSnapshot);
            DownloadEngineTaskModel downloadEngineTaskModel = toEngineTaskModel(
                downloadTaskModel.getId(),
                engineTaskSnapshot,
                metadataOnly,
                now
            );
            if (!metadataOnly) {
                realSnapshots.add(engineTaskSnapshot);
                List<TorrentFileItem> torrentFiles = fetchTorrentFilesSafely(
                    engineTaskSnapshot.getEngineGid(),
                    isCompletedStatus(engineTaskSnapshot.getEngineStatus())
                );
                downloadEngineTaskModel.setTorrentFiles(torrentFiles);
                downloadEngineTaskModel.setTorrentFileListJson(null);
                mergeTorrentFiles(mergedTorrentFileMap, torrentFiles);
            } else {
                downloadEngineTaskModel.setTorrentFiles(Collections.emptyList());
                downloadEngineTaskModel.setTorrentFileListJson(null);
            }
            engineTaskModels.add(downloadEngineTaskModel);
        }

        List<EngineTaskSnapshot> aggregateSnapshots = realSnapshots.isEmpty() ? orderedSnapshots : realSnapshots;
        EngineTaskSnapshot primarySnapshot = choosePrimarySnapshot(realSnapshots, orderedSnapshots, downloadTaskModel);
        BtTaskAggregateSnapshot btTaskAggregateSnapshot = new BtTaskAggregateSnapshot();
        btTaskAggregateSnapshot.setPrimaryEngineGid(primarySnapshot == null ? downloadTaskModel.getEngineGid()
            : normalize(primarySnapshot.getEngineGid()));
        btTaskAggregateSnapshot.setDomainStatus(resolveAggregateStatus(aggregateSnapshots));
        btTaskAggregateSnapshot.setEngineStatus(primarySnapshot == null ? null : normalize(primarySnapshot.getEngineStatus()));
        btTaskAggregateSnapshot.setTotalSizeBytes(sumTotalSize(aggregateSnapshots));
        btTaskAggregateSnapshot.setCompletedSizeBytes(sumCompletedSize(aggregateSnapshots));
        btTaskAggregateSnapshot.setDownloadSpeedBps(sumDownloadSpeed(aggregateSnapshots));
        btTaskAggregateSnapshot.setUploadSpeedBps(sumUploadSpeed(aggregateSnapshots));
        btTaskAggregateSnapshot.setErrorCode(resolveAggregateErrorCode(primarySnapshot, aggregateSnapshots));
        btTaskAggregateSnapshot.setErrorMessage(resolveAggregateErrorMessage(primarySnapshot, aggregateSnapshots));
        btTaskAggregateSnapshot.setEngineTasks(engineTaskModels);
        btTaskAggregateSnapshot.setTorrentFiles(new ArrayList<>(mergedTorrentFileMap.values()));

        TaskOperationResult taskOperationResult = taskCommandService.syncBtTaskAggregate(
            downloadTaskModel.getId(),
            btTaskAggregateSnapshot
        );
        if (!taskOperationResult.isIdempotent() && taskOperationResult.getTaskModel() != null) {
            taskEventPublisher.publishTaskUpdated(taskOperationResult.getTaskModel());
        }
    }

    private List<EngineTaskSnapshot> resolveBtGroupSnapshots(DownloadTaskModel downloadTaskModel, SyncGraph syncGraph) {
        Set<String> knownGids = new LinkedHashSet<>();
        if (StringUtils.hasText(downloadTaskModel.getEngineGid())) {
            knownGids.add(downloadTaskModel.getEngineGid().trim());
        }
        if (downloadTaskModel.getEngineTasks() != null) {
            downloadTaskModel.getEngineTasks().stream()
                .map(DownloadEngineTaskModel::getEngineGid)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .forEach(knownGids::add);
        }
        if (knownGids.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> resolvedGids = new LinkedHashSet<>();
        ArrayDeque<String> pendingGids = new ArrayDeque<>();
        for (String knownGid : knownGids) {
            if (syncGraph.snapshotByGid.containsKey(knownGid)) {
                pendingGids.add(knownGid);
            }
        }
        for (EngineTaskSnapshot engineTaskSnapshot : syncGraph.snapshotByGid.values()) {
            if (knownGids.contains(normalize(engineTaskSnapshot.getBelongsTo()))
                || containsAny(engineTaskSnapshot.getFollowedBy(), knownGids)) {
                pendingGids.add(normalize(engineTaskSnapshot.getEngineGid()));
            }
        }
        while (!pendingGids.isEmpty()) {
            String currentGid = pendingGids.removeFirst();
            if (!StringUtils.hasText(currentGid) || !resolvedGids.add(currentGid)) {
                continue;
            }
            for (String adjacentGid : syncGraph.adjacentGids.getOrDefault(currentGid, Collections.emptySet())) {
                if (!resolvedGids.contains(adjacentGid)) {
                    pendingGids.add(adjacentGid);
                }
            }
        }
        return resolvedGids.stream()
            .map(syncGraph.snapshotByGid::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private SyncGraph buildSyncGraph(List<EngineTaskSnapshot> allSnapshots) {
        Map<String, EngineTaskSnapshot> snapshotByGid = new LinkedHashMap<>();
        Map<String, Set<String>> adjacentGids = new LinkedHashMap<>();
        for (EngineTaskSnapshot engineTaskSnapshot : allSnapshots) {
            String engineGid = normalize(engineTaskSnapshot.getEngineGid());
            if (!StringUtils.hasText(engineGid)) {
                continue;
            }
            snapshotByGid.put(engineGid, engineTaskSnapshot);
            adjacentGids.computeIfAbsent(engineGid, key -> new LinkedHashSet<>());
        }
        for (EngineTaskSnapshot engineTaskSnapshot : allSnapshots) {
            String engineGid = normalize(engineTaskSnapshot.getEngineGid());
            if (!StringUtils.hasText(engineGid)) {
                continue;
            }
            connectIfPresent(adjacentGids, snapshotByGid, engineGid, normalize(engineTaskSnapshot.getBelongsTo()));
            if (engineTaskSnapshot.getFollowedBy() != null) {
                for (String followedGid : engineTaskSnapshot.getFollowedBy()) {
                    connectIfPresent(adjacentGids, snapshotByGid, engineGid, normalize(followedGid));
                }
            }
        }
        return new SyncGraph(snapshotByGid, adjacentGids);
    }

    private void connectIfPresent(
        Map<String, Set<String>> adjacentGids,
        Map<String, EngineTaskSnapshot> snapshotByGid,
        String leftGid,
        String rightGid
    ) {
        if (!StringUtils.hasText(leftGid) || !StringUtils.hasText(rightGid)) {
            return;
        }
        if (!snapshotByGid.containsKey(leftGid) || !snapshotByGid.containsKey(rightGid)) {
            return;
        }
        adjacentGids.computeIfAbsent(leftGid, key -> new LinkedHashSet<>()).add(rightGid);
        adjacentGids.computeIfAbsent(rightGid, key -> new LinkedHashSet<>()).add(leftGid);
    }

    private DownloadEngineTaskModel toEngineTaskModel(
        Long taskId,
        EngineTaskSnapshot engineTaskSnapshot,
        boolean metadataOnly,
        long now
    ) {
        DownloadEngineTaskModel downloadEngineTaskModel = new DownloadEngineTaskModel();
        downloadEngineTaskModel.setTaskId(taskId);
        downloadEngineTaskModel.setEngineGid(normalize(engineTaskSnapshot.getEngineGid()));
        downloadEngineTaskModel.setParentEngineGid(normalize(engineTaskSnapshot.getBelongsTo()));
        downloadEngineTaskModel.setEngineStatus(normalize(engineTaskSnapshot.getEngineStatus()));
        downloadEngineTaskModel.setMetadataOnly(metadataOnly);
        downloadEngineTaskModel.setTotalSizeBytes(safeLong(engineTaskSnapshot.getTotalSizeBytes()));
        downloadEngineTaskModel.setCompletedSizeBytes(safeLong(engineTaskSnapshot.getCompletedSizeBytes()));
        downloadEngineTaskModel.setDownloadSpeedBps(safeLong(engineTaskSnapshot.getDownloadSpeedBps()));
        downloadEngineTaskModel.setUploadSpeedBps(safeLong(engineTaskSnapshot.getUploadSpeedBps()));
        downloadEngineTaskModel.setErrorCode(normalize(engineTaskSnapshot.getErrorCode()));
        downloadEngineTaskModel.setErrorMessage(normalize(engineTaskSnapshot.getErrorMessage()));
        downloadEngineTaskModel.setCreatedAt(now);
        downloadEngineTaskModel.setUpdatedAt(now);
        return downloadEngineTaskModel;
    }

    private List<TorrentFileItem> fetchTorrentFilesSafely(String engineGid, boolean preferActualFileSize) {
        if (!StringUtils.hasText(engineGid)) {
            return Collections.emptyList();
        }
        try {
            return torrentFileListService.fetchTorrentFiles(engineGid.trim(), preferActualFileSize);
        } catch (RuntimeException exception) {
            LOGGER.warn("同步阶段读取 BT 子任务文件列表失败: engineGid={}", engineGid, exception);
            return Collections.emptyList();
        }
    }

    private void mergeTorrentFiles(Map<String, TorrentFileItem> mergedTorrentFileMap, List<TorrentFileItem> torrentFiles) {
        if (torrentFiles == null || torrentFiles.isEmpty()) {
            return;
        }
        for (TorrentFileItem torrentFileItem : torrentFiles) {
            String mergeKey = resolveTorrentFileKey(torrentFileItem);
            TorrentFileItem existingFile = mergedTorrentFileMap.get(mergeKey);
            if (existingFile == null) {
                mergedTorrentFileMap.put(mergeKey, copyTorrentFile(torrentFileItem));
                continue;
            }
            existingFile.setFileIndex(resolveMergedFileIndex(existingFile.getFileIndex(), torrentFileItem.getFileIndex()));
            existingFile.setFileSizeBytes(Math.max(
                safeLong(existingFile.getFileSizeBytes()),
                safeLong(torrentFileItem.getFileSizeBytes())
            ));
            existingFile.setSelected(resolveMergedSelected(existingFile.getSelected(), torrentFileItem.getSelected()));
        }
    }

    private EngineTaskSnapshot choosePrimarySnapshot(
        List<EngineTaskSnapshot> realSnapshots,
        List<EngineTaskSnapshot> orderedSnapshots,
        DownloadTaskModel downloadTaskModel
    ) {
        List<EngineTaskSnapshot> preferredSnapshots = realSnapshots.isEmpty() ? orderedSnapshots : realSnapshots;
        if (StringUtils.hasText(downloadTaskModel.getEngineGid())) {
            String currentEngineGid = downloadTaskModel.getEngineGid().trim();
            for (EngineTaskSnapshot preferredSnapshot : preferredSnapshots) {
                if (currentEngineGid.equals(normalize(preferredSnapshot.getEngineGid()))) {
                    return preferredSnapshot;
                }
            }
        }
        if (preferredSnapshots.isEmpty()) {
            return null;
        }
        return preferredSnapshots.get(0);
    }

    private DownloadTaskStatus resolveAggregateStatus(List<EngineTaskSnapshot> aggregateSnapshots) {
        if (aggregateSnapshots == null || aggregateSnapshots.isEmpty()) {
            return DownloadTaskStatus.RECONCILING;
        }
        boolean hasRunning = false;
        boolean hasFailed = false;
        boolean hasPaused = false;
        boolean hasPending = false;
        boolean hasCancelled = false;
        boolean allCompleted = true;
        for (EngineTaskSnapshot aggregateSnapshot : aggregateSnapshots) {
            DownloadTaskStatus downloadTaskStatus = aria2SyncService.mapToDomainStatus(aggregateSnapshot);
            if (downloadTaskStatus != DownloadTaskStatus.COMPLETED) {
                allCompleted = false;
            }
            if (downloadTaskStatus == DownloadTaskStatus.RUNNING) {
                hasRunning = true;
            } else if (downloadTaskStatus == DownloadTaskStatus.FAILED) {
                hasFailed = true;
            } else if (downloadTaskStatus == DownloadTaskStatus.PAUSED) {
                hasPaused = true;
            } else if (downloadTaskStatus == DownloadTaskStatus.PENDING) {
                hasPending = true;
            } else if (downloadTaskStatus == DownloadTaskStatus.CANCELLED) {
                hasCancelled = true;
            }
        }
        if (allCompleted) {
            return DownloadTaskStatus.COMPLETED;
        }
        if (hasRunning) {
            return DownloadTaskStatus.RUNNING;
        }
        if (hasFailed) {
            return DownloadTaskStatus.FAILED;
        }
        if (hasPaused) {
            return DownloadTaskStatus.PAUSED;
        }
        if (hasPending) {
            return DownloadTaskStatus.PENDING;
        }
        if (hasCancelled) {
            return DownloadTaskStatus.CANCELLED;
        }
        return DownloadTaskStatus.RECONCILING;
    }

    private Long sumTotalSize(List<EngineTaskSnapshot> engineTaskSnapshots) {
        long total = 0L;
        for (EngineTaskSnapshot engineTaskSnapshot : engineTaskSnapshots) {
            total += safeLong(engineTaskSnapshot.getTotalSizeBytes());
        }
        return total;
    }

    private Long sumCompletedSize(List<EngineTaskSnapshot> engineTaskSnapshots) {
        long total = 0L;
        for (EngineTaskSnapshot engineTaskSnapshot : engineTaskSnapshots) {
            total += safeLong(engineTaskSnapshot.getCompletedSizeBytes());
        }
        return total;
    }

    private Long sumDownloadSpeed(List<EngineTaskSnapshot> engineTaskSnapshots) {
        long total = 0L;
        for (EngineTaskSnapshot engineTaskSnapshot : engineTaskSnapshots) {
            total += safeLong(engineTaskSnapshot.getDownloadSpeedBps());
        }
        return total;
    }

    private Long sumUploadSpeed(List<EngineTaskSnapshot> engineTaskSnapshots) {
        long total = 0L;
        for (EngineTaskSnapshot engineTaskSnapshot : engineTaskSnapshots) {
            total += safeLong(engineTaskSnapshot.getUploadSpeedBps());
        }
        return total;
    }

    private String resolveAggregateErrorCode(
        EngineTaskSnapshot primarySnapshot,
        List<EngineTaskSnapshot> aggregateSnapshots
    ) {
        if (primarySnapshot != null && StringUtils.hasText(primarySnapshot.getErrorCode())) {
            return primarySnapshot.getErrorCode().trim();
        }
        for (EngineTaskSnapshot aggregateSnapshot : aggregateSnapshots) {
            if (StringUtils.hasText(aggregateSnapshot.getErrorCode())) {
                return aggregateSnapshot.getErrorCode().trim();
            }
        }
        return null;
    }

    private String resolveAggregateErrorMessage(
        EngineTaskSnapshot primarySnapshot,
        List<EngineTaskSnapshot> aggregateSnapshots
    ) {
        if (primarySnapshot != null && StringUtils.hasText(primarySnapshot.getErrorMessage())) {
            return primarySnapshot.getErrorMessage().trim();
        }
        for (EngineTaskSnapshot aggregateSnapshot : aggregateSnapshots) {
            if (StringUtils.hasText(aggregateSnapshot.getErrorMessage())) {
                return aggregateSnapshot.getErrorMessage().trim();
            }
        }
        return null;
    }

    /**
     * 刷新 BT 文件列表，确保详情页拿到 metadata 解析后的真实文件大小。
     *
     * @param downloadTaskModel 当前任务
     */
    private void refreshTorrentFilesIfNecessary(DownloadTaskModel downloadTaskModel) {
        if (!isBtTask(downloadTaskModel) || !StringUtils.hasText(downloadTaskModel.getEngineGid())) {
            return;
        }
        try {
            TaskOperationResult metadataResult = taskCommandService.updateTorrentFiles(
                downloadTaskModel.getId(),
                torrentFileListService.fetchTorrentFiles(
                    downloadTaskModel.getEngineGid(),
                    DownloadTaskStatus.COMPLETED.name().equals(downloadTaskModel.getDomainStatus())
                )
            );
            if (!metadataResult.isIdempotent() && metadataResult.getTaskModel() != null) {
                taskEventPublisher.publishTaskUpdated(metadataResult.getTaskModel());
            }
        } catch (RuntimeException exception) {
            LOGGER.warn("同步阶段刷新 BT 文件列表失败: taskId={}, engineGid={}",
                downloadTaskModel.getId(), downloadTaskModel.getEngineGid(), exception);
        }
    }

    private boolean isBtTask(DownloadTaskModel downloadTaskModel) {
        TaskSourceType taskSourceType = TaskSourceType.fromCode(downloadTaskModel.getSourceType());
        return taskSourceType == TaskSourceType.TORRENT
            || taskSourceType == TaskSourceType.MAGNET
            || taskSourceType == TaskSourceType.BT;
    }

    private boolean containsAny(Collection<String> values, Set<String> candidates) {
        if (values == null || values.isEmpty() || candidates == null || candidates.isEmpty()) {
            return false;
        }
        for (String value : values) {
            if (candidates.contains(normalize(value))) {
                return true;
            }
        }
        return false;
    }

    private boolean isSupersededMetadataSnapshot(EngineTaskSnapshot engineTaskSnapshot) {
        return isCompletedStatus(engineTaskSnapshot.getEngineStatus())
            && safeLong(engineTaskSnapshot.getTotalSizeBytes()) <= 0L
            && hasFollowedTask(engineTaskSnapshot);
    }

    private boolean hasFollowedTask(EngineTaskSnapshot engineTaskSnapshot) {
        if (engineTaskSnapshot.getFollowedBy() == null || engineTaskSnapshot.getFollowedBy().isEmpty()) {
            return false;
        }
        for (String followedEngineGid : engineTaskSnapshot.getFollowedBy()) {
            if (StringUtils.hasText(followedEngineGid)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMetadataOnly(EngineTaskSnapshot engineTaskSnapshot) {
        if (engineTaskSnapshot == null) {
            return false;
        }
        boolean zeroSized = safeLong(engineTaskSnapshot.getTotalSizeBytes()) <= 0L
            && safeLong(engineTaskSnapshot.getCompletedSizeBytes()) <= 0L;
        if (!zeroSized) {
            return false;
        }
        if (engineTaskSnapshot.getFollowedBy() != null && !engineTaskSnapshot.getFollowedBy().isEmpty()) {
            return true;
        }
        return isCompletedStatus(engineTaskSnapshot.getEngineStatus())
            && !StringUtils.hasText(engineTaskSnapshot.getBelongsTo());
    }

    private static boolean isCompletedStatus(String engineStatus) {
        return "complete".equalsIgnoreCase(engineStatus);
    }

    private static Long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String resolveTorrentFileKey(TorrentFileItem torrentFileItem) {
        if (torrentFileItem == null) {
            return "null";
        }
        if (StringUtils.hasText(torrentFileItem.getFilePath())) {
            return torrentFileItem.getFilePath().trim().toLowerCase(Locale.ROOT);
        }
        if (torrentFileItem.getFileIndex() != null) {
            return "index:" + torrentFileItem.getFileIndex();
        }
        return "unknown";
    }

    private TorrentFileItem copyTorrentFile(TorrentFileItem sourceTorrentFile) {
        TorrentFileItem targetTorrentFile = new TorrentFileItem();
        targetTorrentFile.setFileIndex(sourceTorrentFile.getFileIndex());
        targetTorrentFile.setFilePath(sourceTorrentFile.getFilePath());
        targetTorrentFile.setFileSizeBytes(sourceTorrentFile.getFileSizeBytes());
        targetTorrentFile.setSelected(sourceTorrentFile.getSelected());
        return targetTorrentFile;
    }

    private Integer resolveMergedFileIndex(Integer leftFileIndex, Integer rightFileIndex) {
        if (leftFileIndex == null) {
            return rightFileIndex;
        }
        if (rightFileIndex == null) {
            return leftFileIndex;
        }
        return Math.min(leftFileIndex, rightFileIndex);
    }

    private Boolean resolveMergedSelected(Boolean leftSelected, Boolean rightSelected) {
        if (Boolean.TRUE.equals(leftSelected) || Boolean.TRUE.equals(rightSelected)) {
            return Boolean.TRUE;
        }
        if (Boolean.FALSE.equals(leftSelected) && Boolean.FALSE.equals(rightSelected)) {
            return Boolean.FALSE;
        }
        return leftSelected != null ? leftSelected : rightSelected;
    }

    private List<EngineTaskSnapshot> flattenSnapshots(EngineSyncSnapshot engineSyncSnapshot) {
        List<EngineTaskSnapshot> allSnapshots = new ArrayList<>();
        if (engineSyncSnapshot == null) {
            return allSnapshots;
        }
        addAll(allSnapshots, engineSyncSnapshot.getActiveTasks());
        addAll(allSnapshots, engineSyncSnapshot.getWaitingTasks());
        addAll(allSnapshots, engineSyncSnapshot.getStoppedTasks());
        return allSnapshots;
    }

    private void addAll(List<EngineTaskSnapshot> allSnapshots, List<EngineTaskSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }
        allSnapshots.addAll(snapshots);
    }

    private static final class SyncGraph {

        private final Map<String, EngineTaskSnapshot> snapshotByGid;

        private final Map<String, Set<String>> adjacentGids;

        private SyncGraph(Map<String, EngineTaskSnapshot> snapshotByGid, Map<String, Set<String>> adjacentGids) {
            this.snapshotByGid = snapshotByGid;
            this.adjacentGids = adjacentGids;
        }
    }
}
