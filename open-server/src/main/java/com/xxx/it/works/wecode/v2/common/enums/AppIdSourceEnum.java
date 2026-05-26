package com.xxx.it.works.wecode.v2.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审计日志 app_id 来源枚举
 *
 * <p>用于 @AuditLog 注解，控制审计日志中 app_id 字段的取值方式</p>
 * <p>审计日志 app_id 统一取 openplatform_app_t.app_id（varchar 外部业务 ID）</p>
 *
 * @author SDDU Build Agent
 * @version 2.0.0
 */
@Getter
@AllArgsConstructor
public enum AppIdSourceEnum {

    /**
     * 从方法参数中直接提取 appId
     *
     * <p>适用于路径中包含 {appId} 的接口，如：
     * /service/open/v2/apps/{appId}/apis/subscribe</p>
     *
     * <p>此时 {appId} 已是 openplatform_app_t.app_id（varchar 外部业务 ID）</p>
     */
    PATH_VARIABLE,

    /**
     * 从实体快照中反向查找 appId
     *
     * <p>适用于接口参数中无 appId，但实体记录中包含 app_id 字段的场景。
     * 切面先加载实体快照（before_data），从中提取 numeric app_id（openplatform_app_t.id），
     * 再通过 AppContextResolver.toExternalId() 转换为 varchar app_id（openplatform_app_t.app_id）。</p>
     */
    ENTITY;
}
