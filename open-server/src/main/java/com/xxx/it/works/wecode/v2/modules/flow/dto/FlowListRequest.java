package com.xxx.it.works.wecode.v2.modules.flow.dto;

import lombok.Data;

/**
 * 连接流列表查询请求参数
 * <p>
 * API #9: GET /service/open/v2/flows
 * </p>
 */
@Data
public class FlowListRequest {

    /** 生命周期状态过滤 (可选) */
    private Integer lifecycleStatus;

    /** 搜索关键词 (可选, 匹配中英文名称) */
    private String keyword;

    /** 当前页码 (默认1) */
    private Integer curPage = 1;

    /** 每页数量 (默认20) */
    private Integer pageSize = 20;
}