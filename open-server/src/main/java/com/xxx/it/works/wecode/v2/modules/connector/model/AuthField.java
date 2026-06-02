package com.xxx.it.works.wecode.v2.modules.connector.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 认证字段定义
 * <p>
 * 声明单个认证字段的名称、传递载体、实际字段名、是否必填、是否敏感。
 * 例如：accessKey 通过 header 传递，字段名为 "AK"，必填且敏感。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthField implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 字段名称 (逻辑名，如 "accessKey", "secretKey") */
    private String name;

    /** 传递载体 (如 "header", "query") */
    private String carrier;

    /** 实际字段名 (如 "AK", "SK", "X-Sys-Token") */
    private String fieldName;

    /** 是否必填 */
    private boolean required;

    /** 是否敏感 (敏感字段不记录日志) */
    private boolean sensitive;
}