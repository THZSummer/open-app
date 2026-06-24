package com.xxx.it.works.wecode.v2.modules.app.interceptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.xxx.it.works.wecode.v2.common.enums.OperateEnum;
import com.xxx.it.works.wecode.v2.common.interceptor.LogFieldResolver;
import com.xxx.it.works.wecode.v2.modules.app.constants.AppPropertyConstants;
import com.xxx.it.works.wecode.v2.modules.app.enums.VerifyTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 认证方式占位符解析器：${verifyType} → "Cookie"、"数字签名" 等
 *
 * @author SDDU Build Agent
 */
@Slf4j
@Component
public class VerifyTypeFieldResolver implements LogFieldResolver {

    @Override
    public String placeholderName() {
        return "verifyType";
    }

    @Override
    public String resolve(JsonNode before, JsonNode after, OperateEnum op) {
        if (after == null) {
            return "";
        }
        try {
            JsonNode vtNode = after.get("verifyType");
            if (vtNode == null) {
                vtNode = after.get(AppPropertyConstants.PROP_VERIFY_TYPE);
            }
            if (vtNode == null) {
                return "";
            }

            List<String> names = new ArrayList<>();
            if (vtNode.isArray()) {
                for (JsonNode item : vtNode) {
                    int code = item.asInt();
                    VerifyTypeEnum e = VerifyTypeEnum.fromCode(code);
                    names.add(e != null ? e.getName() : String.valueOf(code));
                }
            } else {
                for (String part : vtNode.asText().split(",")) {
                    try {
                        int code = Integer.parseInt(part.trim());
                        VerifyTypeEnum e = VerifyTypeEnum.fromCode(code);
                        names.add(e != null ? e.getName() : part.trim());
                    } catch (NumberFormatException e) {
                        names.add(part.trim());
                    }
                }
            }
            return String.join("、", names);
        } catch (Exception e) {
            log.warn("[OPERATE_LOG] Failed to resolve verify type name", e);
            return "";
        }
    }
}
