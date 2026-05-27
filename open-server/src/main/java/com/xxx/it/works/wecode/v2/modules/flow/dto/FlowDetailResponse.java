package com.xxx.it.works.wecode.v2.modules.flow.dto;

import lombok.Data;

/**
 * 连接流详情响应
 * <p>
 * API #10: GET /service/open/v2/flows/{flowId}
 * </p>
 */
@Data
public class FlowDetailResponse {

    /** 连接流ID (string格式返回) */
    private String id;

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

    /** 生命周期状态 (TINYINT数字) */
    private Integer lifecycleStatus;

    /** 创建时间 (ISO 8601) */
    private String createTime;

    /** 最后更新时间 (ISO 8601) */
    private String lastUpdateTime;
}