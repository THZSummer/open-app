package com.xxx.it.works.wecode.v2.modules.execution.repository;

import com.xxx.it.works.wecode.v2.modules.execution.entity.ExecutionRecordEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * 执行记录 R2DBC Repository（connector-api 运行时写入侧）
 */
@Repository
public interface ExecutionRecordRepository extends ReactiveCrudRepository<ExecutionRecordEntity, Long> {

    /** 部分更新：状态 + 耗时 + 错误信息（避免 save() 覆盖未映射列） */
    @Modifying
    @Query("UPDATE openplatform_v2_cp_execution_record_t SET status = :status, duration_ms = :durationMs, error_code = :errorCode, error_message = :errorMessage, last_update_time = :lastUpdateTime WHERE id = :id")
    Mono<Integer> updateStatus(Long id, Integer status, Integer durationMs, String errorCode, String errorMessage, LocalDateTime lastUpdateTime);

    Mono<Long> countByFlowId(Long flowId);

    /**
     * 补充流元数据字段（在 loadFlowVersion 之后调用）
     */
    @Modifying
    @Query("UPDATE openplatform_v2_cp_execution_record_t SET flow_version_id = :flowVersionId, flow_version_number = :flowVersionNumber, app_id = :appId, flow_name_cn = :flowNameCn, flow_name_en = :flowNameEn, flow_version_snapshot = :flowVersionSnapshot, last_update_time = NOW() WHERE id = :id")
    Mono<Integer> updateFlowMeta(Long id, Long flowVersionId, Integer flowVersionNumber, Long appId, String flowNameCn, String flowNameEn, String flowVersionSnapshot);

    /** FIFO 清理：删除指定流的最早 N 条（子查询绕开 MySQL DELETE ... LIMIT 方言限制） */
    @Modifying
    @Query("DELETE FROM openplatform_v2_cp_execution_record_t WHERE id IN (SELECT id FROM (SELECT id FROM openplatform_v2_cp_execution_record_t WHERE flow_id = :flowId ORDER BY trigger_time ASC LIMIT :limit) AS tmp)")
    Mono<Integer> deleteOldestByFlowId(Long flowId, int limit);
}
