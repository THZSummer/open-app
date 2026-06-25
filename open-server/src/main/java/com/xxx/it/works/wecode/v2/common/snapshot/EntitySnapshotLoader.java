package com.xxx.it.works.wecode.v2.common.snapshot;

import org.aspectj.lang.ProceedingJoinPoint;

import java.util.List;

/**
 * 实体快照加载器接口
 *
 * <p>每个实现类负责从特定数据表加载实体快照，用于审计日志的 beforeData/afterData。
 * Spring 自动注册所有实现到 {@link EntitySnapshotLoaderFactory}。</p>
 *
 * <p>大部分 Loader 只需实现 {@link #loadById(Long)}，default 方法会自动调用它。
 * 特殊场景（无 resourceId 的 CREATE/批量操作）可 override
 * {@link #loadAfterData(ProceedingJoinPoint, Long, Object)} 从方法参数提取数据。</p>
 *
 * @author SDDU Build Agent
 * @version 2.0.0
 */
public interface EntitySnapshotLoader {

    /**
     * 该 Loader 支持的 operateObject 列表（与 OperateEnum.getOperateObject() 对应）
     */
    List<String> supportedObjects();

    /**
     * 加载操作前快照（UPDATE/DELETE 操作）。
     * 默认走 loadById，有 resourceId 就能自动加载。
     */
    default Object loadBeforeData(ProceedingJoinPoint joinPoint, Long resourceId) {
        return resourceId != null ? loadById(resourceId) : null;
    }

    /**
     * 加载操作后快照（CREATE/UPDATE 操作）。
     * 默认走 loadById，特殊操作（无 resourceId）可 override 从方法参数提取。
     */
    default Object loadAfterData(ProceedingJoinPoint joinPoint, Long resourceId, Object result) {
        return resourceId != null ? loadById(resourceId) : null;
    }

    /**
     * 根据 ID 加载实体（核心方法，必须实现）
     */
    Object loadById(Long id);
}
