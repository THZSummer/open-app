package com.xxx.it.works.wecode.v2.modules.flow.repository;

import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * 连接流基本信息读取 Repository (R2DBC)
 * <p>
 * connector-api 只读, 写入由 open-server 通过 MyBatis 完成
 * V3: 新增 deployed_version_id / deployed_version_number / app_id 字段
 * </p>
 */
@Repository
public interface OpFlowReadRepository extends ReactiveCrudRepository<FlowEntity, Long> {
}
