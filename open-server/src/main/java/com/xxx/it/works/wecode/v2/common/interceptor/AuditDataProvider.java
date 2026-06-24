package com.xxx.it.works.wecode.v2.common.interceptor;

import com.xxx.it.works.wecode.v2.common.enums.OperateEnum;
import org.aspectj.lang.ProceedingJoinPoint;

import java.util.Map;

/**
 * 审计数据提供者（SPI 扩展点）
 *
 * <p>当切面默认的 EntitySnapshotLoader 无法为某个操作加载快照时
 * （如批量操作无单一 resourceId、CREATE 后 resourceId 在返回值中、
 * 复合操作涉及多实体变更），委托此接口提供 beforeData / afterData / 模板占位符值。</p>
 *
 * <p>一个实现类对应一个 OperateEnum，与 {@link OperateEnum#diffConfig()} 粒度一致。</p>
 *
 * <p>优先级：EntitySnapshotLoader 成功 → 使用；失败/无 resourceId → 回退 AuditDataProvider</p>
 *
 * <p>扩展方式：新增操作时只需添加实现类 + OperateEnum 枚举值，切面和已有代码无需修改。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
public interface AuditDataProvider {

    /**
     * 该 Provider 支持的操作枚举（一对一）
     */
    OperateEnum supportedOperation();

    /**
     * 提供操作前数据（Phase 1，proceed 之前调用）
     *
     * <p>适用于 DELETE / UPDATE 操作，在业务方法执行前捕获实体状态</p>
     *
     * @param joinPoint 连接点，可从中提取方法参数
     * @return beforeData JSON 字符串，null 表示无数据（回退到 EntitySnapshotLoader）
     */
    default String provideBeforeData(ProceedingJoinPoint joinPoint) {
        return null;
    }

    /**
     * 提供操作后数据（Phase 3，proceed 之后调用）
     *
     * <p>适用于 CREATE / UPDATE 操作，在业务方法执行后构建结果快照</p>
     *
     * @param joinPoint 连接点
     * @param result    目标方法返回值
     * @return afterData JSON 字符串，null 表示无数据（回退到 EntitySnapshotLoader）
     */
    default String provideAfterData(ProceedingJoinPoint joinPoint, Object result) {
        return null;
    }

    /**
     * 提供模板占位符值（Phase 3.5，模板渲染时调用）
     *
     * <p>当 beforeData / afterData 均为 null 或从中无法解析 ${xxx} 时，
     * 委托此方法从方法参数中直接提取</p>
     *
     * <p>优先级：LogFieldResolver > 通用 JSON 字段提取 > AuditDataProvider.provideTemplateFields</p>
     *
     * @param joinPoint 连接点
     * @param result    目标方法返回值
     * @return 占位符名 → 展示文本值 的映射
     */
    default Map<String, String> provideTemplateFields(ProceedingJoinPoint joinPoint, Object result) {
        return Map.of();
    }
}
