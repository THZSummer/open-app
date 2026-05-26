package com.xxx.it.works.wecode.v2.modules.flow.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 边数据
 * <p>
 * 边的业务属性。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EdgeData implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务类型，如 "default" */
    private String businessType;
}