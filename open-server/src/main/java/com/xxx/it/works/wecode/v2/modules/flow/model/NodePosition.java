package com.xxx.it.works.wecode.v2.modules.flow.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 节点位置
 * <p>
 * React Flow 画布坐标: {x, y}
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodePosition implements Serializable {

    private static final long serialVersionUID = 1L;

    /** X 坐标 */
    private double x;

    /** Y 坐标 */
    private double y;
}