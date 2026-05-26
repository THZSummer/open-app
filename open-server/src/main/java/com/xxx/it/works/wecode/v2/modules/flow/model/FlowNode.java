package com.xxx.it.works.wecode.v2.modules.flow.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 编排节点
 * <p>
 * React Flow 格式节点: {id, type, position, data}
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlowNode implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 节点唯一标识 */
    private String id;

    /** 节点类型: "trigger", "connector", "exit" */
    private String type;

    /** 节点在画布上的位置 */
    private NodePosition position;

    /** 节点数据 (类型差异化) */
    private NodeData data;
}