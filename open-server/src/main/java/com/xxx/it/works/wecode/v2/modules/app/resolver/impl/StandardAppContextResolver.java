package com.xxx.it.works.wecode.v2.modules.app.resolver.impl;

import com.xxx.it.works.wecode.v2.modules.app.resolver.AppAccessException;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContext;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContextResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 标准环境应用上下文解析器
 * 
 * <p>生产环境实现：</p>
 * <ul>
 *   <li>ID 转换：调用应用管理服务获取映射关系</li>
 *   <li>权限校验：校验当前用户对该应用的访问权限</li>
 * </ul>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.resolver.type", havingValue = "standard")
public class StandardAppContextResolver implements AppContextResolver {

    // TODO: 注入应用管理服务
    // @Autowired
    // private AppManageService appManageService;
    
    @Override
    public AppContext resolveAndValidate(String externalAppId) {
        log.info("标准环境解析应用ID: {}", externalAppId);
        
        // 1. 校验参数
        if (externalAppId == null || externalAppId.isEmpty()) {
            throw AppAccessException.notFound(externalAppId);
        }
        
        // TODO: 标准环境实现，对接应用管理服务
        // 2. 调用应用管理服务获取内部ID
        // Long internalId = appManageService.getInternalIdByExternalId(externalAppId);
        // if (internalId == null) {
        //     throw AppAccessException.notFound(externalAppId);
        // }
        
        // 3. 校验当前用户对该应用的访问权限
        // String currentUserId = UserContextHolder.getUserId();
        // boolean hasPermission = appManageService.checkUserAppPermission(
        //     currentUserId, internalId);
        // if (!hasPermission) {
        //     throw AppAccessException.noPermission(externalAppId);
        // }
        
        // 4. 返回上下文
        // return AppContext.builder()
        //     .internalId(internalId)
        //     .externalId(externalAppId)
        //     .build();
        
        throw new UnsupportedOperationException(
            "StandardAppContextResolver not implemented yet, please implement it in standard environment");
    }

    @Override
    public String toExternalId(Long internalId) {
        // TODO: 标准环境实现
        // return appManageService.getExternalIdByInternalId(internalId);
        throw new UnsupportedOperationException(
            "StandardAppContextResolver not implemented yet, please implement it in standard environment");
    }
}