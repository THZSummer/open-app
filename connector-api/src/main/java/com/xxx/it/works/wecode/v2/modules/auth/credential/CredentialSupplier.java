package com.xxx.it.works.wecode.v2.modules.auth.credential;

import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import java.util.Map;

/**
 * 凭据提供 — 每种 authType 一个实现类。
 *
 * <p>key 是 value 表达式原文，value 是解析后的凭据值。
 * ExecutionContext 为运行时上下文，供 COOKIE 等需要运行时的类型取值。
 * authConfig 为完整的认证配置 Map，供 SIGNATURE 等需要读取 secretKey 的类型使用。
 * </p>
 */
public interface CredentialSupplier {

    /** 匹配的 authConfig.type */
    String getAuthType();

    /**
     * @param fieldDefs  字段定义 Map&lt;fieldName, {value, type, sensitive, ...}&gt;
     * @param authConfig 完整 authConfig Map（含 type/secretKey/header/query 等），SIGNATURE 等类型需要
     * @param context    运行时上下文，COOKIE 等类型需要，其他类型可忽略
     * @return Map&lt;value表达式, 解析后的凭据值&gt;
     */
    Map<String, String> resolve(Map<String, Object> fieldDefs, Map<String, Object> authConfig, ExecutionContext context);
}
