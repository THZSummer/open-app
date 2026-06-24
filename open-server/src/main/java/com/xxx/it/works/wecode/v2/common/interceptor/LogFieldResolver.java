package com.xxx.it.works.wecode.v2.common.interceptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.xxx.it.works.wecode.v2.common.enums.OperateEnum;

/**
 * 操作日志占位符解析器
 *
 * <p>用于将审计描述模板中的 ${placeholder} 替换为实际展示文本。
 * 每个业务模块实现此接口，Spring 自动注入到 {@link OperateLogV2Aspect} 中。</p>
 *
 * <p>新增业务字段时只需添加实现类，无需修改切面代码。</p>
 *
 * @author SDDU Build Agent
 */
public interface LogFieldResolver {

    /**
     * 支持的占位符名称（如 "verifyType"、"memberType"）
     */
    String placeholderName();

    /**
     * 解析占位符的值
     *
     * @param before 操作前实体快照（可能为 null）
     * @param after  操作后实体快照（可能为 null）
     * @param op     操作枚举
     * @return 占位符对应的展示文本，不应返回 null
     */
    String resolve(JsonNode before, JsonNode after, OperateEnum op);
}
