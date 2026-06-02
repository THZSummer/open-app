package com.xxx.it.works.wecode.v2.modules.connector.dto;

import lombok.Data;

/**
 * 连接器列表查询请求参数
 * <p>
 * API #2: GET /service/open/v2/connectors
 * </p>
 */
@Data
public class ConnectorListRequest {

    /** 连接器类型过滤 (可选) */
    private Integer connectorType;

    /** 搜索关键词 (可选, 匹配中英文名称) */
    private String keyword;

    /** 当前页码 (默认1) */
    private Integer curPage = 1;

    /** 每页数量 (默认20) */
    private Integer pageSize = 20;
}