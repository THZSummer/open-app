package com.xxx.it.works.wecode.v2.common.config;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * R2DBC repository for {@link OpenplatformPropertyEntity}.
 *
 * <p>Reads platform configuration from {@code openplatform_property_t} table.
 * Write operations are handled by open-server; this module is read-only.</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Repository
public interface OpenplatformPropertyRepository extends ReactiveCrudRepository<OpenplatformPropertyEntity, Long> {

    /**
     * Look up a property value by path + code, returning only active records.
     *
     * @param path the property path (e.g. {@code connector_platform})
     * @param code the property code (e.g. {@code log_collection_enabled})
     * @return the matching property entity, or empty Mono if not found
     */
    @Query("SELECT * FROM openplatform_property_t WHERE path = :path AND code = :code AND status = 1 LIMIT 1")
    Mono<OpenplatformPropertyEntity> findByPathAndCodeAndStatus(String path, String code);
}
