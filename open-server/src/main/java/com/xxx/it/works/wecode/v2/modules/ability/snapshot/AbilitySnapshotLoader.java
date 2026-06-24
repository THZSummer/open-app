package com.xxx.it.works.wecode.v2.modules.ability.snapshot;

import com.xxx.it.works.wecode.v2.common.util.CommonUtils;
import com.xxx.it.works.wecode.v2.common.snapshot.EntitySnapshotLoader;
import com.xxx.it.works.wecode.v2.modules.ability.dto.AddAbilityRequest;
import com.xxx.it.works.wecode.v2.modules.ability.enums.AbilityTypeEnum;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AppAbilityRelationMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 应用能力快照加载器
 *
 * <p>ADD_APP_ABILITY 无 resourceId，override loadAfterData 从方法参数提取能力类型描述。</p>
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
    public Object loadAfterData(ProceedingJoinPoint joinPoint, Long resourceId, Object result) {
        // 先尝试从方法参数提取（ADD_APP_ABILITY）
        AddAbilityRequest req = CommonUtils.findArg(joinPoint, AddAbilityRequest.class);
        if (req != null) {
            Map<String, Object> data = new LinkedHashMap<>();
            AbilityTypeEnum abilityType = findAbilityType(req.getAbilityType());
            data.put("abilityTypeDesc", abilityType != null ? abilityType.getDesc() : String.valueOf(req.getAbilityType()));
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
        return abilityRelationMapper.selectById(id);
    }

    private AbilityTypeEnum findAbilityType(Integer code) {
        if (code == null) {
            return null;
        }
        for (AbilityTypeEnum e : AbilityTypeEnum.values()) {
            if (e.getCode() == code) {
                return e;
            }
        }
        return null;
    }
}
