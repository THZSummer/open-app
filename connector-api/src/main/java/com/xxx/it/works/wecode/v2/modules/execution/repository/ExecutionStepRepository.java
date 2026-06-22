package com.xxx.it.works.wecode.v2.modules.execution.repository;

import com.xxx.it.works.wecode.v2.modules.execution.entity.ExecutionStepEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * 执行步骤详情 R2DBC Repository（connector-api 运行时写入侧）
 */
@Repository
public interface ExecutionStepRepository extends ReactiveCrudRepository<ExecutionStepEntity, Long> {
}
