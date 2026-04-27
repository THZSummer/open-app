package com.xxx.event.common.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 认证处理器实现
 * 
 * <p>实现认证处理接口，提供具体的认证处理逻辑</p>
 * 
 * <p>简化设计：直接遍历设置 CredentialProvider 返回的头字段映射</p>
 * 
 * @author SDDU Build Agent
 * @version 6.0.0
 * @since 2026-04-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthHandlerImpl implements AuthHandler {

    private final CredentialProvider credentialProvider;

    @Override
    public void applyAuth(HttpHeaders headers, String appId, AuthTypeEnum authType) {
        // 参数校验
        if (authType == null || authType == AuthTypeEnum.NONE) {
            log.debug("免认证，跳过认证头应用: appId={}", appId);
            return;
        }

        if (appId == null || appId.trim().isEmpty()) {
            log.warn("应用ID为空，无法应用认证: authType={}", authType);
            return;
        }

        // 获取凭证（直接是头字段映射）
        Map<String, String> credentialHeaders = credentialProvider.getCredentials(appId, authType);
        
        if (credentialHeaders.isEmpty()) {
            log.warn("获取凭证失败，跳过认证头应用: appId={}, authType={}", appId, authType);
            return;
        }

        // 直接设置所有头字段（无需二次映射）
        for (Map.Entry<String, String> entry : credentialHeaders.entrySet()) {
            String headerName = entry.getKey();
            String headerValue = entry.getValue();
            
            // 只设置非空值
            if (headerValue != null && !headerValue.isEmpty()) {
                headers.set(headerName, headerValue);
                log.debug("已设置认证头: {}={}", headerName, headerValue);
            } else {
                log.warn("认证头值为空，跳过设置: {}", headerName);
            }
        }
        
        log.info("认证已应用: appId={}, authType={}, headers={}", appId, authType, credentialHeaders.size());
    }

    @Override
    public boolean validateAuth(String appId, AuthTypeEnum authType) {
        if (appId == null || appId.trim().isEmpty()) {
            log.warn("应用ID为空，无法验证认证");
            return false;
        }

        if (authType == null || authType == AuthTypeEnum.NONE) {
            log.debug("免认证类型，验证通过: appId={}", appId);
            return true;
        }

        // TODO: 实际项目中应调用应用管理系统或凭证服务验证凭证
        Map<String, String> credentials = credentialProvider.getCredentials(appId, authType);
        boolean isValid = !credentials.isEmpty();
        
        if (!isValid) {
            log.warn("认证凭证验证失败: appId={}, authType={}", appId, authType);
        }
        
        return isValid;
    }
}
