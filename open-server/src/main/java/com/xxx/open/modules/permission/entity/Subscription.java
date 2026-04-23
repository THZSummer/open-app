package com.xxx.open.modules.permission.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 订阅关系实体
 * 
 * <p>对应表 openplatform_v2_subscription_t</p>
 * <p>记录应用对权限资源的订阅关系</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class Subscription implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订阅ID
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
     * 订阅状态：0=待审, 1=已授权, 2=已拒绝, 3=已取消
     */
    private Integer status;

    /**
     * 通道类型：0=内部消息队列, 1=WebHook, 2=SSE, 3=WebSocket
     */
    private Integer channelType;

    /**
     * 通道地址
     */
    private String channelAddress;

    /**
     * 认证方式：0=Cookie, 1=SOA, 2=APIG, 3=IAM, 4=免认证, 5=AKSK, 6=CLITOKEN
     */
    private Integer authType;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 最后更新时间
     */
    private Date lastUpdateTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 最后更新人
     */
    private String lastUpdateBy;

    /**
     * 审批时间
     */
    private Date approvedAt;

    /**
     * 审批人
     */
    private String approvedBy;
}
