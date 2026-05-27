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

    /** 中文名称 */
    @NotBlank(message = "中文名称不能为空")
    @Size(max = 200, message = "中文名称长度不能超过200")
    private String nameCn;

    /** 英文名称 */
    @NotBlank(message = "英文名称不能为空")
    @Size(max = 200, message = "英文名称长度不能超过200")
    private String nameEn;

    /** 中文描述 */
    private String descriptionCn;

    /** 英文描述 */
    private String descriptionEn;

    /** 图标文件ID */
    private String iconFileId;
}