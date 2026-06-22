package com.xxx.it.works.wecode.v2.modules.execution.mapper;

import com.xxx.it.works.wecode.v2.modules.execution.entity.ExecutionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

/**
 * 执行记录 Mapper 接口（connector-api 运行时写入侧）
 *
 * <p>对应表 openplatform_v2_cp_execution_record_t</p>
 * <p>connector-api 运行时直接写入执行记录表，与 open-server 查询侧共享同一张表</p>
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
     * 更新执行记录（状态、耗时、错误信息）
     */
    int update(ExecutionRecord record);

    /**
     * 按连接流ID统计记录数（用于 FIFO 清理）
     */
    Long countByFlowId(@Param("flowId") Long flowId);

    /**
     * 按创建时间升序批量删除最早记录（FIFO 清理）
     */
    int deleteOldestByFlowId(@Param("flowId") Long flowId, @Param("limit") int limit);

    /**
     * 按时间范围删除过期记录（30天定时清理）
     */
    int deleteByTriggerTimeBefore(@Param("beforeTime") Date beforeTime, @Param("limit") int limit);
}
