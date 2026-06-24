package com.xxx.it.works.wecode.v2.modules.version.snapshot;

import com.xxx.it.works.wecode.v2.common.snapshot.EntitySnapshotLoader;
import com.xxx.it.works.wecode.v2.modules.version.mapper.AppVersionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 版本快照加载器（审计日志）
 * 对应 TASK-016
 */
@Component
public class VersionSnapshotLoader implements EntitySnapshotLoader {

    @Autowired
    private AppVersionMapper versionMapper;

    @Override
    public List<String> supportedObjects() {
        return List.of("APP_VERSION");
    }

    @Override
    public Object loadById(Long id) {
        return versionMapper.selectById(id);
    }
}
