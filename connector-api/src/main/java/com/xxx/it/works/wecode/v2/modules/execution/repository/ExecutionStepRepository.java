package com.xxx.it.works.wecode.v2.modules.execution.repository;

import com.xxx.it.works.wecode.v2.modules.execution.entity.ExecutionStepEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 执行步骤详情 R2DBC Repository（connector-api 运行时写入侧）
 *
 * <p>对应表 openplatform_v2_cp_execution_step_t</p>
 *
 * @author SDDU Build Agent
 * @version 2.0.0
 */
@Repository
public interface ExecutionStepRepository extends ReactiveCrudRepository<ExecutionStepEntity, Long> {

    Flux<ExecutionStepEntity> findByExecutionIdOrderByCreateTimeAsc(Long executionId);

    @Modifying
    @Query("DELETE FROM openplatform_v2_cp_execution_step_t WHERE execution_id = :executionId")
    Mono<Integer> deleteByExecutionId(Long executionId);
}
