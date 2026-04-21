package com.xxx.api.gateway.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 权限校验请求
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class PermissionCheckRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 权限标识（Scope）
     */
    private String scope;
}
