package com.xxx.it.works.wecode.v2.modules.flow.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 编排配置 v5.5 (React Flow 格式)
 * <p>
 * 对应 orchestrationConfig JSON 结构。
 * 采用 React Flow 格式：nodes 为节点列表，edges 为边列表。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrchestrationConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 节点列表 */
    private List<FlowNode> nodes;

    /** 边列表 */
    private List<FlowEdge> edges;
}