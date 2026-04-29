package com.xxx.it.works.wecode.v2.modules.app.resolver;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 应用上下文
 *
 * <p>包含解析后的应用信息，用于业务逻辑处理</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
public class AppContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 内部主键ID（用于数据库查询）
     */
    private Long internalId;

    /**
     * 外部业务ID（用于响应返回）
     */
    private String externalId;
}