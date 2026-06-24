package com.xxx.it.works.wecode.v2.common.snapshot;

import com.xxx.it.works.wecode.v2.common.enums.OperateEnum;

import com.xxx.it.works.wecode.v2.common.enums.OperateEnum;

import java.util.List;

/**
 * 实体快照加载器接口
 *
 * <p>每个实现类负责从特定数据表加载实体快照。
 * Spring 自动注册所有实现到 {@link EntitySnapshotLoaderFactory}。</p>
 *
 * <p>扩展方式：新增资源类型时，只需实现此接口并注入对应 Mapper，
 * 切面代码无需修改。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
public interface EntitySnapshotLoader {

    /**
     * 返回该 Loader 支持的 operateObject 列表
     *
     * <p>与 {@link OperateEnum#getOperateObject()} 对应</p>
     *
     * @return operateObject 标识列表
     */
    List<String> supportedObjects();

    /**
     * 根据 ID 加载实体
     *
     * @param id 资源 ID
     * @return 实体对象，未找到返回 null
     */
    Object loadById(Long id);
}
