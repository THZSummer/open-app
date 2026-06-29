package com.xxx.it.works.wecode.v2.modules.connectorversion.mapper;

import com.xxx.it.works.wecode.v2.modules.connectorversion.entity.ConnectorVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 连接器版本/配置 Mapper 接口
 * <p>
 * V3 多版本模型: 每 connector 可有多条记录
 * </p>
 */
@Mapper
public interface OpConnectorVersionMapper {

    /**
     * 插入连接器版本
     */
    int insert(ConnectorVersion connectorVersion);

    /**
     * 根据 ID 查询
     */
    ConnectorVersion selectById(@Param("id") Long id);

    /**
     * 按连接器ID查询最新版本配置（兼容 V1）
     */
    ConnectorVersion selectByConnectorId(@Param("connectorId") Long connectorId);

    /**
     * 按连接器ID查询所有版本
     */
    List<ConnectorVersion> selectListByConnectorId(
            @Param("connectorId") Long connectorId,
            @Param("status") Integer status
    );

    /**
     * 按连接器ID和版本号查询
     */
    ConnectorVersion selectByConnectorIdAndVersionNumber(
            @Param("connectorId") Long connectorId,
            @Param("versionNumber") Integer versionNumber
    );

    /**
     * 查询连接器最大版本号
     */
    Integer selectMaxVersionNumberByConnectorId(@Param("connectorId") Long connectorId);

    /**
     * 更新连接器版本配置
     */
    int update(ConnectorVersion connectorVersion);

    /**
     * 根据 ID 删除
     */
    int deleteById(@Param("id") Long id);

    /**
     * 删除连接器的所有版本
     */
    int deleteByConnectorId(@Param("connectorId") Long connectorId);
}
