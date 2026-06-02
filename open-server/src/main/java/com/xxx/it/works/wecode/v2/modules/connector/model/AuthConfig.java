package com.xxx.it.works.wecode.v2.modules.connector.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 认证配置
 * <p>
 * 声明认证类型及其字段 Schema。
 * type 为字符串枚举，如 "SYSTOKEN", "AKSK", "BASIC_AUTH" 等。
 * fields 声明各认证字段的名称、传递载体、字段名、是否必填、是否敏感。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 认证类型 (字符串枚举: "SYSTOKEN", "AKSK", "BASIC_AUTH", ...) */
    private String type;

    /** 认证字段列表 */
    private List<AuthField> fields;
}