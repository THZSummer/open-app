package com.xxx.api.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 权限校验响应
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionCheckResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否已授权
     */
    private Boolean authorized;

    /**
     * 订阅ID
     */
    private String subscriptionId;

    /**
     * 订阅状态
     */
    private Integer subscriptionStatus;

    /**
     * 未授权原因
     */
    private String reason;
}
