package com.xxx.it.works.wecode.v2.modules.flow.mapper;

import com.xxx.it.works.wecode.v2.modules.flow.entity.ExecutionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 执行记录 Mapper 接口
 *
 * <p>对应表 openplatform_v2_cp_execution_record_t</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface ExecutionRecordMapper {

    /**
     * 插入执行记录
     */
    int insert(ExecutionRecord record);

    /**
     * 根据 ID 查询
     */
    ExecutionRecord selectById(@Param("id") Long id);

    /**
     * 更新执行记录
     */
    int update(ExecutionRecord record);

    /**
     * 删除执行记录
     */
    int deleteById(@Param("id") Long id);

    /**
     * 按连接流ID + 应用ID + 状态 分页查询
     */
    List<ExecutionRecord> selectList(
            @Param("flowId") Long flowId,
            @Param("appId") Long appId,
            @Param("status") Integer status,
            @Param("triggerType") Integer triggerType,
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize
    );

    /**
     * 统计数量
     */
    Long countList(
            @Param("flowId") Long flowId,
            @Param("appId") Long appId,
            @Param("status") Integer status,
            @Param("triggerType") Integer triggerType,
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime
    );

    /**
     * 按连接流ID统计记录数（用于 FIFO 清理）
     */
    Long countByFlowId(@Param("flowId") Long flowId);

    /**
     * 按时间范围删除过期记录（用于 30 天定时清理）
     */
    int deleteByTriggerTimeBefore(@Param("beforeTime") Date beforeTime, @Param("limit") int limit);

    /**
     * 按创建时间升序批量删除最早记录（用于 FIFO 清理）
     */
    int deleteOldestByFlowId(@Param("flowId") Long flowId, @Param("limit") int limit);
}
