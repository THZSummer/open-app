package com.xxx.it.works.wecode.v2.common.config;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * R2DBC repository for {@link OpenplatformPropertyEntity}.
 *
 * <p>Reads platform configuration from {@code openplatform_property_t} table.
 * 写操作由 open-server 处理；本模块为只读。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Repository
public interface OpenplatformPropertyRepository extends ReactiveCrudRepository<OpenplatformPropertyEntity, Long> {

    /**
     * 按 path + code 查询属性值，只返回有效记录。
     *
     * @param path 属性路径（如 {@code connector_platform}）
     * @param code 属性编码（如 {@code log_collection_enabled}）
     * @return 匹配的属性实体，未找到时返回 empty Mono
     */
    @Query("SELECT id, path, code, value, status FROM openplatform_property_t WHERE path = :path AND code = :code AND status = 1 LIMIT 1")
    Mono<OpenplatformPropertyEntity> findByPathAndCodeAndStatus(String path, String code);
}
