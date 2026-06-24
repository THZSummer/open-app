package com.xxx.it.works.wecode.v2.modules.member.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.enums.OperateEnum;
import com.xxx.it.works.wecode.v2.common.interceptor.AuditDataProvider;
import com.xxx.it.works.wecode.v2.modules.member.dto.AddMemberRequest;
import com.xxx.it.works.wecode.v2.modules.member.enums.MemberTypeEnum;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 添加成员审计数据提供者
 *
 * <p>ADD_APP_MEMBER 是批量操作，方法参数中无成员 resourceId，
 * EntitySnapshotLoader 无法加载快照，由此 Provider 从 AddMemberRequest 提取数据</p>
 *
 * @author SDDU Build Agent
 */
@Component
public class AddMemberAuditDataProvider implements AuditDataProvider {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public OperateEnum supportedOperation() {
        return OperateEnum.ADD_APP_MEMBER;
    }

    @Override
    public String provideAfterData(ProceedingJoinPoint joinPoint, Object result) {
        AddMemberRequest req = findArg(joinPoint, AddMemberRequest.class);
        if (req == null) {
            return null;
        }

        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("memberType", req.getRole());
            data.put("role", MemberTypeEnum.fromCode(req.getRole()) != null
                    ? MemberTypeEnum.fromCode(req.getRole()).getDesc() : String.valueOf(req.getRole()));
            data.put("accountIds", req.getAccountIds());
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Map<String, String> provideTemplateFields(ProceedingJoinPoint joinPoint, Object result) {
        AddMemberRequest req = findArg(joinPoint, AddMemberRequest.class);
        if (req == null) {
            return Map.of();
        }

        Map<String, String> fields = new HashMap<>();
        MemberTypeEnum memberType = MemberTypeEnum.fromCode(req.getRole());
        fields.put("memberType", memberType != null ? memberType.getDesc() : String.valueOf(req.getRole()));
        fields.put("accountId", String.join(",", req.getAccountIds()));
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
