package com.xxx.api.scope.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 用户授权创建请求
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class UserAuthorizationCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private String userId;

    /**
     * 应用ID
     */
    @NotNull(message = "应用ID不能为空")
    private String appId;

    /**
     * Scope 列表
     */
    @NotEmpty(message = "Scope 列表不能为空")
    private List<String> scopes;

    /**
     * 过期时间，不填则永久有效
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date expiresAt;
}
