package com.xxx.it.works.wecode.v2.modules.flow.snapshot;

import com.xxx.it.works.wecode.v2.common.snapshot.EntitySnapshotLoader;
import com.xxx.it.works.wecode.v2.modules.flow.entity.Flow;
import com.xxx.it.works.wecode.v2.modules.flow.mapper.OpFlowMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 连接流快照加载器（审计日志）
 *
 * <p>为操作日志 AOP 提供 FLOW 实体的 beforeData/afterData 快照，
 * 使模板占位符 ${nameCn} / ${nameEn} 能正确渲染。</p>
 *
 * <p>注册 operateObject = "FLOW"，覆盖所有 Flow 相关操作：
 * CREATE/UPDATE/DELETE/START/STOP/DEPLOY/COPY。</p>
 *
 * @author SDDU
 */
@Slf4j
@Component
public class FlowSnapshotLoader implements EntitySnapshotLoader {

    @Autowired
    private OpFlowMapper flowMapper;

    @Override
    public List<String> supportedObjects() {
        return List.of("FLOW");
    }

    @Override
    public Object loadById(Long id) {
        Flow flow = flowMapper.selectById(id);
        if (flow == null) {
            return null;
        }

        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", flow.getId());
        snapshot.put("nameCn", flow.getNameCn());
        snapshot.put("nameEn", flow.getNameEn());
        snapshot.put("descriptionCn", flow.getDescriptionCn());
        snapshot.put("descriptionEn", flow.getDescriptionEn());
        snapshot.put("lifecycleStatus", flow.getLifecycleStatus());
        snapshot.put("deployedVersionId", flow.getDeployedVersionId());
        snapshot.put("deployedVersionNumber", flow.getDeployedVersionNumber());
        snapshot.put("appId", flow.getAppId());
        snapshot.put("createTime", flow.getCreateTime());
        snapshot.put("lastUpdateTime", flow.getLastUpdateTime());
        snapshot.put("createBy", flow.getCreateBy());
        snapshot.put("lastUpdateBy", flow.getLastUpdateBy());

        return snapshot;
    }
}
