package com.xxx.it.works.wecode.v2.modules.version.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.enums.OperateEnum;
import com.xxx.it.works.wecode.v2.common.interceptor.AuditDataProvider;
import com.xxx.it.works.wecode.v2.modules.version.dto.CreateVersionRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 创建版本审计数据提供者
 *
 * <p>CREATE_APP_VERSION 方法参数中无版本 resourceId（新版本 ID 在 Service 层生成），
 * EntitySnapshotLoader 无法加载快照，由此 Provider 从 CreateVersionRequest 提取版本号</p>
 *
 * @author SDDU Build Agent
 */
@Component
public class CreateVersionAuditDataProvider implements AuditDataProvider {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public OperateEnum supportedOperation() {
        return OperateEnum.CREATE_APP_VERSION;
    }

    @Override
    public String provideAfterData(ProceedingJoinPoint joinPoint, Object result) {
        CreateVersionRequest req = findArg(joinPoint, CreateVersionRequest.class);
        if (req == null) {
            return null;
        }

        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("versionCode", req.getVersionCode());
            data.put("versionDescCn", req.getVersionDescCn());
            data.put("versionDescEn", req.getVersionDescEn());
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Map<String, String> provideTemplateFields(ProceedingJoinPoint joinPoint, Object result) {
        CreateVersionRequest req = findArg(joinPoint, CreateVersionRequest.class);
        if (req == null) {
            return Map.of();
        }

        Map<String, String> fields = new HashMap<>();
        fields.put("versionCode", req.getVersionCode());
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
