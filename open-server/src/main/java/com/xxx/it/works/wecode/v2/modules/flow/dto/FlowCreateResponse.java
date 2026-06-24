package com.xxx.it.works.wecode.v2.modules.flow.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 创建连接流响应 (plan-api §3.3 #17)
 * <p>
 * POST /service/open/v2/flows
 * 返回: flowId(string), nameCn, nameEn, lifecycleStatus=1(已停止), appId, createTime, note
 * </p>
 */
@Data
@Builder
public class FlowCreateResponse {

    /** 连接流ID (string格式，雪花ID) */
    private String flowId;

    /** 中文名称 */
    private String nameCn;

    /** 英文名称 */
    private String nameEn;

    /** 生命周期状态：1=已停止 */
    private Integer lifecycleStatus;

    /** 归属应用ID */
    private String appId;

    /** 创建时间 (yyyy-MM-dd HH:mm:ss) */
    private String createTime;

    /** 提示：创建后需手动创建草稿版本 */
    private String note;
}
