package com.xxx.it.works.wecode.v2.modules.version.snapshot;

import com.xxx.it.works.wecode.v2.common.util.CommonUtils;
import com.xxx.it.works.wecode.v2.common.snapshot.EntitySnapshotLoader;
import com.xxx.it.works.wecode.v2.modules.version.dto.CreateVersionRequest;
import com.xxx.it.works.wecode.v2.modules.version.mapper.AppVersionMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 版本快照加载器
 *
 * <p>标准操作（UPDATE/PUBLISH/WITHDRAW/DELETE）走 loadById 查 DB。
 * CREATE_APP_VERSION 无 resourceId，override loadAfterData 从方法参数提取。</p>
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
    public Object loadAfterData(ProceedingJoinPoint joinPoint, Long resourceId, Object result) {
        // 先尝试从方法参数提取（CREATE_APP_VERSION）
        CreateVersionRequest req = CommonUtils.findArg(joinPoint, CreateVersionRequest.class);
        if (req != null) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("versionCode", req.getVersionCode());
            data.put("versionDescCn", req.getVersionDescCn());
            data.put("versionDescEn", req.getVersionDescEn());
            return data;
        }

        // 标准路径：从 DB 查
        if (resourceId != null) {
            return loadById(resourceId);
        }
        return null;
    }

    @Override
    public Object loadById(Long id) {
        return versionMapper.selectById(id);
    }
}
