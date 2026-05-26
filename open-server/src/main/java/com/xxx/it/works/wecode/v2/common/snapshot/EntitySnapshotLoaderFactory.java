package com.xxx.it.works.wecode.v2.common.snapshot;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 实体快照加载器工厂
 *
 * <p>自动收集所有 {@link EntitySnapshotLoader} 实现，根据 operateObject 路由到对应 Loader。</p>
 *
 * <p>扩展方式：新增 Loader 实现后，工厂自动注册，无需修改此类。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EntitySnapshotLoaderFactory {

    private final List<EntitySnapshotLoader> loaders;
    private final Map<String, EntitySnapshotLoader> loaderMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (EntitySnapshotLoader loader : loaders) {
            for (String obj : loader.supportedObjects()) {
                loaderMap.put(obj, loader);
                log.debug("[SNAPSHOT] Registered loader: {} -> {}", obj, loader.getClass().getSimpleName());
            }
        }
        log.info("[SNAPSHOT] Initialized {} snapshot loaders for {} operateObjects",
                loaders.size(), loaderMap.size());
    }

    /**
     * 根据 operateObject 获取对应的快照加载器
     *
     * @param operateObject 操作对象标识（如 API_PERMISSION）
     * @return 对应的 Loader，未注册返回 null
     */
    public EntitySnapshotLoader getLoader(String operateObject) {
        return loaderMap.get(operateObject);
    }
}
