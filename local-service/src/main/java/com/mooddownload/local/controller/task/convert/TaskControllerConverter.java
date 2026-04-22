package com.mooddownload.local.controller.task.convert;

import com.mooddownload.local.controller.task.vo.TaskCreateRequest;
import com.mooddownload.local.controller.task.vo.TaskCreateResponse;
import com.mooddownload.local.controller.task.vo.TaskDeleteResponse;
import com.mooddownload.local.controller.task.vo.TaskDeletionPreviewResponse;
import com.mooddownload.local.controller.task.vo.TaskDetailResponse;
import com.mooddownload.local.controller.task.vo.TaskEngineDetailVO;
import com.mooddownload.local.controller.task.vo.TaskListItemVO;
import com.mooddownload.local.controller.task.vo.TaskListResponse;
import com.mooddownload.local.controller.task.vo.TaskOpenContextResponse;
import com.mooddownload.local.controller.task.vo.TaskOperationResponse;
import com.mooddownload.local.controller.task.vo.TaskTorrentFileVO;
import com.mooddownload.local.service.task.BtSourceHashResolver;
import com.mooddownload.local.service.task.model.CreateTaskCommand;
import com.mooddownload.local.service.task.model.DownloadEngineTaskModel;
import com.mooddownload.local.service.task.model.DownloadTaskModel;
import com.mooddownload.local.service.task.model.TaskDeletionPreview;
import com.mooddownload.local.service.task.model.TaskOpenContextModel;
import com.mooddownload.local.service.task.model.TaskOperationResult;
import com.mooddownload.local.service.task.model.TaskPageResult;
import com.mooddownload.local.service.task.model.TorrentFileItem;
import com.mooddownload.local.service.task.state.TaskSourceType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 任务接口模型转换器。
 */
@Component
public class TaskControllerConverter {

    private final BtSourceHashResolver btSourceHashResolver;

    public TaskControllerConverter(BtSourceHashResolver btSourceHashResolver) {
        this.btSourceHashResolver = btSourceHashResolver;
    }

    /**
     * 将创建请求转换为创建命令。
     *
     * @param request 创建请求
     * @param resolvedSaveDir 实际保存目录
     * @param clientType 调用方类型
     * @return 创建命令
     */
    public CreateTaskCommand toCreateCommand(
        TaskCreateRequest request,
        String resolvedSaveDir,
        String clientType
    ) {
        CreateTaskCommand createTaskCommand = new CreateTaskCommand();
        createTaskCommand.setClientRequestId(request.getClientRequestId());
        createTaskCommand.setSourceType(request.getSourceType());
        createTaskCommand.setSourceUri(request.getSourceUri());
        createTaskCommand.setSourceHash(
            btSourceHashResolver.resolve(request.getSourceType(), request.getSourceUri(), null)
        );
        createTaskCommand.setSaveDir(resolvedSaveDir);
        createTaskCommand.setDisplayName(request.getDisplayName());
        createTaskCommand.setClientType(clientType);
        return createTaskCommand;
    }

    /**
     * 组装种子任务创建命令。
     *
     * @param clientRequestId 幂等键
     * @param torrentFilePath 种子文件路径
     * @param displayName 展示名称
     * @param resolvedSaveDir 实际保存目录
     * @param clientType 调用方类型
     * @return 创建命令
     */
    public CreateTaskCommand toTorrentCreateCommand(
        String clientRequestId,
        String torrentFilePath,
        String displayName,
        String resolvedSaveDir,
        String clientType
    ) {
        CreateTaskCommand createTaskCommand = new CreateTaskCommand();
        createTaskCommand.setClientRequestId(clientRequestId);
        createTaskCommand.setSourceType("TORRENT");
        createTaskCommand.setTorrentFilePath(torrentFilePath);
        createTaskCommand.setSourceHash(btSourceHashResolver.resolve("TORRENT", null, torrentFilePath));
        createTaskCommand.setDisplayName(displayName);
        createTaskCommand.setSaveDir(resolvedSaveDir);
        createTaskCommand.setClientType(clientType);
        return createTaskCommand;
    }

    /**
     * 将任务模型转换为创建响应。
     *
     * @param downloadTaskModel 任务模型
     * @return 创建响应
     */
    public TaskCreateResponse toCreateResponse(DownloadTaskModel downloadTaskModel) {
        TaskCreateResponse taskCreateResponse = new TaskCreateResponse();
        taskCreateResponse.setTaskId(downloadTaskModel.getId());
        taskCreateResponse.setTaskCode(downloadTaskModel.getTaskCode());
        taskCreateResponse.setDomainStatus(downloadTaskModel.getDomainStatus());
        taskCreateResponse.setEngineStatus(downloadTaskModel.getEngineStatus());
        taskCreateResponse.setDisplayName(downloadTaskModel.getDisplayName());
        taskCreateResponse.setCreatedAt(downloadTaskModel.getCreatedAt());
        return taskCreateResponse;
    }

    /**
     * 将分页结果转换为接口响应。
     *
     * @param taskPageResult 分页结果
     * @return 接口响应
     */
    public TaskListResponse toListResponse(TaskPageResult taskPageResult) {
        TaskListResponse taskListResponse = new TaskListResponse();
        taskListResponse.setPageNo(taskPageResult.getPageNo());
        taskListResponse.setPageSize(taskPageResult.getPageSize());
        taskListResponse.setTotal(taskPageResult.getTotal());
        List<TaskListItemVO> items = taskPageResult.getItems().stream()
            .map(this::toListItemVO)
            .collect(Collectors.toList());
        taskListResponse.setItems(items);
        return taskListResponse;
    }

    /**
     * 将任务模型转换为详情响应。
     *
     * @param downloadTaskModel 任务模型
     * @return 详情响应
     */
    public TaskDetailResponse toDetailResponse(DownloadTaskModel downloadTaskModel) {
        TaskDetailResponse taskDetailResponse = new TaskDetailResponse();
        taskDetailResponse.setTaskId(downloadTaskModel.getId());
        taskDetailResponse.setTaskCode(downloadTaskModel.getTaskCode());
        taskDetailResponse.setDisplayName(downloadTaskModel.getDisplayName());
        taskDetailResponse.setSourceType(downloadTaskModel.getSourceType());
        taskDetailResponse.setSourceUri(downloadTaskModel.getSourceUri());
        taskDetailResponse.setDomainStatus(downloadTaskModel.getDomainStatus());
        taskDetailResponse.setEngineStatus(downloadTaskModel.getEngineStatus());
        taskDetailResponse.setProgress(calculateProgress(downloadTaskModel));
        taskDetailResponse.setTotalSizeBytes(downloadTaskModel.getTotalSizeBytes());
        taskDetailResponse.setCompletedSizeBytes(downloadTaskModel.getCompletedSizeBytes());
        taskDetailResponse.setDownloadSpeedBps(downloadTaskModel.getDownloadSpeedBps());
        taskDetailResponse.setUploadSpeedBps(downloadTaskModel.getUploadSpeedBps());
        taskDetailResponse.setSaveDir(downloadTaskModel.getSaveDir());
        taskDetailResponse.setRetryCount(downloadTaskModel.getRetryCount());
        taskDetailResponse.setErrorCode(downloadTaskModel.getErrorCode());
        taskDetailResponse.setErrorMessage(downloadTaskModel.getErrorMessage());
        taskDetailResponse.setTorrentFiles(downloadTaskModel.getTorrentFiles().stream()
            .map(this::toTorrentFileVO)
            .collect(Collectors.toList()));
        taskDetailResponse.setEngineTasks(downloadTaskModel.getEngineTasks().stream()
            .map(this::toEngineTaskVO)
            .collect(Collectors.toList()));
        taskDetailResponse.setTorrentMetadataReady(isTorrentMetadataReady(downloadTaskModel));
        taskDetailResponse.setCreatedAt(downloadTaskModel.getCreatedAt());
        taskDetailResponse.setUpdatedAt(downloadTaskModel.getUpdatedAt());
        return taskDetailResponse;
    }

    /**
     * 将任务操作结果转换为统一操作响应。
     *
     * @param taskOperationResult 任务操作结果
     * @return 操作响应
     */
    public TaskOperationResponse toOperationResponse(TaskOperationResult taskOperationResult) {
        TaskOperationResponse taskOperationResponse = new TaskOperationResponse();
        taskOperationResponse.setTaskId(taskOperationResult.getTaskModel().getId());
        taskOperationResponse.setDomainStatus(taskOperationResult.getTaskModel().getDomainStatus());
        taskOperationResponse.setRetryCount(taskOperationResult.getTaskModel().getRetryCount());
        taskOperationResponse.setOperationApplied(!taskOperationResult.isIdempotent());
        return taskOperationResponse;
    }

    /**
     * 将任务操作结果转换为删除响应。
     *
     * @param executionResult 删除执行结果
     * @return 删除响应
     */
    public TaskDeleteResponse toDeleteResponse(com.mooddownload.local.service.task.TaskDeleteExecutionResult executionResult) {
        TaskDeleteResponse taskDeleteResponse = new TaskDeleteResponse();
        taskDeleteResponse.setTaskId(executionResult.getTaskOperationResult().getTaskModel().getId());
        taskDeleteResponse.setRemoved(Boolean.TRUE);
        taskDeleteResponse.setDeleteMode(executionResult.getDeleteMode().name());
        taskDeleteResponse.setOutputRemoved(executionResult.isOutputRemoved());
        taskDeleteResponse.setArtifactRemoved(executionResult.isArtifactRemoved());
        taskDeleteResponse.setPartialSuccess(executionResult.isPartialSuccess());
        taskDeleteResponse.setMessage(executionResult.isPartialSuccess()
            ? "任务已删除，但部分文件清理失败"
            : "任务删除成功");
        return taskDeleteResponse;
    }

    /**
     * 将删除预览模型转换为接口响应。
     *
     * @param preview 删除预览模型
     * @return 接口响应
     */
    public TaskDeletionPreviewResponse toDeletionPreviewResponse(TaskDeletionPreview preview) {
        TaskDeletionPreviewResponse response = new TaskDeletionPreviewResponse();
        response.setTaskId(preview.getTaskId());
        response.setDeleteMode(preview.getDeleteMode() == null ? null : preview.getDeleteMode().name());
        response.setTargets(preview.getTargets());
        response.setWarnings(preview.getWarnings());
        response.setRemovable(preview.getRemovable());
        return response;
    }

    /**
     * 将打开上下文模型转换为接口响应。
     *
     * @param contextModel 打开上下文模型
     * @return 接口响应
     */
    public TaskOpenContextResponse toOpenContextResponse(TaskOpenContextModel contextModel) {
        TaskOpenContextResponse response = new TaskOpenContextResponse();
        response.setTaskId(contextModel.getTaskId());
        response.setOpenFolderPath(contextModel.getOpenFolderPath());
        response.setPrimaryFilePath(contextModel.getPrimaryFilePath());
        response.setCanOpen(contextModel.getCanOpen());
        response.setReason(contextModel.getReason());
        return response;
    }

    private TaskListItemVO toListItemVO(DownloadTaskModel downloadTaskModel) {
        TaskListItemVO taskListItemVO = new TaskListItemVO();
        taskListItemVO.setTaskId(downloadTaskModel.getId());
        taskListItemVO.setTaskCode(downloadTaskModel.getTaskCode());
        taskListItemVO.setDisplayName(downloadTaskModel.getDisplayName());
        taskListItemVO.setSourceType(downloadTaskModel.getSourceType());
        taskListItemVO.setDomainStatus(downloadTaskModel.getDomainStatus());
        taskListItemVO.setEngineStatus(downloadTaskModel.getEngineStatus());
        taskListItemVO.setProgress(calculateProgress(downloadTaskModel));
        taskListItemVO.setDownloadSpeedBps(downloadTaskModel.getDownloadSpeedBps());
        taskListItemVO.setSaveDir(downloadTaskModel.getSaveDir());
        taskListItemVO.setUpdatedAt(downloadTaskModel.getUpdatedAt());
        return taskListItemVO;
    }

    private Double calculateProgress(DownloadTaskModel downloadTaskModel) {
        long totalSizeBytes = downloadTaskModel.getTotalSizeBytes() == null ? 0L : downloadTaskModel.getTotalSizeBytes();
        long completedSizeBytes = downloadTaskModel.getCompletedSizeBytes() == null
            ? 0L
            : downloadTaskModel.getCompletedSizeBytes();
        if (totalSizeBytes <= 0L) {
            return 0D;
        }
        return BigDecimal.valueOf(completedSizeBytes)
            .divide(BigDecimal.valueOf(totalSizeBytes), 4, RoundingMode.HALF_UP)
            .doubleValue();
    }

    private TaskTorrentFileVO toTorrentFileVO(TorrentFileItem torrentFileItem) {
        TaskTorrentFileVO taskTorrentFileVO = new TaskTorrentFileVO();
        taskTorrentFileVO.setFileIndex(torrentFileItem.getFileIndex());
        taskTorrentFileVO.setFilePath(torrentFileItem.getFilePath());
        taskTorrentFileVO.setFileSizeBytes(torrentFileItem.getFileSizeBytes());
        taskTorrentFileVO.setSelected(torrentFileItem.getSelected());
        return taskTorrentFileVO;
    }

    private TaskEngineDetailVO toEngineTaskVO(DownloadEngineTaskModel downloadEngineTaskModel) {
        TaskEngineDetailVO taskEngineDetailVO = new TaskEngineDetailVO();
        taskEngineDetailVO.setEngineGid(downloadEngineTaskModel.getEngineGid());
        taskEngineDetailVO.setParentEngineGid(downloadEngineTaskModel.getParentEngineGid());
        taskEngineDetailVO.setEngineStatus(downloadEngineTaskModel.getEngineStatus());
        taskEngineDetailVO.setMetadataOnly(downloadEngineTaskModel.getMetadataOnly());
        taskEngineDetailVO.setTotalSizeBytes(downloadEngineTaskModel.getTotalSizeBytes());
        taskEngineDetailVO.setCompletedSizeBytes(downloadEngineTaskModel.getCompletedSizeBytes());
        taskEngineDetailVO.setDownloadSpeedBps(downloadEngineTaskModel.getDownloadSpeedBps());
        taskEngineDetailVO.setUploadSpeedBps(downloadEngineTaskModel.getUploadSpeedBps());
        taskEngineDetailVO.setErrorCode(downloadEngineTaskModel.getErrorCode());
        taskEngineDetailVO.setErrorMessage(downloadEngineTaskModel.getErrorMessage());
        return taskEngineDetailVO;
    }

    private boolean isTorrentMetadataReady(DownloadTaskModel downloadTaskModel) {
        TaskSourceType taskSourceType = TaskSourceType.fromCode(downloadTaskModel.getSourceType());
        if (taskSourceType != TaskSourceType.TORRENT
            && taskSourceType != TaskSourceType.MAGNET
            && taskSourceType != TaskSourceType.BT) {
            return true;
        }
        if (downloadTaskModel.getTotalSizeBytes() != null && downloadTaskModel.getTotalSizeBytes() > 0L) {
            return true;
        }
        return downloadTaskModel.getTorrentFiles().stream()
            .anyMatch(torrentFileItem -> torrentFileItem.getFileSizeBytes() != null
                && torrentFileItem.getFileSizeBytes() > 0L);
    }
}
