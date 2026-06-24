package com.xxx.it.works.wecode.v2.modules.connector.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 连接器版本复制到草稿响应（V3 新增）
 * <p>
 * API #13: POST /service/open/v2/connectors/{connectorId}/versions/{versionId}/copy-to-draft
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
public class ConnectorCopyResponse {

    /** 新草稿版本 ID（string） */
    private String versionId;

    /** 新版本号 */
    private Integer versionNumber;

    /** 固定 1（草稿） */
    private Integer status;

    /** 源版本号 */
    private Integer sourceVersionNumber;

    /** 操作说明（覆盖已有草稿时提示） */
    private String message;

    public ConnectorCopyResponse(String versionId, Integer versionNumber, Integer status, Integer sourceVersionNumber, String message) {
        this.versionId = versionId;
        this.versionNumber = versionNumber;
        this.status = status;
        this.sourceVersionNumber = sourceVersionNumber;
        this.message = message;
    }
}
