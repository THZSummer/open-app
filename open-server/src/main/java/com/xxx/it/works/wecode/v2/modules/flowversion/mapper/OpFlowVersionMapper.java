package com.xxx.it.works.wecode.v2.modules.flowversion.mapper;

import com.xxx.it.works.wecode.v2.modules.flowversion.entity.FlowVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 连接流版本/配置 Mapper 接口
 * <p>
 * V3 多版本模型: 每 flow 可有多条记录
 * </p>
 */
@Mapper
public interface OpFlowVersionMapper {

    /**
     * 插入连接流版本
     */
    int insert(FlowVersion flowVersion);

    /**
     * 根据 ID 查询
     */
    FlowVersion selectById(@Param("id") Long id);

    /**
     * 按连接流ID查询最新版本配置（兼容 V1）
     */
    FlowVersion selectByFlowId(@Param("flowId") Long flowId);

    /**
     * 按连接流ID查询所有版本
     */
    List<FlowVersion> selectListByFlowId(
            @Param("flowId") Long flowId,
            @Param("status") Integer status
    );

    /**
     * 按连接流ID和版本号查询
     */
    FlowVersion selectByFlowIdAndVersionNumber(
            @Param("flowId") Long flowId,
            @Param("versionNumber") Integer versionNumber
    );

    /**
     * 查询连接流最大版本号
     */
    Integer selectMaxVersionNumberByFlowId(@Param("flowId") Long flowId);

    /**
     * 更新连接流版本配置
     */
    int update(FlowVersion flowVersion);

    /**
     * 根据 ID 删除
     */
    int deleteById(@Param("id") Long id);

    /**
     * 删除连接流的所有版本
     */
    int deleteByFlowId(@Param("flowId") Long flowId);
}
