package com.xxx.it.works.wecode.v2.modules.ability.snapshot;

import com.xxx.it.works.wecode.v2.common.snapshot.EntitySnapshotLoader;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AppAbilityRelationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 应用能力快照加载器（审计日志）
 * 对应 TASK-016
 */
@Component
public class AbilitySnapshotLoader implements EntitySnapshotLoader {

    @Autowired
    private AppAbilityRelationMapper abilityRelationMapper;

    @Override
    public List<String> supportedObjects() {
        return List.of("APP_ABILITY");
    }

    @Override
    public Object loadById(Long id) {
        return abilityRelationMapper.selectById(id);
    }
}
