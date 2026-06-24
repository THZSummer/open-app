package com.xxx.it.works.wecode.v2.modules.member.interceptor;

import com.xxx.it.works.wecode.v2.common.enums.OperateEnum;
import com.xxx.it.works.wecode.v2.common.interceptor.AuditDataProvider;
import com.xxx.it.works.wecode.v2.modules.member.dto.TransferOwnerRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 转移 Owner 审计数据提供者
 *
 * <p>TRANSFER_APP_OWNER 涉及两条记录变更（删除原 Owner + 新增新 Owner），
 * 不是单一实体的更新，EntitySnapshotLoader 无法加载快照，
 * 由此 Provider 从 TransferOwnerRequest 提取目标账号信息</p>
 *
 * @author SDDU Build Agent
 */
@Component
public class TransferOwnerAuditDataProvider implements AuditDataProvider {

    @Override
    public OperateEnum supportedOperation() {
        return OperateEnum.TRANSFER_APP_OWNER;
    }

    @Override
    public Map<String, String> provideTemplateFields(ProceedingJoinPoint joinPoint, Object result) {
        TransferOwnerRequest req = findArg(joinPoint, TransferOwnerRequest.class);
        if (req == null) {
            return Map.of();
        }

        Map<String, String> fields = new HashMap<>();
        fields.put("accountId", req.getToAccountId());
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
