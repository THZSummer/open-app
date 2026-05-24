package com.xxx.it.works.wecode.v2.modules.connector.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建连接器请求
 * <p>
 * API #1: POST /api/v1/connectors
 * </p>
 */
@Data
public class ConnectorCreateRequest {

    /** 中文名称 */
    @NotBlank(message = "中文名称不能为空")
    private String nameCn;

    /** 英文名称 */
    @NotBlank(message = "英文名称不能为空")
    private String nameEn;

    /** 中文描述 */
    private String descriptionCn;

    /** 英文描述 */
    private String descriptionEn;

    /** 图标文件ID */
    private String iconFileId;

    /** 连接器类型: 1=HTTP (MVP仅支持HTTP) */
    @NotNull(message = "连接器类型不能为空")
    private Integer connectorType;
}