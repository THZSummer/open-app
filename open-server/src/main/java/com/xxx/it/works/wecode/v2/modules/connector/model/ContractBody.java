package com.xxx.it.works.wecode.v2.modules.connector.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 契约 Body 段数据结构
 * <p>
 * 描述 HTTP 请求/响应中某一段 (header/query/body) 的数据结构。
 * type 为数据类型 (如 "object")，
 * properties 为字段属性映射，
 * required 为必填字段列表。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContractBody implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数据类型，如 "object" */
    private String type;

    /** 字段属性映射，key 为字段名，value 为属性定义 */
    private Map<String, ContractProperty> properties;

    /** 必填字段列表 */
    private List<String> required;
}