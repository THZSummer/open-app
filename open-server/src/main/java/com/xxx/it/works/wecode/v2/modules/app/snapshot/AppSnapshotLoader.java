package com.xxx.it.works.wecode.v2.modules.app.snapshot;

import com.xxx.it.works.wecode.v2.common.snapshot.EntitySnapshotLoader;
import com.xxx.it.works.wecode.v2.common.util.JsonUtils;
import com.xxx.it.works.wecode.v2.modules.app.constants.AppPropertyConstants;
import com.xxx.it.works.wecode.v2.modules.app.entity.App;
import com.xxx.it.works.wecode.v2.modules.app.entity.AppProperty;
import com.xxx.it.works.wecode.v2.modules.app.mapper.AppMapper;
import com.xxx.it.works.wecode.v2.modules.app.enums.VerifyTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 应用快照加载器（审计日志）
 *
 * <p>loadById() 返回包含 App 实体所有字段 + 扩展属性字段的 Map，
 * 使得审计日志模板中的 ${diffFields}（含"修改示意图"）和 ${verifyType} 占位符能正确渲染</p>
 *
 * <p>扩展字段映射：
 * <ul>
 *   <li>funcImgId ← app_property.diagram_id_list</li>
 *   <li>verifyType ← app_property.verify_type</li>
 *   <li>eamapAppCode ← app_property.eamap_app_code</li>
 * </ul>
 */
@Slf4j
@Component
public class AppSnapshotLoader implements EntitySnapshotLoader {

    @Autowired
    private AppMapper appMapper;

    @Override
    public List<String> supportedObjects() {
        return List.of("APP", "APP_VERIFY_TYPE");
    }

    @Override
    public Object loadById(Long id) {
        App app = appMapper.selectById(id);
        if (app == null) {
            return null;
        }

        Map<String, Object> snapshot = JsonUtils.toMap(app);

        // 从 property 表加载扩展属性（funcImgId、verifyType）
        loadProperties(id, snapshot);

        return snapshot;
    }

    private void loadProperties(Long id, Map<String, Object> snapshot) {
        List<AppProperty> properties = appMapper.selectPropertiesByParentId(id);
        if (CollectionUtils.isEmpty(properties)) {
            return;
        }
        for (AppProperty p : properties) {
            try {
                if (AppPropertyConstants.PROP_DIAGRAM_ID_LIST.equals(p.getPropertyName())) {
                    // 示意图 ID 列表映射为 funcImgId，匹配 OperateLogV2Aspect.APP_FIELD_LABELS
                    snapshot.put("funcImgId", p.getPropertyValue());
                } else if (AppPropertyConstants.PROP_EAMAP_CODE.equals(p.getPropertyName())) {
                    // EAMAP 编码，用于审计日志 BIND_APP_EAMAP 模板占位符 ${eamapAppCode}
                    snapshot.put("eamapAppCode", p.getPropertyValue());
                }
            } catch (Exception e) {
                log.warn("[AppSnapshotLoader] Failed to process property: id={}, name={}", id, p.getPropertyName(), e);
            }
        }

        loadVerifyType(properties, snapshot);
    }

    /**
     * 加载认证方式到快照（verifyType + verifyTypeDesc）
     */
    private void loadVerifyType(List<AppProperty> properties, Map<String, Object> snapshot) {
        try {
            List<Integer> verifyTypeList = AppPropertyConstants.resolveVerifyTypeList(properties);
            if (verifyTypeList.isEmpty()) {
                return;
            }
            snapshot.put("verifyType", verifyTypeList);
            // 描述字段供模板 ${verifyTypeDesc} 使用
            String verifyTypeDesc = verifyTypeList.stream()
                    .map(code -> {
                        VerifyTypeEnum e = VerifyTypeEnum.fromCode(code);
                        return e != null ? e.getName() : String.valueOf(code);
                    })
                    .collect(Collectors.joining("、"));
            snapshot.put("verifyTypeDesc", verifyTypeDesc);
        } catch (Exception e) {
            log.warn("[AppSnapshotLoader] Failed to load verifyType", e);
        }
    }
}
