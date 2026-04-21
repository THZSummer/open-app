package com.xxx.api.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * API 网关代理响应
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiGatewayResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    private Integer statusCode;

    /**
     * 响应头
     */
    private java.util.Map<String, String> headers;

    /**
     * 响应体
     */
    private String body;

    /**
     * 错误信息
     */
    private String error;
}
