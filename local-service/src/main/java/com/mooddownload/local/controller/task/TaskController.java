package com.mooddownload.local.controller.task;

import com.mooddownload.local.common.enums.ErrorCode;
import com.mooddownload.local.common.exception.BizException;
import com.mooddownload.local.common.response.ApiResponse;
import com.mooddownload.local.common.security.RequestContext;
import com.mooddownload.local.controller.task.convert.TaskControllerConverter;
import com.mooddownload.local.controller.task.vo.TaskCreateRequest;
import com.mooddownload.local.controller.task.vo.TaskCreateResponse;
import com.mooddownload.local.controller.task.vo.TaskDeleteResponse;
import com.mooddownload.local.controller.task.vo.TaskDetailResponse;
import com.mooddownload.local.controller.task.vo.TaskListResponse;
import com.mooddownload.local.controller.task.vo.TaskOperationResponse;
import com.mooddownload.local.service.config.ConfigService;
import com.mooddownload.local.service.task.TaskDeleteExecutionResult;
import com.mooddownload.local.service.task.TaskQueryService;
import com.mooddownload.local.service.task.TaskWorkflowService;
import com.mooddownload.local.service.task.model.TaskOperationResult;
import com.mooddownload.local.service.task.model.TaskPageQuery;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 任务接口控制器。
 */
@Validated
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskController.class);

    private final TaskQueryService taskQueryService;

    private final ConfigService configService;

    private final TaskWorkflowService taskWorkflowService;

    private final TaskControllerConverter taskControllerConverter;

    public TaskController(
        TaskQueryService taskQueryService,
        ConfigService configService,
        TaskWorkflowService taskWorkflowService,
        TaskControllerConverter taskControllerConverter
    ) {
        this.taskQueryService = taskQueryService;
        this.configService = configService;
        this.taskWorkflowService = taskWorkflowService;
        this.taskControllerConverter = taskControllerConverter;
    }

    /**
     * 创建 URL / 磁力任务。
     *
     * @param request 创建请求
     * @return 统一响应
     */
    @PostMapping
    public ApiResponse<TaskCreateResponse> createTask(@Valid @RequestBody TaskCreateRequest request) {
        TaskOperationResult taskOperationResult = taskWorkflowService.createTask(
            taskControllerConverter.toCreateCommand(
                request,
                configService.resolveSaveDir(request.getSaveDir()),
                RequestContext.getClientType()
            )
        );
        return ApiResponse.success(taskControllerConverter.toCreateResponse(taskOperationResult.getTaskModel()));
    }

    /**
     * 导入种子任务。
     *
     * @param clientRequestId 幂等键
     * @param torrentFile 种子文件
     * @param saveDir 保存目录
     * @return 统一响应
     */
    @PostMapping("/torrent")
    public ApiResponse<TaskCreateResponse> importTorrentTask(
        @RequestParam("clientRequestId") @NotBlank(message = "不能为空") String clientRequestId,
        @RequestPart("torrentFile") MultipartFile torrentFile,
        @RequestParam(value = "saveDir", required = false) String saveDir
    ) {
        if (torrentFile == null || torrentFile.isEmpty()) {
            throw new BizException(ErrorCode.TORRENT_PARSE_FAILED, "torrentFile 不能为空");
        }
        String storedTorrentPath = storeTorrentFile(clientRequestId, torrentFile);
        TaskOperationResult taskOperationResult = taskWorkflowService.createTask(
            taskControllerConverter.toTorrentCreateCommand(
                clientRequestId,
                storedTorrentPath,
                torrentFile.getOriginalFilename(),
                configService.resolveSaveDir(saveDir),
                RequestContext.getClientType()
            )
        );
        return ApiResponse.success(taskControllerConverter.toCreateResponse(taskOperationResult.getTaskModel()));
    }

    /**
     * 查询任务列表。
     *
     * @param status 状态过滤
     * @param keyword 关键字
     * @param pageNo 页码
     * @param pageSize 每页条数
     * @return 统一响应
     */
    @GetMapping
    public ApiResponse<TaskListResponse> listTasks(
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "pageNo", defaultValue = "1") @Min(value = 1, message = "必须大于 0") Integer pageNo,
        @RequestParam(value = "pageSize", defaultValue = "50") @Min(value = 1, message = "必须大于 0")
        @Max(value = 200, message = "不能超过 200") Integer pageSize
    ) {
        TaskPageQuery taskPageQuery = new TaskPageQuery();
        taskPageQuery.setStatus(status);
        taskPageQuery.setKeyword(keyword);
        taskPageQuery.setPageNo(pageNo);
        taskPageQuery.setPageSize(pageSize);
        return ApiResponse.success(taskControllerConverter.toListResponse(taskQueryService.listTasks(taskPageQuery)));
    }

    /**
     * 查询任务详情。
     *
     * @param taskId 任务 ID
     * @return 统一响应
     */
    @GetMapping("/{id}")
    public ApiResponse<TaskDetailResponse> getTaskDetail(@PathVariable("id") Long taskId) {
        return ApiResponse.success(taskControllerConverter.toDetailResponse(taskQueryService.getTaskById(taskId)));
    }

    /**
     * 暂停任务。
     *
     * @param taskId 任务 ID
     * @return 统一响应
     */
    @PostMapping("/{id}/pause")
    public ApiResponse<TaskOperationResponse> pauseTask(@PathVariable("id") Long taskId) {
        TaskOperationResult taskOperationResult = taskWorkflowService.pauseTask(taskId);
        return ApiResponse.success(taskControllerConverter.toOperationResponse(taskOperationResult));
    }

    /**
     * 继续任务。
     *
     * @param taskId 任务 ID
     * @return 统一响应
     */
    @PostMapping("/{id}/resume")
    public ApiResponse<TaskOperationResponse> resumeTask(@PathVariable("id") Long taskId) {
        TaskOperationResult taskOperationResult = taskWorkflowService.resumeTask(taskId);
        return ApiResponse.success(taskControllerConverter.toOperationResponse(taskOperationResult));
    }

    /**
     * 重试任务。
     *
     * @param taskId 任务 ID
     * @return 统一响应
     */
    @PostMapping("/{id}/retry")
    public ApiResponse<TaskOperationResponse> retryTask(@PathVariable("id") Long taskId) {
        TaskOperationResult taskOperationResult = taskWorkflowService.retryTask(taskId);
        return ApiResponse.success(taskControllerConverter.toOperationResponse(taskOperationResult));
    }

    /**
     * 删除任务。
     *
     * @param taskId 任务 ID
     * @param removeFiles 是否删除文件
     * @return 统一响应
     */
    @DeleteMapping("/{id}")
    public ApiResponse<TaskDeleteResponse> deleteTask(
        @PathVariable("id") Long taskId,
        @RequestParam(value = "removeFiles", defaultValue = "false") boolean removeFiles
    ) {
        TaskDeleteExecutionResult taskDeleteExecutionResult = taskWorkflowService.deleteTask(taskId, removeFiles);
        return ApiResponse.success(taskControllerConverter.toDeleteResponse(
            taskDeleteExecutionResult.getTaskOperationResult(),
            taskDeleteExecutionResult.isFilesRemoved()
        ));
    }

    private String storeTorrentFile(String clientRequestId, MultipartFile torrentFile) {
        try {
            Path torrentDirectory = Paths.get(System.getProperty("java.io.tmpdir"), "mooddownload", "torrents");
            Files.createDirectories(torrentDirectory);
            String originalFilename = torrentFile.getOriginalFilename();
            String safeFileName = (originalFilename == null || originalFilename.trim().isEmpty())
                ? clientRequestId + ".torrent"
                : originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path targetPath = torrentDirectory.resolve(clientRequestId + "-" + safeFileName);
            torrentFile.transferTo(targetPath.toFile());
            LOGGER.info("保存种子文件成功: clientRequestId={}, path={}", clientRequestId, targetPath);
            return targetPath.toString();
        } catch (IOException exception) {
            LOGGER.error("保存种子文件失败: clientRequestId={}", clientRequestId, exception);
            throw new BizException(ErrorCode.TORRENT_PARSE_FAILED, "保存种子文件失败");
        }
    }
}
