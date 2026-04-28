package com.xxx.it.works.wecode.v2.modules.sync.entity;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 旧事件实体
 * 对应表：openplatform_event_t
 */
@Data
public class OldEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String eventNameCn;
    private String eventNameEn;
    private Long moduleId;
    private String topic;
    private String eventType;
    private Integer isApprovalRequired;
    private Integer status;
    private String createBy;
    private Date createTime;
    private String lastUpdateBy;
    private Date lastUpdateTime;
}