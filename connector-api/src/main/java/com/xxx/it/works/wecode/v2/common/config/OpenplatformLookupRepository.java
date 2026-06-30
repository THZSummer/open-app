package com.xxx.it.works.wecode.v2.common.config;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * R2DBC repository for Lookup items (openplatform_lookup_classify_t + openplatform_lookup_item_t).
 *
 * <p>Reads platform configuration from Lookup tables. Write operations are handled by market-server; this module is read-only.</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Repository
public interface OpenplatformLookupRepository extends ReactiveCrudRepository<LookupItemEntity, Long> {

    /**
     * 按 path + classify_code 联查所有启用的 item_code 和 item_value。
     *
     * @param path         路径（命名空间）
     * @param classifyCode 分类编码
     * @return item_code → item_value 的 Flux
     */
    @Query("SELECT i.item_code, i.item_value " +
           "FROM openplatform_lookup_item_t i " +
           "JOIN openplatform_lookup_classify_t c ON c.classify_id = i.classify_id " +
           "WHERE c.path = :path AND c.classify_code = :classifyCode " +
           "AND c.status = 1 AND i.status = 1 " +
           "ORDER BY i.item_index")
    Flux<LookupItemEntity> findByPathAndClassifyCode(String path, String classifyCode);
}
