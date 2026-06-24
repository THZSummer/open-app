package com.xxx.it.works.wecode.v2.modules.member.interceptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.xxx.it.works.wecode.v2.common.enums.OperateEnum;
import com.xxx.it.works.wecode.v2.common.interceptor.LogFieldResolver;
import com.xxx.it.works.wecode.v2.modules.member.enums.MemberTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 成员角色占位符解析器：${memberType} → "Owner"/"管理员"/"开发者"
 *
 * @author SDDU Build Agent
 */
@Slf4j
@Component
public class MemberRoleFieldResolver implements LogFieldResolver {

    @Override
    public String placeholderName() {
        return "memberType";
    }

    @Override
    public String resolve(JsonNode before, JsonNode after, OperateEnum op) {
        if (after == null) {
            return "";
        }
        try {
            JsonNode node = after.get("role");
            if (node == null) {
                node = after.get("memberType");
            }
            if (node == null) {
                return "";
            }
            int code = node.asInt();
            MemberTypeEnum e = MemberTypeEnum.fromCode(code);
            return e != null ? e.getDesc() : String.valueOf(code);
        } catch (Exception e) {
            log.warn("[OPERATE_LOG] Failed to resolve member role", e);
            return "";
        }
    }
}
