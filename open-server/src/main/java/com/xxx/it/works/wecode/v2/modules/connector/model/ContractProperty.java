package com.xxx.it.works.wecode.v2.modules.connector.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 契约字段属性定义
 * <p>
 * 描述单个字段的数据类型。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContractProperty implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数据类型，如 "string", "integer", "boolean" */
    private String type;
}