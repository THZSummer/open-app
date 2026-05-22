package com.xxx.it.works.wecode.v2.modules.lookup.service.impl;

import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.task.TaskCreateDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.task.TaskQueryDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.task.TaskUpdateStatusDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.entity.LookUpTaskEntity;
import com.xxx.it.works.wecode.v2.modules.lookup.enums.BizTypeEnum;
import com.xxx.it.works.wecode.v2.modules.lookup.enums.TaskStatusEnum;
import com.xxx.it.works.wecode.v2.modules.lookup.enums.TaskTypeEnum;
import com.xxx.it.works.wecode.v2.modules.lookup.mapper.LookUpTaskMapper;
import com.xxx.it.works.wecode.v2.modules.lookup.service.TaskService;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.common.PageVO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.task.TaskListVO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.task.TaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务服务实现
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final LookUpTaskMapper lookUpTaskMapper;

    /**
     * 创建任务（参数重载）
     *
     * @param taskType 任务类型
     * @param bizType  业务类型
     * @param fileName 文件名称
     * @return 任务ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Long> createTask(Integer taskType, Integer bizType, String fileName) {
        log.info("Create task (by params), taskType={}, bizType={}, fileName={}", taskType, bizType, fileName);

        // 构造DTO并调用主要创建方法
        TaskCreateDTO createDTO = new TaskCreateDTO();
        createDTO.setTaskType(taskType);
        createDTO.setBizType(bizType);
        createDTO.setFileName(fileName);

        return createTask(createDTO);
    }

    /**
     * 创建任务（DTO方式）
     *
     * @param createDTO 创建参数
     * @return 任务ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Long> createTask(TaskCreateDTO createDTO) {
        log.info("Create task, taskType={}, bizType={}, fileName={}",
                createDTO.getTaskType(), createDTO.getBizType(), createDTO.getFileName());

        // 校验任务类型
        if (createDTO.getTaskType() != null) {
            TaskTypeEnum taskTypeEnum = TaskTypeEnum.of(createDTO.getTaskType());
            if (taskTypeEnum == null) {
                return ApiResponse.error("40001",
                        "任务类型不存在: " + createDTO.getTaskType(),
                        "Task type not found: " + createDTO.getTaskType());
            }
        }

        // 校验业务类型
        if (createDTO.getBizType() != null) {
            BizTypeEnum bizTypeEnum = BizTypeEnum.of(createDTO.getBizType());
            if (bizTypeEnum == null) {
                return ApiResponse.error("40002",
                        "业务类型不存在: " + createDTO.getBizType(),
                        "Biz type not found: " + createDTO.getBizType());
            }
        }

        // 创建实体
        LookUpTaskEntity entity = new LookUpTaskEntity();
        entity.setTaskType(createDTO.getTaskType());
        entity.setBizType(createDTO.getBizType());
        entity.setFileName(createDTO.getFileName());
        entity.setFileId(createDTO.getFileId());
        entity.setStatus(TaskStatusEnum.PENDING.getCode());
        entity.setCreateBy(UserContextHolder.getUserId());
        entity.setCreateTime(new Date());
        entity.setLastUpdateBy(UserContextHolder.getUserId());
        entity.setLastUpdateTime(new Date());

        // 插入数据
        lookUpTaskMapper.insert(entity);

        log.info("Task created successfully, taskId={}", entity.getTaskId());

        return ApiResponse.success(entity.getTaskId());
    }

    /**
     * 更新任务状态
     *
     * @param taskId           任务ID
     * @param updateStatusDTO 更新参数
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> updateTaskStatus(Long taskId, TaskUpdateStatusDTO updateStatusDTO) {
        log.info("Update task status, taskId={}, status={}, result={}",
                taskId, updateStatusDTO.getStatus(), updateStatusDTO.getResult());

        // 校验任务是否存在
        LookUpTaskEntity existTask = lookUpTaskMapper.selectById(taskId);
        if (existTask == null) {
            return ApiResponse.error("40401",
                    "任务不存在: " + taskId,
                    "Task not found: " + taskId);
        }

        // 校验状态值
        if (updateStatusDTO.getStatus() != null) {
            TaskStatusEnum statusEnum = TaskStatusEnum.of(updateStatusDTO.getStatus());
            if (statusEnum == null) {
                return ApiResponse.error("40003",
                        "任务状态不存在: " + updateStatusDTO.getStatus(),
                        "Task status not found: " + updateStatusDTO.getStatus());
            }
        }

        // 更新任务状态
        int updated = lookUpTaskMapper.updateStatus(
                taskId,
                updateStatusDTO.getStatus(),
                updateStatusDTO.getResult(),
                updateStatusDTO.getFileId()
        );

        if (updated == 0) {
            log.error("Failed to update task status, taskId={}", taskId);
            return ApiResponse.error("50001",
                    "更新任务状态失败",
                    "Failed to update task status");
        }

        log.info("Task status updated successfully, taskId={}", taskId);

        return ApiResponse.success();
    }

    /**
     * 获取任务列表（分页）
     *
     * @param queryDTO 查询条件
     * @return 分页任务列表
     */
    @Override
    public ApiResponse<PageVO<TaskListVO>> getTaskList(TaskQueryDTO queryDTO) {
        log.debug("Get task list, queryDTO={}", queryDTO);

        // 设置默认值
        if (queryDTO.getPageNum() == null || queryDTO.getPageNum() < 1) {
            queryDTO.setPageNum(1);
        }
        if (queryDTO.getPageSize() == null || queryDTO.getPageSize() < 1) {
            queryDTO.setPageSize(10);
        }

        // 计算分页参数
        int offset = (queryDTO.getPageNum() - 1) * queryDTO.getPageSize();
        int limit = queryDTO.getPageSize();

        // 查询列表
        List<LookUpTaskEntity> entityList = lookUpTaskMapper.selectList(
                queryDTO.getTaskType(),
                queryDTO.getBizType(),
                queryDTO.getStatus(),
                offset,
                limit
        );

        // 统计总数
        long total = lookUpTaskMapper.countList(
                queryDTO.getTaskType(),
                queryDTO.getBizType(),
                queryDTO.getStatus()
        );

        // 转换为VO
        List<TaskListVO> voList = entityList.stream()
                .map(this::convertToListVO)
                .collect(Collectors.toList());

        // 构建分页结果
        PageVO<TaskListVO> pageVO = PageVO.of(voList, total, queryDTO.getPageNum(), queryDTO.getPageSize());

        return ApiResponse.success(pageVO);
    }

    /**
     * 获取任务详情
     *
     * @param taskId 任务ID
     * @return 任务详情
     */
    @Override
    public ApiResponse<TaskVO> getTaskDetail(Long taskId) {
        log.info("Get task detail, taskId={}", taskId);

        // 查询任务
        LookUpTaskEntity entity = lookUpTaskMapper.selectById(taskId);
        if (entity == null) {
            return ApiResponse.error("40401",
                    "任务不存在: " + taskId,
                    "Task not found: " + taskId);
        }

        // 转换为VO
        TaskVO vo = convertToDetailVO(entity);

        return ApiResponse.success(vo);
    }

    /**
     * 转换为任务列表VO
     *
     * @param entity 任务实体
     * @return 任务列表VO
     */
    private TaskListVO convertToListVO(LookUpTaskEntity entity) {
        TaskListVO vo = new TaskListVO();
        vo.setTaskId(entity.getTaskId());
        vo.setTaskType(entity.getTaskType());
        vo.setTaskTypeName(TaskTypeEnum.getNameByCode(entity.getTaskType()));
        vo.setBizType(entity.getBizType());
        vo.setBizTypeName(BizTypeEnum.getNameByCode(entity.getBizType()));
        vo.setStatus(entity.getStatus());
        vo.setStatusName(TaskStatusEnum.getNameByCode(entity.getStatus()));
        vo.setFileId(entity.getFileId());
        vo.setFileName(entity.getFileName());
        vo.setResult(entity.getResult());
        vo.setCreateBy(entity.getCreateBy());
        vo.setCreateTime(entity.getCreateTime());
        vo.setLastUpdateTime(entity.getLastUpdateTime());
        return vo;
    }

    /**
     * 转换为任务详情VO
     *
     * @param entity 任务实体
     * @return 任务详情VO
     */
    private TaskVO convertToDetailVO(LookUpTaskEntity entity) {
        TaskVO vo = new TaskVO();
        vo.setTaskId(entity.getTaskId());
        vo.setTaskType(entity.getTaskType());
        vo.setTaskTypeName(TaskTypeEnum.getNameByCode(entity.getTaskType()));
        vo.setBizType(entity.getBizType());
        vo.setBizTypeName(BizTypeEnum.getNameByCode(entity.getBizType()));
        vo.setStatus(entity.getStatus());
        vo.setStatusName(TaskStatusEnum.getNameByCode(entity.getStatus()));
        vo.setFileId(entity.getFileId());
        vo.setFileName(entity.getFileName());
        vo.setResult(entity.getResult());
        vo.setCreateBy(entity.getCreateBy());
        vo.setCreateTime(entity.getCreateTime());
        vo.setLastUpdateBy(entity.getLastUpdateBy());
        vo.setLastUpdateTime(entity.getLastUpdateTime());
        return vo;
    }
}
