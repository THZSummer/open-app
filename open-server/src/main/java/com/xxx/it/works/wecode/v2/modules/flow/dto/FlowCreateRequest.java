package com.xxx.it.works.wecode.v2.modules.flow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建连接流请求
 * <p>
 * API #8: POST /service/open/v2/flows
 * 创建后默认 lifecycle_status=1 running
 * </p>
 */
@Data
public class FlowCreateRequest {

    /** 中文名称，最长 128 字符（与 DB VARCHAR(128) 对齐） */
    @NotBlank(message = "中文名称不能为空")
    @Size(max = 128, message = "中文名称长度不能超过128")
    private String nameCn;

    /** 英文名称，最长 128 字符（与 DB VARCHAR(128) 对齐） */
    @NotBlank(message = "英文名称不能为空")
    @Size(max = 128, message = "英文名称长度不能超过128")
    private String nameEn;

    /** 中文描述，最长 512 字符 */
    @Size(max = 512, message = "中文描述长度不能超过512")
    private String descriptionCn;

    /** 英文描述，最长 512 字符 */
    @Size(max = 512, message = "英文描述长度不能超过512")
    private String descriptionEn;

    /** 图标文件ID，最长 128 字符 */
    @Size(max = 128, message = "图标文件ID长度不能超过128")
    private String iconFileId;
}
