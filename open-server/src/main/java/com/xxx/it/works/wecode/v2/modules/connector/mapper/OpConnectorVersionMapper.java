package com.xxx.it.works.wecode.v2.modules.connector.mapper;

import com.xxx.it.works.wecode.v2.modules.connector.entity.ConnectorVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 连接器版本/配置 Mapper 接口
 * <p>
 * MVP 单版本模型: 每 connector 仅一条记录, 编辑即生效
 * </p>
 */
@Mapper
public interface OpConnectorVersionMapper {

    /**
     * 插入连接器版本
     */
    int insert(ConnectorVersion connectorVersion);

    /**
     * 按连接器ID查询最新版本配置
     */
    ConnectorVersion selectByConnectorId(@Param("connectorId") Long connectorId);

    /**
     * 更新连接器版本配置 (全文替换 connection_config)
     */
    int update(ConnectorVersion connectorVersion);

    /**
     * 删除连接器的所有版本
     */
    int deleteByConnectorId(@Param("connectorId") Long connectorId);
}