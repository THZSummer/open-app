package com.xxx.it.works.wecode.v2.modules.flow.dto;

import lombok.Data;

/**
 * 连接流版本详情响应
 * <p>
 * API #30: GET /service/open/v2/flows/{flowId}/versions/{versionId}
 * 含编排配置快照
 * </p>
 */
@Data
public class FlowVersionDetailResponse {

    /** 版本ID（String 格式） */
    private String versionId;

    /** 连接流ID（String 格式） */
    private String flowId;

    /** 版本号 */
    private Integer versionNumber;

    /** 版本状态 */
    private Integer status;

    /** 编排配置 JSON 快照 */
    private String orchestrationConfig;

    /** 发布时间 */
    private String publishedTime;

    /** 发布人 */
    private String publishedBy;

    /** 创建时间 */
    private String createTime;

    /** 创建人 */
    private String createBy;

    /** 最后更新时间 */
    private String lastUpdateTime;

    /** 最后更新人 */
    private String lastUpdateBy;
}
