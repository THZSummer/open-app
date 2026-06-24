package com.xxx.it.works.wecode.v2.modules.flow.dto;

import lombok.Data;

import java.util.Date;

/**
 * 连接流详情响应
 * <p>
 * API #19: GET /service/open/v2/flows/{flowId}
 * V3 新增: deployedVersionId, deployedVersionNumber, appId, invokeUrl, latestPublishedVersionNumber, draftVersionNumber
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

    /** 生命周期状态 (TINYINT数字): 1=已停止, 2=运行中, 3=已失效 */
    private Integer lifecycleStatus;

    /** 已部署版本ID（String格式，V3 新增） */
    private String deployedVersionId;

    /** 已部署版本号（V3 新增） */
    private Integer deployedVersionNumber;

    /** 归属应用ID（String格式，V3 新增） */
    private String appId;

    /** 调用入口 URL（V3 新增） */
    private String invokeUrl;

    /** 最新已发布版本号（V3 新增） */
    private Integer latestPublishedVersionNumber;

    /** 草稿版本号（V3 新增） */
    private Integer draftVersionNumber;

    /** 创建时间 */
    private Date createTime;

    /** 最后更新时间 */
    private Date lastUpdateTime;

    /** 创建人 */
    private String createBy;

    /** 最后更新人 */
    private String lastUpdateBy;
}