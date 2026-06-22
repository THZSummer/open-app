package com.xxx.it.works.wecode.v2.modules.flow.dto;

import lombok.Data;

import java.util.Date;

/**
 * 连接流列表响应项
 * <p>
 * API #18: GET /service/open/v2/flows
 * V3 新增: deployedVersionId, deployedVersionNumber, appId, draftVersionNumber
 * </p>
 */
@Data
public class FlowListResponse {

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

    /** 生命周期状态 (TINYINT数字): 1=已停止, 2=运行中, 3=已失效 */
    private Integer lifecycleStatus;

    /** 已部署版本ID（String格式，V3 新增） */
    private String deployedVersionId;

    /** 已部署版本号（V3 新增） */
    private Integer deployedVersionNumber;

    /** 归属应用ID（String格式，V3 新增） */
    private String appId;

    /** 草稿版本号（V3 新增） */
    private Integer draftVersionNumber;

    /** 创建时间 */
    private Date createTime;

    /** 最后更新时间 */
    private Date lastUpdateTime;
}