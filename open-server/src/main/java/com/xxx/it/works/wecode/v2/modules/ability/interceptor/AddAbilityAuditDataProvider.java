package com.xxx.it.works.wecode.v2.modules.ability.interceptor;

import com.xxx.it.works.wecode.v2.common.enums.OperateEnum;
import com.xxx.it.works.wecode.v2.common.interceptor.AuditDataProvider;
import com.xxx.it.works.wecode.v2.modules.ability.dto.AddAbilityRequest;
import com.xxx.it.works.wecode.v2.modules.ability.enums.AbilityTypeEnum;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 添加能力审计数据提供者
 *
 * <p>ADD_APP_ABILITY 是 CREATE 操作，方法参数中无能力 resourceId，
 * EntitySnapshotLoader 无法加载快照，由此 Provider 从 AddAbilityRequest 提取能力类型描述</p>
 *
 * @author SDDU Build Agent
 */
@Component
public class AddAbilityAuditDataProvider implements AuditDataProvider {

    @Override
    public OperateEnum supportedOperation() {
        return OperateEnum.ADD_APP_ABILITY;
    }

    @Override
    public Map<String, String> provideTemplateFields(ProceedingJoinPoint joinPoint, Object result) {
        AddAbilityRequest req = findArg(joinPoint, AddAbilityRequest.class);
        if (req == null) {
            return Map.of();
        }

        Map<String, String> fields = new HashMap<>();
        AbilityTypeEnum abilityType = AbilityTypeEnum.values().length > 0
                ? findAbilityType(req.getAbilityType()) : null;
        fields.put("abilityTypeDesc", abilityType != null ? abilityType.getDesc() : String.valueOf(req.getAbilityType()));
        return fields;
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

    @SuppressWarnings("unchecked")
    private <T> T findArg(ProceedingJoinPoint joinPoint, Class<T> clazz) {
        for (Object arg : joinPoint.getArgs()) {
            if (clazz.isInstance(arg)) {
                return (T) arg;
            }
        }
        return null;
    }
}
