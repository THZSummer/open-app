package com.xxx.it.works.wecode.v2.modules.lookup.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.task.TaskCreateDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.task.TaskQueryDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.task.TaskUpdateStatusDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.service.LookUpFileService;
import com.xxx.it.works.wecode.v2.modules.lookup.service.TaskService;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.common.PageVO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.task.TaskListVO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.task.TaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;

/**
 * 任务管理 Controller
 *
 * <p>提供任务的CRUD接口</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/lookup/tasks")
@RequiredArgsConstructor
@Tag(name = "任务管理", description = "LookUp任务管理接口")
public class TaskController {

    private final TaskService taskService;
    private final LookUpFileService lookUpFileService;

    /**
     * 创建任务
     *
     * @param createDTO 创建参数
     * @return 任务ID
     */
    @PostMapping
    @Operation(summary = "创建任务",
               description = "创建导入或导出任务")
    public ApiResponse<Long> createTask(
            @Valid @RequestBody TaskCreateDTO createDTO) {

        log.info("Create task, taskType={}, bizType={}, fileName={}",
                createDTO.getTaskType(), createDTO.getBizType(), createDTO.getFileName());

        return taskService.createTask(createDTO);
    }

    /**
     * 更新任务状态
     *
     * @param taskId           任务ID
     * @param updateStatusDTO 更新参数
     * @return 操作结果
     */
    @PutMapping("/{taskId}/status")
    @Operation(summary = "更新任务状态",
               description = "更新任务的状态和结果描述")
    public ApiResponse<Void> updateTaskStatus(
            @Parameter(description = "任务ID", required = true)
            @PathVariable Long taskId,
            @Valid @RequestBody TaskUpdateStatusDTO updateStatusDTO) {

        log.info("Update task status, taskId={}, status={}",
                taskId, updateStatusDTO.getStatus());

        return taskService.updateTaskStatus(taskId, updateStatusDTO);
    }

    /**
     * 获取任务列表
     *
     * @param taskType 任务类型：1-导入(IMPORT)，2-导出(EXPORT)
     * @param bizType  业务类型：1-LookUp，2-数据字典(DATA_DICTIONARY)
     * @param status   任务状态：0-待处理(PENDING)，1-处理中(PROCESSING)，2-已完成(COMPLETED)，3-失败(FAILED)
     * @param pageNum  页码，默认1
     * @param pageSize 每页条数，默认10
     * @return 分页任务列表
     */
    @GetMapping
    @Operation(summary = "获取任务列表",
               description = "支持分页和状态筛选，返回任务列表")
    public ApiResponse<PageVO<TaskListVO>> getTaskList(
            @Parameter(description = "任务类型：1-导入(IMPORT)，2-导出(EXPORT)")
            @RequestParam(required = false) Integer taskType,
            @Parameter(description = "业务类型：1-LookUp，2-数据字典(DATA_DICTIONARY)")
            @RequestParam(required = false) Integer bizType,
            @Parameter(description = "任务状态：0-待处理(PENDING)，1-处理中(PROCESSING)，2-已完成(COMPLETED)，3-失败(FAILED)")
            @RequestParam(required = false) Integer status,
            @Parameter(description = "页码，默认1")
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数，默认10")
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {

        log.info("Get task list, taskType={}, bizType={}, status={}, pageNum={}, pageSize={}",
                taskType, bizType, status, pageNum, pageSize);

        TaskQueryDTO queryDTO = new TaskQueryDTO();
        queryDTO.setTaskType(taskType);
        queryDTO.setBizType(bizType);
        queryDTO.setStatus(status);
        queryDTO.setPageNum(pageNum);
        queryDTO.setPageSize(pageSize);

        return taskService.getTaskList(queryDTO);
    }

    /**
     * 获取任务详情
     *
     * @param taskId 任务ID
     * @return 任务详情
     */
    @GetMapping("/{taskId}")
    @Operation(summary = "获取任务详情",
               description = "获取任务的详细信息")
    public ApiResponse<TaskVO> getTaskDetail(
            @Parameter(description = "任务ID", required = true)
            @PathVariable Long taskId) {

        log.info("Get task detail, taskId={}", taskId);

        return taskService.getTaskDetail(taskId);
    }

    /**
     * 下载任务结果文件
     *
     * @param taskId 任务ID
     * @param response HTTP响应
     */
    @GetMapping("/{taskId}/download")
    @Operation(summary = "下载任务结果文件",
               description = "下载任务生成的结果文件（如导出Excel）")
    public void downloadTaskResult(
            @Parameter(description = "任务ID", required = true)
            @PathVariable Long taskId,
            HttpServletResponse response) {

        log.info("Download task result, taskId={}", taskId);

        ApiResponse<TaskVO> taskResponse = taskService.getTaskDetail(taskId);
        if (taskResponse.getCode() == null || !taskResponse.getCode().equals("200") || taskResponse.getData() == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        TaskVO task = taskResponse.getData();
        String filePath = null;
        if (task.getFileId() != null) {
            filePath = lookUpFileService.getFilePath(Long.parseLong(task.getFileId()));
        }
        if (filePath == null || filePath.isEmpty()) {
            filePath = task.getResult();
        }
        if (filePath == null || filePath.isEmpty()) {
            throw new RuntimeException("Task result file not available");
        }

        File file = new File(filePath);
        if (!file.exists()) {
            throw new RuntimeException("Task result file not found: " + filePath);
        }

        try {
            String fileName = file.getName();
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader("Content-Disposition", 
                    "attachment; filename=" + java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8));
            response.setContentLengthLong(file.length());

            try (java.io.InputStream is = new java.io.FileInputStream(file);
                 java.io.OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }
        } catch (IOException e) {
            log.error("Failed to download task result, taskId={}", taskId, e);
            throw new RuntimeException("Download failed: " + e.getMessage());
        }
    }
}
