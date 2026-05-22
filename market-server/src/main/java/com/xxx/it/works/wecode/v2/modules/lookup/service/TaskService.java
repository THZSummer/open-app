package com.xxx.it.works.wecode.v2.modules.lookup.service;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.task.TaskCreateDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.task.TaskQueryDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.task.TaskUpdateStatusDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.common.PageVO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.task.TaskListVO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.task.TaskVO;

/**
 * 任务服务接口
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
public interface TaskService {

    /**
     * 创建任务
     *
     * @param taskType 任务类型
     * @param bizType  业务类型
     * @param fileName 文件名称
     * @return 任务ID
     */
    ApiResponse<Long> createTask(Integer taskType, Integer bizType, String fileName);

    /**
     * 创建任务（带文件ID）
     *
     * @param createDTO 创建参数
     * @return 任务ID
     */
    ApiResponse<Long> createTask(TaskCreateDTO createDTO);

    /**
     * 更新任务状态
     *
     * @param taskId       任务ID
     * @param updateStatusDTO 更新参数
     * @return 操作结果
     */
    ApiResponse<Void> updateTaskStatus(Long taskId, TaskUpdateStatusDTO updateStatusDTO);

    /**
     * 获取任务列表
     *
     * @param queryDTO 查询条件
     * @return 分页任务列表
     */
    ApiResponse<PageVO<TaskListVO>> getTaskList(TaskQueryDTO queryDTO);

    /**
     * 获取任务详情
     *
     * @param taskId 任务ID
     * @return 任务详情
     */
    ApiResponse<TaskVO> getTaskDetail(Long taskId);
}
