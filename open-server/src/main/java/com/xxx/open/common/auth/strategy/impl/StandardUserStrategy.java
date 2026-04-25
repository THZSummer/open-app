package com.xxx.open.common.auth.strategy.impl;

import com.xxx.open.common.auth.strategy.UserResolveStrategy;
import com.xxx.open.common.model.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 标准环境用户解析策略
 * 
 * <p>非开发环境（测试、预发布、生产等）的默认用户解析策略</p>
 * <p>待环境认证方式确定后补充具体实现</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Component
public class StandardUserStrategy implements UserResolveStrategy {

    @Override
    public UserContext resolve(HttpServletRequest request) {
        // TODO: 标准环境预留实现
        // 可选方案：
        // 1. 从 APIG Header 解析: request.getHeader("X-User-Id")
        // 2. 从 IAM Token 解析: JWT 解析
        // 3. 从 SOA Header 解析: request.getHeader("SVC-USER-ID")
        // 4. 从 Session 解析
        
        log.debug("标准环境用户解析策略暂未实现");
        return null;
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