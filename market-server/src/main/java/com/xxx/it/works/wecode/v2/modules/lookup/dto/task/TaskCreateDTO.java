package com.xxx.it.works.wecode.v2.modules.lookup.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 任务创建DTO
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Schema(description = "任务创建请求")
public class TaskCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务类型：1-导入(IMPORT)，2-导出(EXPORT)，对应 TaskTypeEnum
     */
    @NotNull(message = "任务类型不能为空")
    @Schema(description = "任务类型：1-导入(IMPORT)，2-导出(EXPORT)，对应 TaskTypeEnum", required = true)
    private Integer taskType;

    /**
     * 业务类型：1-LookUp，2-数据字典(DATA_DICTIONARY)，对应 BizTypeEnum
     */
    @NotNull(message = "业务类型不能为空")
    @Schema(description = "业务类型：1-LookUp，2-数据字典(DATA_DICTIONARY)，对应 BizTypeEnum", required = true)
    private Integer bizType;

    /**
     * 文件名称
     */
    @NotBlank(message = "文件名称不能为空")
    @Schema(description = "文件名称", required = true)
    private String fileName;

    /**
     * OBS文件ID
     */
    @Schema(description = "OBS文件ID")
    private String fileId;
}
