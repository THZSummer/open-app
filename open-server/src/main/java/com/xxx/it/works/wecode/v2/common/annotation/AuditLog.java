package com.xxx.it.works.wecode.v2.common.annotation;

import com.xxx.it.works.wecode.v2.common.enums.OperateEnum;
import com.xxx.it.works.wecode.v2.common.enums.AppIdSourceEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解
 *
 * <p>标记在 Controller 方法上，AOP 切面自动捕获操作前后实体快照、用户信息和 IP 地址，
 * 异步写入 openplatform_operate_log_t 表</p>
 *
 * <p>使用示例：</p>
 * <pre>
 * &#64;AuditLog(value = OperateEnum.SUBSCRIBE_API_PERMISSION)
 * &#64;AuditLog(value = OperateEnum.CREATE_APP, appIdSource = AppIdSourceEnum.RESPONSE_FIELD)
 * </pre>
 *
 * @author SDDU Build Agent
 * @version 2.1.0
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
     * app_id 来源策略
     * <ul>
     *   <li>PATH_VARIABLE（默认）：从方法参数 appId 直接获取（已是 openplatform_app_t.app_id）</li>
     *   <li>ENTITY：从实体快照中提取 numeric app_id，再通过 AppContextResolver.toExternalId() 转换</li>
     *   <li>RESPONSE_FIELD：从返回值 ApiResponse.data.appId 中提取（CREATE 类操作）</li>
     * </ul>
     */
    AppIdSourceEnum appIdSource() default AppIdSourceEnum.PATH_VARIABLE;

    /**
     * 资源 ID 参数名
     *
     * <p>用于从方法参数中提取资源 ID，以加载实体快照（before_data / after_data）</p>
     * <p>默认 "id" 匹配现有 Controller 的 @PathVariable String id</p>
     * <p>SUBSCRIBE 操作无需指定（批量操作无单一实体 ID，resourceId 为 null，
     * afterData 从 ApiResponse.data 提取响应对象）</p>
     */
    String resourceIdParam() default "id";
}
