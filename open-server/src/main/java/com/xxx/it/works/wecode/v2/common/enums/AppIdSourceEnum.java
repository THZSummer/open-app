package com.xxx.it.works.wecode.v2.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审计日志 app_id 来源枚举
 *
 * <p>用于 @AuditLog 注解，控制 app_id 字段的取值方式</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor
public enum AppIdSourceEnum {

    /** 固定值 "platform"，用于资源管理接口（注册/更新/删除/撤回） */
    PLATFORM("platform"),

    /** 从路径参数 {appId} 获取，用于权限订阅接口 */
    PATH_VARIABLE("appId");

    /** PLATFORM 模式下的固定 app_id 值；PATH_VARIABLE 模式下表示参数名 */
    private final String value;
}
