package com.xxx.it.works.wecode.v2.modules.connector.mapper;

import com.xxx.it.works.wecode.v2.modules.connector.entity.ConnectorVersionRef;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 连接器版本引用中间表 Mapper 接口
 *
 * <p>管理编排中 connector 节点对 ConnectorVersion 的引用关系</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface ConnectorVersionRefMapper {

    /**
     * 插入引用关系
     */
    int insert(ConnectorVersionRef ref);

    /**
     * 批量插入引用关系
     */
    int insertBatch(@Param("list") List<ConnectorVersionRef> refs);

    /**
     * 根据 ID 删除
     */
    int deleteById(@Param("id") Long id);

    /**
     * 按连接流版本ID删除所有引用
     */
    int deleteByFlowVersionId(@Param("flowVersionId") Long flowVersionId);

    /**
     * 按连接流版本ID和节点ID查询引用
     */
    ConnectorVersionRef selectByFlowVersionAndNode(
            @Param("flowVersionId") Long flowVersionId,
            @Param("nodeId") String nodeId
    );

    /**
     * 按连接流版本ID查询所有引用
     */
    List<ConnectorVersionRef> selectByFlowVersionId(@Param("flowVersionId") Long flowVersionId);

    /**
     * 按连接流ID查询所有引用
     */
    List<ConnectorVersionRef> selectByFlowId(@Param("flowId") Long flowId);

    /**
     * 按连接器版本ID查询引用（用于校验版本是否被引用）
     */
    List<ConnectorVersionRef> selectByConnectorVersionId(@Param("connectorVersionId") Long connectorVersionId);

    /**
     * 按连接器ID查询引用（用于校验连接器是否被引用）
     */
    List<ConnectorVersionRef> selectByConnectorId(@Param("connectorId") Long connectorId);

    /**
     * 按连接器版本ID + 连接流ID + 连接流版本ID 查询引用
     */
    List<ConnectorVersionRef> selectByConnectorVersionAndFlow(
            @Param("connectorVersionId") Long connectorVersionId,
            @Param("flowId") Long flowId,
            @Param("flowVersionId") Long flowVersionId
    );
}
