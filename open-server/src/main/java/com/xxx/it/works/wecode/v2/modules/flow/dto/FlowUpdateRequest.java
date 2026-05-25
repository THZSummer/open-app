package com.xxx.it.works.wecode.v2.modules.flow.dto;

import lombok.Data;

/**
 * 更新连接流基本信息请求
 * <p>
 * API #11: PUT /api/v1/flows/{flowId}
 * </p>
 */
@Data
public class FlowUpdateRequest {

    /** 中文名称 */
    private String nameCn;

    /** 英文名称 */
    private String nameEn;

    /** 中文描述 */
    private String descriptionCn;

    /** 英文描述 */
    private String descriptionEn;

    /** 图标文件ID */
    private String iconFileId;
}