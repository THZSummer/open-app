package com.xxx.it.works.wecode.v2.modules.connector.snapshot;

import com.xxx.it.works.wecode.v2.common.snapshot.EntitySnapshotLoader;
import com.xxx.it.works.wecode.v2.modules.connector.entity.Connector;
import com.xxx.it.works.wecode.v2.modules.connector.mapper.OpConnectorMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 连接器快照加载器（审计日志）
 *
 * <p>为操作日志 AOP 提供 CONNECTOR 实体的 beforeData/afterData 快照，
 * 使模板占位符 ${nameCn} / ${nameEn} 能正确渲染。</p>
 *
 * <p>注册 operateObject = "CONNECTOR"，覆盖所有 Connector 相关操作：
 * CREATE/UPDATE/DELETE/PUBLISH。</p>
 *
 * @author SDDU
 */
@Slf4j
@Component
public class ConnectorSnapshotLoader implements EntitySnapshotLoader {

    @Autowired
    private OpConnectorMapper connectorMapper;

    @Override
    public List<String> supportedObjects() {
        return List.of("CONNECTOR");
    }

    @Override
    public Object loadById(Long id) {
        Connector connector = connectorMapper.selectById(id);
        if (connector == null) {
            return null;
        }

        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", connector.getId());
        snapshot.put("nameCn", connector.getNameCn());
        snapshot.put("nameEn", connector.getNameEn());
        snapshot.put("descriptionCn", connector.getDescriptionCn());
        snapshot.put("descriptionEn", connector.getDescriptionEn());
        snapshot.put("connectorType", connector.getConnectorType());
        snapshot.put("status", connector.getStatus());
        snapshot.put("appId", connector.getAppId());
        snapshot.put("createTime", connector.getCreateTime());
        snapshot.put("lastUpdateTime", connector.getLastUpdateTime());
        snapshot.put("createBy", connector.getCreateBy());
        snapshot.put("lastUpdateBy", connector.getLastUpdateBy());

        return snapshot;
    }
}
