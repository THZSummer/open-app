package com.xxx.it.works.wecode.v2.common.id;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 标准环境 ID 生成器策略
 * 
 * <p>非开发环境（测试、预发布、生产等）的默认 ID 生成策略</p>
 * <p>待环境 ID 生成方式确定后补充具体实现</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Component
public class StandardIdGeneratorStrategy implements IdGeneratorStrategy {

    @Override
    public long nextId() {
        // TODO: 标准环境预留实现
        // 可选方案：
        // 1. 使用分布式 ID 服务（如美团 Leaf、百度 UidGenerator）
        // 2. 使用 Redis 自增 ID
        // 3. 使用数据库序列
        // 4. 调用外部 ID 生成服务
        
        log.debug("标准环境 ID 生成策略暂未实现");
        throw new UnsupportedOperationException("标准环境 ID 生成策略暂未实现，请先配置 ID 生成服务");
    }

    @Override
    public boolean supports(String activeProfile) {
        // 非开发环境的默认策略
        // 支持: test, uat, prod, production 等所有非开发环境
        return !"dev".equals(activeProfile) 
                && !"development".equals(activeProfile)
                && !"local".equals(activeProfile);
    }
}
