package com.xxx.it.works.wecode.v2.modules.flow.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xxx.it.works.wecode.v2.modules.connector.model.AuthConfig;
import com.xxx.it.works.wecode.v2.modules.connector.model.ContractSchema;
import com.xxx.it.works.wecode.v2.modules.connector.model.RateLimitConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 节点数据 (类型差异化)
 * <p>
 * 不同类型节点使用不同字段：
 * <ul>
 *   <li>所有节点: labelCn, labelEn</li>
 *   <li>trigger 节点: type (http/manual), authConfig, inputContract, rateLimitConfig</li>
 *   <li>connector 节点: authConfig, inputContract, outputContract, connectorId, url, method, timeoutMs, headers, inputMapping</li>
 *   <li>exit 节点: outputMapping</li>
 * </ul>
 * 未使用的字段通过 @JsonInclude(NON_NULL) 在序列化时自动忽略。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeData implements Serializable {

    private static final long serialVersionUID = 1L;

    // ====== 所有节点通用 ======

    /** 节点中文标签 */
    private String labelCn;

    /** 节点英文标签 */
    private String labelEn;

    // ====== trigger 节点 ======

    /** 触发类型: "http" / "manual" (仅 trigger 节点) */
    private String type;

    /** 认证配置 (trigger / connector 节点) */
    private AuthConfig authConfig;

    /** 输入契约 Schema (trigger / connector 节点) */
    private ContractSchema inputContract;

    /** 输出契约 Schema (connector 节点) */
    private ContractSchema outputContract;

    /** 限流配置 (trigger 节点) */
    private RateLimitConfig rateLimitConfig;

    // ====== connector 节点 ======

    /** 引用的连接器 ID */
    private String connectorId;

    /** 目标 URL (connector 节点 HTTP 目标) */
    private String url;

    /** HTTP 方法 (connector 节点) */
    private String method;

    /** 超时时间 (毫秒, connector 节点) */
    private int timeoutMs;

    /** 固定请求头 (connector 节点) */
    private Map<String, String> headers;

    /** 输入映射 (connector 节点: header/query/body 分段) */
    private InputMapping inputMapping;

    // ====== exit 节点 ======

    /** 输出映射 (exit 节点: header/body 分段) */
    private OutputMapping outputMapping;
}