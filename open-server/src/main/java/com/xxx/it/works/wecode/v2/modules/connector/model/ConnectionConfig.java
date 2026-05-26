package com.xxx.it.works.wecode.v2.modules.connector.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 连接配置 v5.5
 * <p>
 * 对应 connectionConfig JSON 结构。
 * 通过 @JsonAlias 实现向后兼容：authConfig ← authTypeSchema,
 * inputContract ← inputSchema, outputContract ← outputSchema,
 * rateLimitConfig ← rateLimit。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConnectionConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 协议类型，如 "HTTP" */
    private String protocol;

    /** 协议配置，如 {url, method, headers...} */
    private Map<String, Object> protocolConfig;

    /** 认证配置 (旧名: authTypeSchema) */
    @JsonProperty("authConfig")
    @JsonAlias("authTypeSchema")
    private AuthConfig authConfig;

    /** 输入契约 Schema (旧名: inputSchema) */
    @JsonProperty("inputContract")
    @JsonAlias("inputSchema")
    private ContractSchema inputContract;

    /** 输出契约 Schema (旧名: outputSchema) */
    @JsonProperty("outputContract")
    @JsonAlias("outputSchema")
    private ContractSchema outputContract;

    /** 超时时间 (毫秒) */
    private int timeoutMs;

    /** 限流配置 (旧名: rateLimit) */
    @JsonProperty("rateLimitConfig")
    @JsonAlias("rateLimit")
    private RateLimitConfig rateLimitConfig;
}