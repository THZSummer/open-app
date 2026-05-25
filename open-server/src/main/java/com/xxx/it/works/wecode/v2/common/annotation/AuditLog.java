package com.xxx.it.works.wecode.v2.common.annotation;

import com.xxx.it.works.wecode.v2.common.enums.AppIdSourceEnum;
import com.xxx.it.works.wecode.v2.common.enums.OperateObjectEnum;
import com.xxx.it.works.wecode.v2.common.enums.OperateTypeEnum;

import java.lang.annotation.*;

/**
 * 审计日志注解
 *
 * <p>标记在 Controller 方法上，AOP 切面自动捕获操作前后实体快照、用户信息和 IP 地址，
 * 异步写入 openplateform_operate_log_t 表</p>
 *
 * <p>使用示例：</p>
 * <pre>
 * &#64;AuditLog(operateType = OperateTypeEnum.CREATE,
 *           operateObject = OperateObjectEnum.API,
 *           descCn = "注册API",
 *           descEn = "Register API")
 * </pre>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /**
     * 操作类型
     */
    OperateTypeEnum operateType();

    /**
     * 操作对象
     */
    OperateObjectEnum operateObject();

    /**
     * 中文描述（写入 operate_desc_cn 字段）
     */
    String descCn() default "";

    /**
     * 英文描述（写入 operate_desc_en 字段）
     */
    String descEn() default "";

    /**
     * app_id 取值方式
     * <ul>
     *   <li>PLATFORM：固定值 "platform"（资源管理接口）</li>
     *   <li>PATH_VARIABLE：从方法参数 appId 获取（权限订阅接口）</li>
     * </ul>
     */
    AppIdSourceEnum appIdSource() default AppIdSourceEnum.PLATFORM;

    /**
     * 资源 ID 参数名
     *
     * <p>用于从方法参数中提取资源 ID，以加载实体快照（before_data / after_data）</p>
     * <p>默认 "id" 匹配现有 Controller 的 @PathVariable String id</p>
     * <p>CREATE 操作忽略此参数（实体尚未存在）</p>
     */
    String resourceIdParam() default "id";
}
