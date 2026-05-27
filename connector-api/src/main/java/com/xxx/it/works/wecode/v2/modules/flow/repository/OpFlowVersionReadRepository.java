package com.xxx.it.works.wecode.v2.modules.flow.repository;

import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * 连接流版本读取 Repository (R2DBC)
 * <p>
 * 按 flow_id 查询编排配置 (orchestration_config JSON)
 * connector-api 只读, 写入由 open-server 通过 MyBatis 完成
 * </p>
 */
@Repository
public interface OpFlowVersionReadRepository extends ReactiveCrudRepository<FlowVersionEntity, Long> {

    /**
     * 按连接流ID查询最新版本配置
     * MVP 单版本模型: 每 flow 仅一条记录
     */
    @Query("SELECT * FROM openplatform_v2_cp_flow_version_t WHERE flow_id = :flowId LIMIT 1")
    Mono<FlowVersionEntity> findByFlowId(Long flowId);
}