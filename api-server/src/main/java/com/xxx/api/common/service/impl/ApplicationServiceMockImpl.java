package com.xxx.api.common.service.impl;

import com.xxx.api.common.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 应用服务 Mock 实现
 * 
 * <p>注意：这是 Mock 实现，仅供开发测试使用</p>
 * <p>生产环境需要替换为对接现有应用管理系统/AKSK 管理系统的实现</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Service
public class ApplicationServiceMockImpl implements ApplicationService {

    @Override
    public Long getAppIdByAk(String ak) {
        // TODO: 对接现有 AKSK 管理系统
        // 实际实现应该调用现有的应用管理系统或 AKSK 管理系统接口
        
        if (ak == null || ak.isEmpty()) {
            return null;
        }
        
        // Mock: 以 "AK" 开头的视为有效 AK
        if (ak.startsWith("AK")) {
            try {
                String numPart = ak.substring(2);
                // 简单处理：取模生成 appId（仅用于测试）
                Long appId = Math.abs(numPart.hashCode() % 1000) + 1L;
                log.debug("Mock: AK={} -> appId={}", ak, appId);
                return appId;
            } catch (Exception e) {
                log.warn("Mock: Failed to parse AK: {}", ak, e);
                return null;
            }
        }
        
        return null;
    }

    @Override
    public boolean verifyApplication(String appId, Integer authType, String authCredential) {
        // TODO: 对接现有应用管理系统
        // 实际实现应该：
        // 1. authType = 5: 验证 AKSK 签名
        // 2. authType = 3: 验证 Bearer Token
        // 3. 调用应用管理系统获取应用信息并验证
        
        if (appId == null || appId.isEmpty()) {
            return false;
        }
        
        if (authCredential == null || authCredential.isEmpty()) {
            return false;
        }
        
        // Mock: 简单验证通过
        log.debug("Mock: Application authentication passed: appId={}, authType={}", appId, authType);
        return true;
    }
}