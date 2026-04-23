package com.xxx.api.common.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 订阅关系实体
 * 
 * <p>对应表 openplatform_v2_subscription_t</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class Subscription implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long appId;
    private Long permissionId;
    private Integer status;
    private Integer channelType;
    private String channelAddress;
    /**
     * 认证方式：0=Cookie, 1=SOA, 2=APIG, 3=IAM, 4=免认证, 5=AKSK, 6=CLITOKEN
     */
    private Integer authType;
    private Date createTime;
    private Date lastUpdateTime;
    private String createBy;
    private String lastUpdateBy;
    private Date approvedAt;
    private String approvedBy;
}
