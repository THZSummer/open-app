package com.xxx.open.common.auth.strategy.impl;

import com.xxx.open.common.auth.strategy.UserResolveStrategy;
import com.xxx.open.common.model.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 生产环境用户解析策略
 * 
 * <p>生产环境用户解析策略（预留实现）</p>
 * <p>待生产环境认证方式确定后补充具体实现</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Component
public class ProdUserStrategy implements UserResolveStrategy {

    @Override
    public UserContext resolve(HttpServletRequest request) {
        // TODO: 生产环境预留实现
        // 可选方案：
        // 1. 从 APIG Header 解析: request.getHeader("X-User-Id")
        // 2. 从 IAM Token 解析: JWT 解析
        // 3. 从 SOA Header 解析: request.getHeader("SVC-USER-ID")
        // 4. 从 Session 解析
        
        log.debug("生产环境用户解析策略暂未实现");
        return null;
    }

    @Override
    public boolean supports(String activeProfile) {
        // 生产环境激活: prod, production
        return "prod".equals(activeProfile) 
                || "production".equals(activeProfile);
    }
}