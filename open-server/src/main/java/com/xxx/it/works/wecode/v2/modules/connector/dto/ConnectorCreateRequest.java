package com.xxx.it.works.wecode.v2.modules.connector.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建连接器请求（V3 应用隔离版本）
 * <p>
 * API #1: POST /service/open/v2/connectors
 * V3 变更：移除 iconFileId，创建时不自动生成草稿版本
 * </p>
 */
@Data
public class ConnectorCreateRequest {

    /** 中文名称，最长 128 字符 */
    @NotBlank(message = "中文名称不能为空")
    @Size(max = 128, message = "中文名称长度不能超过128")
    private String nameCn;

    /** 英文名称，最长 128 字符 */
    @NotBlank(message = "英文名称不能为空")
    @Size(max = 128, message = "英文名称长度不能超过128")
    private String nameEn;

    /** 中文描述，最长 512 字符 */
    @Size(max = 512, message = "中文描述长度不能超过512")
    private String descriptionCn;

    /** 英文描述，最长 512 字符 */
    @Size(max = 512, message = "英文描述长度不能超过512")
    private String descriptionEn;

    /** 协议类型，固定传 1（HTTP） */
    @NotNull(message = "连接器类型不能为空")
    private Integer connectorType;
}