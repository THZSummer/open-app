package com.xxx.api.scope.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 用户授权列表响应
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class UserAuthorizationListResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 授权ID
     */
    private String id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * Scope 列表
     */
    private List<String> scopes;

    /**
     * 过期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Date expiresAt;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Date createTime;
}
