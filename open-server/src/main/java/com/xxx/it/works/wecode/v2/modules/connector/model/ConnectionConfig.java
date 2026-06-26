package com.xxx.it.works.wecode.v2.modules.connector.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 连接配置 (v6.0 — 对齐 plan-json-schema.md connectorVersionConfigDef)
 * <p>
 * 对应 connectionConfig JSON 结构，字段名与 Schema §4.3.7 严格一致。
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

    /** 协议配置，如 {url, method} */
    private Map<String, Object> protocolConfig;

    /** 认证配置列表 (Schema: authConfigs, minItems 1) */
    @JsonProperty("authConfigs")
    private List<AuthConfig> authConfigs;

    /** 入参 Schema 声明 (Schema: input → httpInputDef) */
    @JsonProperty("input")
    private ContractSchema input;

    /** 出参 Schema 声明 (Schema: output → httpOutputDef) */
    @JsonProperty("output")
    private ContractSchema output;

    /** 超时时间 (毫秒) */
    private int timeoutMs;

    /** 限流配置 */
    @JsonProperty("rateLimitConfig")
    private RateLimitConfig rateLimitConfig;
}
