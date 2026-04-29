package com.xxx.it.works.wecode.v2.modules.sync.entity;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 旧审批日志实体
 * 对应表：openplatform_eflow_log_t
 */
@Data
public class OldEflowLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long eflowLogId;
    private Long eflowLogTraceId;
    private String eflowLogType;
    private String eflowLogUser;
    private String eflowLogMessage;
    private Integer status;
    private String createBy;
    private Date createTime;
    private String lastUpdateBy;
    private Date lastUpdateTime;
}