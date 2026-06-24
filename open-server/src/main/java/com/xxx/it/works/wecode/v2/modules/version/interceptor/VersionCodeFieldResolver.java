package com.xxx.it.works.wecode.v2.modules.version.interceptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.xxx.it.works.wecode.v2.common.enums.OperateEnum;
import com.xxx.it.works.wecode.v2.common.interceptor.LogFieldResolver;
import org.springframework.stereotype.Component;

/**
 * 版本号占位符解析器：${versionCode} → 版本号字符串
 *
 * <p>DELETE 操作从 beforeData 取，其余从 afterData 取</p>
 *
 * @author SDDU Build Agent
 */
@Component
public class VersionCodeFieldResolver implements LogFieldResolver {

    @Override
    public String placeholderName() {
        return "versionCode";
    }

    @Override
    public String resolve(JsonNode before, JsonNode after, OperateEnum op) {
        JsonNode source = "DELETE".equals(op.getOperateType()) ? before : after;
        if (source == null) {
            return "";
        }
        JsonNode field = source.get("versionCode");
        if (field == null || field.isNull()) {
            return "";
        }
        return field.asText();
    }
}
