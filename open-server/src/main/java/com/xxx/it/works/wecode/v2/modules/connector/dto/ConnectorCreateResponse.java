package com.xxx.it.works.wecode.v2.modules.connector.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 创建连接器响应
 * <p>
 * API #1: POST /api/v1/connectors
 * 返回创建的连接器ID (string)
 * </p>
 */
@Data
@Builder
public class ConnectorCreateResponse {

    /** 连接器ID (string格式) */
    private String id;
}