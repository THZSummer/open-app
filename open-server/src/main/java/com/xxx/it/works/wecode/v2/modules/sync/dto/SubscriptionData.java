package com.xxx.it.works.wecode.v2.modules.sync.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 订阅关系数据（用于应急接口）
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class SubscriptionData implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（必填）
     */
    private Long id;

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 权限ID
     */
    private Long permissionId;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 权限类型（旧表：0=API, 1=事件）
     */
    private String permissionType;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 通道类型（新表）
     */
    private Integer channelType;

    /**
     * 通道地址（新表）
     */
    private String channelAddress;

    /**
     * 认证类型
     */
    private Integer authType;
}
