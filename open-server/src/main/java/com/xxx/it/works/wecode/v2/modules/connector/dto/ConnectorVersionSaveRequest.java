package com.xxx.it.works.wecode.v2.modules.connector.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 连接器版本保存请求（V3 新增）
 * <p>
 * API #11: PUT /service/open/v2/connectors/{connectorId}/versions/{versionId}
 * 仅做 DB 存储级校验（JSON 可解析即可），业务校验推迟到发布时
 * </p>
 */
@Data
public class ConnectorVersionSaveRequest {

    /** 连接配置 JSON 字符串（全文替换） */
    @NotBlank(message = "连接配置不能为空")
    private String connectionConfig;
}
