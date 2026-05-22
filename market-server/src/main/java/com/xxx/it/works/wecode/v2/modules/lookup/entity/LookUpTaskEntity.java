package com.xxx.it.works.wecode.v2.modules.lookup.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务实体类
 *
 * <p>对应表 openplatform_lookup_task_t</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class LookUpTaskEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务ID，主键
     */
    private Long taskId;

    /**
     * 任务类型：1-导入(IMPORT)，2-导出(EXPORT)，对应 TaskTypeEnum
     */
    private Integer taskType;

    /**
     * 业务类型：1-LookUp，2-数据字典(DATA_DICTIONARY)，对应 BizTypeEnum
     */
    private Integer bizType;

    /**
     * 任务状态：0-待处理(PENDING)，1-处理中(PROCESSING)，2-已完成(COMPLETED)，3-失败(FAILED)，对应 TaskStatusEnum
     */
    private Integer status;

    /**
     * OBS文件ID
     */
    private String fileId;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 结果描述
     */
    private String result;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 最后更新人
     */
    private String lastUpdateBy;

    /**
     * 最后更新时间
     */
    private Date lastUpdateTime;
}
