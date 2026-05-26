package com.xxx.it.works.wecode.v2.modules.connector.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 限流配置
 * <p>
 * 声明每秒最大请求数 (QPS)。
 * 旧字段名为 rateLimit，通过 @JsonAlias 或全局配置兼容。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RateLimitConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 每秒最大请求数 */
    private int maxQps;
}