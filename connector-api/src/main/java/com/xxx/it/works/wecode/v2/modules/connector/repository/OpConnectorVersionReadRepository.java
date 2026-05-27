package com.xxx.it.works.wecode.v2.modules.connector.repository;

import com.xxx.it.works.wecode.v2.modules.connector.entity.ConnectorVersionEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * 连接器版本读取 Repository (R2DBC)
 * <p>
 * 按 connector_id 查询连接配置 (connection_config JSON)
 * connector-api 只读, 写入由 open-server 通过 MyBatis 完成
 * </p>
 */
@Repository
public interface OpConnectorVersionReadRepository extends ReactiveCrudRepository<ConnectorVersionEntity, Long> {

    /**
     * 按连接器ID查询最新版本配置
     * MVP 单版本模型: 每 connector 仅一条记录
     */
    @Query("SELECT * FROM openplatform_v2_cp_connector_version_t WHERE connector_id = :connectorId LIMIT 1")
    Mono<ConnectorVersionEntity> findByConnectorId(Long connectorId);
}