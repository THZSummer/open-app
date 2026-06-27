package com.xxx.it.works.wecode.v2.modules.connector.mapper;

import com.xxx.it.works.wecode.v2.modules.connector.entity.Connector;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 连接器 Mapper 接口（v2.0.0）
 * <p>
 * 连接器基本信息 CRUD
 * </p>
 * <p>
 * v2.0.0 变更：selectAll / selectList 增加 appId 参数，SQL 层实现应用数据隔离
 * </p>
 */
@Mapper
public interface OpConnectorMapper {

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
     * 查询连接器列表（v2.0.0: 增加 appId 过滤）
     *
     * @param connectorType 连接器类型过滤（可选）
     * @param keyword       搜索关键词（可选）
     * @param appId         应用内部 ID（v2.0.0 新增，数据库层隔离）
     */
    List<Connector> selectAll(
            @Param("connectorType") Integer connectorType,
            @Param("keyword") String keyword,
            @Param("appId") Long appId
    );

    /**
     * 分页查询连接器列表（v2.0.0: 增加 appId 过滤；v2.1.0: 增加 status 过滤，SQL 层实现状态+应用隔离）
     *
     * @param connectorType 连接器类型过滤（可选）
     * @param keyword       搜索关键词（可选）
     * @param appId         应用内部 ID（v2.0.0 新增，数据库层隔离）
     * @param status        连接器状态过滤（v2.1.0 新增，可选）
     * @param offset        分页偏移量
     * @param pageSize      每页大小
     */
    List<Connector> selectList(
            @Param("connectorType") Integer connectorType,
            @Param("keyword") String keyword,
            @Param("appId") Long appId,
            @Param("status") Integer status,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize
    );

    /**
     * 统计连接器总数（v2.1.0: 增加 appId 和 status 过滤，修复 count 未按应用隔离的 bug）
     *
     * @param connectorType 连接器类型过滤（可选）
     * @param keyword       搜索关键词（可选）
     * @param appId         应用内部 ID（v2.1.0 新增，数据库层隔离）
     * @param status        连接器状态过滤（v2.1.0 新增，可选）
     */
    Long countList(
            @Param("connectorType") Integer connectorType,
            @Param("keyword") String keyword,
            @Param("appId") Long appId,
            @Param("status") Integer status
    );

    /**
     * 查询连接器是否被任何连接流引用
     * 通过检查 flow_version_t.orchestration_config JSON 中是否包含该 connectorId
     */
    Long countFlowReferences(@Param("connectorId") Long connectorId);
}
