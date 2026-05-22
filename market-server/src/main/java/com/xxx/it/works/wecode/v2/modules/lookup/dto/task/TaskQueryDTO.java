package com.xxx.it.works.wecode.v2.modules.lookup.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 任务查询DTO
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Schema(description = "任务查询请求")
public class TaskQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务类型：1-导入(IMPORT)，2-导出(EXPORT)，对应 TaskTypeEnum
     */
    @Schema(description = "任务类型：1-导入(IMPORT)，2-导出(EXPORT)，对应 TaskTypeEnum")
    private Integer taskType;

    /**
     * 业务类型：1-LookUp，2-数据字典(DATA_DICTIONARY)，对应 BizTypeEnum
     */
    @Schema(description = "业务类型：1-LookUp，2-数据字典(DATA_DICTIONARY)，对应 BizTypeEnum")
    private Integer bizType;

    /**
     * 任务状态：0-待处理(PENDING)，1-处理中(PROCESSING)，2-已完成(COMPLETED)，3-失败(FAILED)，对应 TaskStatusEnum
     */
    @Schema(description = "任务状态：0-待处理(PENDING)，1-处理中(PROCESSING)，2-已完成(COMPLETED)，3-失败(FAILED)，对应 TaskStatusEnum")
    private Integer status;

    /**
     * 页码，默认1
     */
    @Schema(description = "页码，默认1", example = "1")
    private Integer pageNum = 1;

    /**
     * 每页条数，默认10
     */
    @Schema(description = "每页条数，默认10", example = "10")
    private Integer pageSize = 10;
}
