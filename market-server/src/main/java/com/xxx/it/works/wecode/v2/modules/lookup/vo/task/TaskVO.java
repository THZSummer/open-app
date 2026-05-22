package com.xxx.it.works.wecode.v2.modules.lookup.vo.task;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务详情VO
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Schema(description = "任务详情")
public class TaskVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    @Schema(description = "任务ID", example = "1")
    private Long taskId;

    /**
     * 任务类型：1-导入(IMPORT)，2-导出(EXPORT)
     */
    @Schema(description = "任务类型：1-导入(IMPORT)，2-导出(EXPORT)", example = "1")
    private Integer taskType;

    /**
     * 任务类型名称
     */
    @Schema(description = "任务类型名称", example = "导入")
    private String taskTypeName;

    /**
     * 业务类型：1-LookUp，2-数据字典(DATA_DICTIONARY)
     */
    @Schema(description = "业务类型：1-LookUp，2-数据字典(DATA_DICTIONARY)", example = "1")
    private Integer bizType;

    /**
     * 业务类型名称
     */
    @Schema(description = "业务类型名称", example = "LookUp")
    private String bizTypeName;

    /**
     * 任务状态：0-待处理(PENDING)，1-处理中(PROCESSING)，2-已完成(COMPLETED)，3-失败(FAILED)
     */
    @Schema(description = "任务状态：0-待处理(PENDING)，1-处理中(PROCESSING)，2-已完成(COMPLETED)，3-失败(FAILED)", example = "0")
    private Integer status;

    /**
     * 任务状态名称
     */
    @Schema(description = "任务状态名称", example = "待处理")
    private String statusName;

    /**
     * OBS文件ID
     */
    @Schema(description = "OBS文件ID")
    private String fileId;

    /**
     * 文件名称
     */
    @Schema(description = "文件名称")
    private String fileName;

    /**
     * 结果描述
     */
    @Schema(description = "结果描述")
    private String result;

    /**
     * 创建人
     */
    @Schema(description = "创建人")
    private String createBy;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 最后更新人
     */
    @Schema(description = "最后更新人")
    private String lastUpdateBy;

    /**
     * 最后更新时间
     */
    @Schema(description = "最后更新时间")
    private Date lastUpdateTime;
}
