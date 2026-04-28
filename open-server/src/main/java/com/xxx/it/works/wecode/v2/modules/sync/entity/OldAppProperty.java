package com.xxx.it.works.wecode.v2.modules.sync.entity;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 旧应用属性实体
 * 对应表：openplatform_app_p_t
 * 
 * 用于获取事件订阅的通道配置
 */
@Data
public class OldAppProperty implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private Long parentId;              // 关联应用的 app_id
    private String propertyName;
    private String propertyValue;
    private Integer status;
    private String createBy;
    private Date createTime;
    private String lastUpdateBy;
    private Date lastUpdateTime;
    
    // === 非数据库字段，用于通道配置 ===
    
    /**
     * 通道类型：1=WebHook, 2=企业内部消息通道
     * 对应 event_msg_recive_mode
     */
    private Integer channelType;
    
    /**
     * 通道地址
     * 对应 event_push_url
     */
    private String channelAddress;
    
    /**
     * 认证类型：固定值1（SOA）
     * 对应 event_push_auth_type
     */
    private Integer authType;
}