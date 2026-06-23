package com.xxx.it.works.wecode.v2.modules.flow.mapper;

import com.xxx.it.works.wecode.v2.modules.flow.entity.ExecutionStep;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 执行步骤详情 Mapper 接口
 *
 * <p>对应表 openplatform_v2_cp_execution_step_t</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface ExecutionStepMapper {

    /**
     * 插入步骤记录
     */
    int insert(ExecutionStep step);

    /**
     * 批量插入步骤记录
     */
    int insertBatch(@Param("list") List<ExecutionStep> steps);

    /**
     * 根据 ID 查询
     */
    ExecutionStep selectById(@Param("id") Long id);

    /**
     * 按执行记录ID查询全部步骤
     */
    List<ExecutionStep> selectByExecutionId(@Param("executionId") Long executionId);

    /**
     * 按执行记录ID删除全部步骤
     */
    int deleteByExecutionId(@Param("executionId") Long executionId);

    /**
     * 按执行记录ID列表批量删除步骤
     */
    int deleteByExecutionIds(@Param("executionIds") List<Long> executionIds);
}
