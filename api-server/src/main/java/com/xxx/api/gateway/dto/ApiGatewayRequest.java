package com.xxx.api.gateway.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * API 网关代理请求
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ApiGatewayRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 请求路径
     */
    private String path;

    /**
     * 请求方法
     */
    private String method;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 认证类型
     */
    private Integer authType;

    /**
     * 请求头
     */
    private java.util.Map<String, String> headers;

    /**
     * 请求体
     */
    private String body;
}
