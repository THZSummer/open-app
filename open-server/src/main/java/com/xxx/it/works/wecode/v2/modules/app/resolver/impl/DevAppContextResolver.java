package com.xxx.it.works.wecode.v2.modules.app.resolver.impl;

import com.xxx.it.works.wecode.v2.modules.app.resolver.AppAccessException;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContext;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContextResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 开发环境应用上下文解析器
 * 
 * <p>默认实现，用于开发和测试环境：</p>
 * <ul>
 *   <li>ID 转换：直接将 String 解析为 Long（或原样返回兼容测试）</li>
 *   <li>权限校验：跳过校验，允许所有访问</li>
 * </ul>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Component
public class DevAppContextResolver implements AppContextResolver {

    @Override
    public AppContext resolveAndValidate(String externalAppId) {
        log.debug("开发环境解析应用ID: {}", externalAppId);
        
        // 开发环境：简单解析，不做权限校验
        Long internalId = parseInternalId(externalAppId);
        
        return AppContext.builder()
            .internalId(internalId)
            .externalId(externalAppId)
            .build();
    }

    @Override
    public String toExternalId(Long internalId) {
        // 开发环境：直接返回 String
        return String.valueOf(internalId);
    }
    
    /**
     * 解析内部ID
     * 支持两种格式：
     * 1. 纯数字字符串 "1001" → 1001L
     * 2. 长业务ID，提取末尾数字或使用默认值
     */
    private Long parseInternalId(String externalAppId) {
        if (externalAppId == null || externalAppId.isEmpty()) {
            throw AppAccessException.notFound(externalAppId);
        }
        
        // 尝试直接解析为数字
        try {
            return Long.parseLong(externalAppId);
        } catch (NumberFormatException e) {
            // 开发环境：非数字ID时返回默认值 1L，方便测试
            log.debug("非数字格式的应用ID: {}, 使用默认值 1L", externalAppId);
            return 1L;
        }
    }
}