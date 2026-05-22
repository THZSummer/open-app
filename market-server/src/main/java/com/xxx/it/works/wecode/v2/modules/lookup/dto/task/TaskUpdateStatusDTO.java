package com.xxx.it.works.wecode.v2.modules.lookup.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 任务状态更新DTO
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Schema(description = "任务状态更新请求")
public class TaskUpdateStatusDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务状态：0-待处理(PENDING)，1-处理中(PROCESSING)，2-已完成(COMPLETED)，3-失败(FAILED)，对应 TaskStatusEnum
     */
    @NotNull(message = "任务状态不能为空")
    @Schema(description = "任务状态：0-待处理(PENDING)，1-处理中(PROCESSING)，2-已完成(COMPLETED)，3-失败(FAILED)，对应 TaskStatusEnum", required = true)
    private Integer status;

    /**
     * 结果描述
     */
    @Schema(description = "结果描述")
    private String result;

    /**
     * 文件ID
     */
    @Schema(description = "文件ID")
    private Long fileId;
}
