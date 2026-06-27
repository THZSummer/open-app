package com.xxx.it.works.wecode.v2.modules.flowversion.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * 连接流版本保存请求
 * <p>
 * API #31: PUT /service/open/v2/flows/{flowId}/versions/{versionId}
 * 更新草稿仅做 DB 存储级校验，不校验平台限制
 * </p>
 */
@Data
public class FlowVersionSaveRequest {

    /** 编排配置 JSON 字符串（React Flow 格式：nodes + edges + flowConfig） */
    private JsonNode orchestrationConfig;
}
