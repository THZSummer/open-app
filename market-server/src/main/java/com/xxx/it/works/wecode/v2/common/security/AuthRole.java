package com.xxx.it.works.wecode.v2.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限角色注解
 *
 * <p>标注在Controller方法上，表示该接口需要权限校验</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthRole {
}