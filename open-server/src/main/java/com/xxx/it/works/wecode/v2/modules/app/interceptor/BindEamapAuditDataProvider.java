package com.xxx.it.works.wecode.v2.modules.app.interceptor;

import com.xxx.it.works.wecode.v2.common.enums.OperateEnum;
import com.xxx.it.works.wecode.v2.common.interceptor.AuditDataProvider;
import com.xxx.it.works.wecode.v2.modules.app.dto.BindEamapRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 绑定 EAMAP 审计数据提供者
 *
 * <p>BIND_APP_EAMAP 操作的 afterData 可能无法通过 EntitySnapshotLoader 加载
 * （当 resourceId 解析为 null 时，afterData 从返回值 BindEamapVO 提取，仅含 appId），
 * 由此 Provider 直接从方法参数 BindEamapRequest 提取 eamapAppCode，
 * 确保审计日志模板 ${eamapAppCode} 能正确渲染。</p>
 *
 * <p>优先级链路：LogFieldResolver → 通用 JSON 字段提取(afterData) → AuditDataProvider（本类生效）</p>
 *
 * @author SDDU Build Agent
 */
@Component
public class BindEamapAuditDataProvider implements AuditDataProvider {

    @Override
    public OperateEnum supportedOperation() {
        return OperateEnum.BIND_APP_EAMAP;
    }

    @Override
    public Map<String, String> provideTemplateFields(ProceedingJoinPoint joinPoint, Object result) {
        BindEamapRequest req = findArg(joinPoint, BindEamapRequest.class);
        if (req == null || req.getEamapAppCode() == null) {
            return Map.of();
        }

        Map<String, String> fields = new HashMap<>();
        fields.put("eamapAppCode", req.getEamapAppCode());
        return fields;
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
