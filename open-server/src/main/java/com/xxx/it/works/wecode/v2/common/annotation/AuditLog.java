package com.xxx.it.works.wecode.v2.common.annotation;

import com.xxx.it.works.wecode.v2.common.enums.OperateEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解
 *
 * <p>标记在 Controller 方法上，AOP 切面自动捕获操作前后实体快照、用户信息和 IP 地址，
 * 异步写入操作日志表。</p>
 *
 * <p>appId 自动提取策略：方法参数 → beforeData 快照 → 返回值</p>
 *
 * @author SDDU Build Agent
 * @version 3.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /**
     * 操作枚举值（包含 operateType / operateObject / descCn / descEn）
     */
    OperateEnum value();

    /**
     * 资源 ID 参数名（用于加载实体快照），默认 "id"
     */
    String resourceIdParam() default "id";
}
