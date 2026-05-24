package com.xxx.it.works.wecode.v2.modules.connector.mapper;

import com.xxx.it.works.wecode.v2.modules.connector.entity.Connector;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 连接器 Mapper 接口
 * <p>
 * 连接器基本信息 CRUD
 * </p>
 */
@Mapper
public interface ConnectorMapper {

    /**
     * 插入连接器
     */
    int insert(Connector connector);

    /**
     * 根据 ID 查询连接器
     */
    Connector selectById(@Param("id") Long id);

    /**
     * 更新连接器基本信息
     */
    int update(Connector connector);

    /**
     * 删除连接器
     */
    int deleteById(@Param("id") Long id);

    /**
     * 分页查询连接器列表
     */
    List<Connector> selectList(
            @Param("connectorType") Integer connectorType,
            @Param("keyword") String keyword,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize
    );

    /**
     * 统计连接器总数
     */
    Long countList(
            @Param("connectorType") Integer connectorType,
            @Param("keyword") String keyword
    );

    /**
     * 查询连接器是否被任何连接流引用
     * 通过检查 flow_version_t.orchestration_config JSON 中是否包含该 connectorId
     */
    Long countFlowReferences(@Param("connectorId") Long connectorId);
}