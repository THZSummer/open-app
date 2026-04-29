package com.xxx.it.works.wecode.v2.modules.sync.entity;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 旧审批流程实体
 * 对应表：openplatform_eflow_t
 */
@Data
public class OldEflow implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long eflowId;
    private String eflowType;
    private Integer eflowStatus;
    private String eflowSubmitUser;
    private String eflowSubmitMessage;
    private String eflowAuditUser;    // 审批用户（用于构造combined_nodes）
    private String eflowAuditMessage;
    private String resourceType;
    private Long resourceId;
    private String resourceInfo;
    private String resourceDelta;
    private String tenantId;
    private Integer status;
    private String createBy;
    private Date createTime;
    private String lastUpdateBy;
    private Date lastUpdateTime;
}