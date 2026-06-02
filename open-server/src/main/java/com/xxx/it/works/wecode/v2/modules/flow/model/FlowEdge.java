package com.xxx.it.works.wecode.v2.modules.flow.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 编排边 (React Flow 格式)
 * <p>
 * sourceNodeId / targetNodeId 通过 @JsonAlias 向后兼容。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlowEdge implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 边唯一标识 */
    private String id;

    /** 源节点 ID (旧名: sourceNodeId) */
    @JsonProperty("source")
    @JsonAlias("sourceNodeId")
    private String source;

    /** 目标节点 ID (旧名: targetNodeId) */
    @JsonProperty("target")
    @JsonAlias("targetNodeId")
    private String target;

    /** 边的类型，如 "smoothstep" */
    private String type;

    /** 边数据 */
    private EdgeData data;
}