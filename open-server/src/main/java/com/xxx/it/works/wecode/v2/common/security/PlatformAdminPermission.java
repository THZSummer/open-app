package com.xxx.it.works.wecode.v2.common.security;

import java.lang.annotation.*;

/**
 * 平台管理员权限注解
 *
 * <p>标注在Controller方法上，表示该接口需要平台管理员权限</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PlatformAdminPermission {
}