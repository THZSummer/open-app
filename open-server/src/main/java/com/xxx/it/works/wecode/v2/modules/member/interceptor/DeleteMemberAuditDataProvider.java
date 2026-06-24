package com.xxx.it.works.wecode.v2.modules.member.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.enums.OperateEnum;
import com.xxx.it.works.wecode.v2.common.interceptor.AuditDataProvider;
import com.xxx.it.works.wecode.v2.modules.member.entity.AppMember;
import com.xxx.it.works.wecode.v2.modules.member.enums.MemberTypeEnum;
import com.xxx.it.works.wecode.v2.modules.member.mapper.AppMemberMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 删除成员审计数据提供者
 *
 * <p>DELETE_APP_MEMBER 需要在 proceed 前加载成员快照作为 beforeData。
 * 当 EntitySnapshotLoader 加载失败时（如 resourceId 解析异常），由此 Provider 兜底</p>
 *
 * @author SDDU Build Agent
 */
@Component
public class DeleteMemberAuditDataProvider implements AuditDataProvider {

    @Autowired
    private AppMemberMapper memberMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public OperateEnum supportedOperation() {
        return OperateEnum.DELETE_APP_MEMBER;
    }

    @Override
    public String provideBeforeData(ProceedingJoinPoint joinPoint) {
        String idStr = findArgByName(joinPoint, "id");
        if (idStr == null) {
            return null;
        }
        try {
            Long memberId = Long.parseLong(idStr);
            AppMember member = memberMapper.selectById(memberId);
            return member != null ? buildMemberJson(member) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public Map<String, String> provideTemplateFields(ProceedingJoinPoint joinPoint, Object result) {
        // 兜底：beforeData 加载失败时，至少展示成员 ID
        String idStr = findArgByName(joinPoint, "id");
        if (idStr != null) {
            Map<String, String> fields = new HashMap<>();
            fields.put("accountId", idStr);
            return fields;
        }
        return Map.of();
    }

    private String buildMemberJson(AppMember member) {
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("id", member.getId());
            snapshot.put("accountId", member.getAccountId());
            snapshot.put("memberNameCn", member.getMemberNameCn());
            snapshot.put("memberNameEn", member.getMemberNameEn());
            snapshot.put("memberType", member.getMemberType());
            MemberTypeEnum type = MemberTypeEnum.fromCode(member.getMemberType());
            snapshot.put("role", type != null ? type.getDesc() : String.valueOf(member.getMemberType()));
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            return null;
        }
    }

    private String findArgByName(ProceedingJoinPoint joinPoint, String name) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        String[] names = sig.getParameterNames();
        Object[] args = joinPoint.getArgs();
        if (names == null) {
            return null;
        }
        for (int i = 0; i < names.length; i++) {
            if (name.equals(names[i]) && args[i] != null) {
                return args[i].toString();
            }
        }
        return null;
    }
}
