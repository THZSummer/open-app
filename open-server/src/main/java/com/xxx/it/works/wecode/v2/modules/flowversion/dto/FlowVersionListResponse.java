package com.xxx.it.works.wecode.v2.modules.flowversion.dto;

import lombok.Data;

/**
 * 连接流版本列表响应项
 * <p>
 * API #29: GET /service/open/v2/flows/{flowId}/versions
 * </p>
 */
@Data
public class FlowVersionListResponse {

    /** 版本ID（String 格式） */
    private String versionId;

    /** 版本号 */
    private Integer versionNumber;

    /** 版本状态 */
    private Integer status;

    /** 是否为当前已部署版本 */
    private Boolean deployed;

    /** 发布时间 */
    private String publishedTime;

    /** 发布人 */
    private String publishedBy;

    /** 创建时间 */
    private String createTime;

    /** 创建人 */
    private String createBy;
}
