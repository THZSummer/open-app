package com.xxx.it.works.wecode.v2.modules.flow.mapper;

import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 连接流版本/配置 Mapper 接口
 * <p>
 * MVP 单版本模型: 每 flow 仅一条记录, 编辑即生效
 * </p>
 */
@Mapper
public interface OpFlowVersionMapper {

    /**
     * 插入连接流版本
     */
    int insert(FlowVersion flowVersion);

    /**
     * 按连接流ID查询最新版本配置
     */
    FlowVersion selectByFlowId(@Param("flowId") Long flowId);

    /**
     * 更新连接流版本配置 (全文替换 orchestration_config)
     */
    int update(FlowVersion flowVersion);

    /**
     * 删除连接流的所有版本
     */
    int deleteByFlowId(@Param("flowId") Long flowId);
}