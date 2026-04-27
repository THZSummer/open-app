package com.xxx.event.common.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 认证处理器测试
 * 
 * <p>测试简化后的认证机制：</p>
 * <ul>
 *   <li>支持的认证类型：SOA、APIG、AKSK</li>
 *   <li>CredentialProvider 直接返回 HTTP 头字段映射</li>
 *   <li>AuthHandlerImpl 直接遍历设置，无需二次映射</li>
 * </ul>
 * 
 * @author SDDU Build Agent
 * @version 7.0.0
 * @since 2026-04-27
 */
class AuthHandlerImplTest {

    private AuthHandlerImpl authHandler;
    private TestCredentialProvider testCredentialProvider;

    @BeforeEach
    void setUp() {
        testCredentialProvider = new TestCredentialProvider();
        authHandler = new AuthHandlerImpl(testCredentialProvider);
    }

    @Test
    void testApplyAuth_Soa() {
        testCredentialProvider.setToken("test-soa-token");
        
        HttpHeaders headers = new HttpHeaders();
        authHandler.applyAuth(headers, "app-002", AuthTypeEnum.SOA);

        assertEquals("test-soa-token", headers.getFirst("X-SOA-TOKEN"));
    }

    @Test
    void testApplyAuth_APIG() {
        testCredentialProvider.setToken("test-apig-token");
        
        HttpHeaders headers = new HttpHeaders();
        authHandler.applyAuth(headers, "app-004", AuthTypeEnum.APIG);

        assertEquals("app-004", headers.getFirst("X-APIG-APPID"));
        assertEquals("test-apig-token", headers.getFirst("X-APIG-APPKEY"));
    }

    @Test
    void testApplyAuth_AKSK() {
        testCredentialProvider.setAkskToken("test-aksk-token");
        
        HttpHeaders headers = new HttpHeaders();
        authHandler.applyAuth(headers, "app-005", AuthTypeEnum.AKSK);

        assertEquals("test-aksk-token", headers.getFirst("X-AKSK-TOKEN"));
    }

    @Test
    void testApplyAuth_None() {
        HttpHeaders headers = new HttpHeaders();
        authHandler.applyAuth(headers, "app-007", AuthTypeEnum.NONE);

        // 免认证不应该添加任何头部
        assertNull(headers.getFirst("Authorization"));
        assertNull(headers.getFirst("X-Auth-Type"));
        assertTrue(headers.isEmpty());
    }

    @Test
    void testApplyAuth_NullAuthType() {
        HttpHeaders headers = new HttpHeaders();
        authHandler.applyAuth(headers, "app-008", null);

        // 应该跳过认证，不添加任何头部
        assertTrue(headers.isEmpty());
    }

    @Test
    void testApplyAuth_NullAppId() {
        HttpHeaders headers = new HttpHeaders();
        authHandler.applyAuth(headers, null, AuthTypeEnum.SOA);

        // 应该跳过认证
        assertTrue(headers.isEmpty());
    }

    @Test
    void testApplyAuth_EmptyAppId() {
        HttpHeaders headers = new HttpHeaders();
        authHandler.applyAuth(headers, "", AuthTypeEnum.SOA);

        // 应该跳过认证
        assertTrue(headers.isEmpty());
    }

    @Test
    void testApplyAuth_EmptyCredentials() {
        // 不设置凭证，CredentialProvider 返回空 Map
        testCredentialProvider.setReturnEmpty(true);
        
        HttpHeaders headers = new HttpHeaders();
        authHandler.applyAuth(headers, "app-009", AuthTypeEnum.SOA);

        // 应该跳过认证
        assertTrue(headers.isEmpty());
    }

    @Test
    void testValidateAuth_Valid() {
        testCredentialProvider.setToken("test-token");
        
        // 凭证存在，应该验证通过
        assertTrue(authHandler.validateAuth("app-001", AuthTypeEnum.SOA));
    }

    @Test
    void testValidateAuth_NoneAuth() {
        // 免认证类型，应该验证通过
        assertTrue(authHandler.validateAuth("app-001", AuthTypeEnum.NONE));
    }

    @Test
    void testValidateAuth_NullAppId() {
        // 应用ID为空，应该验证失败
        assertFalse(authHandler.validateAuth(null, AuthTypeEnum.SOA));
    }

    @Test
    void testValidateAuth_EmptyAppId() {
        // 应用ID为空，应该验证失败
        assertFalse(authHandler.validateAuth("", AuthTypeEnum.SOA));
    }

    /**
     * 测试用凭证提供器（直接返回 HTTP 头字段映射）
     * 
     * <p>支持的认证类型：SOA、APIG、AKSK</p>
     */
    private static class TestCredentialProvider implements CredentialProvider {
        private String token;
        private String akskToken;
        private boolean returnEmpty = false;

        public void setToken(String token) {
            this.token = token;
        }

        public void setAkskToken(String akskToken) {
            this.akskToken = akskToken;
        }

        public void setReturnEmpty(boolean returnEmpty) {
            this.returnEmpty = returnEmpty;
        }

        @Override
        public Map<String, String> getCredentials(String appId, AuthTypeEnum authType) {
            if (returnEmpty) {
                return Map.of();
            }

            Map<String, String> headers = new java.util.HashMap<>();
            
            switch (authType) {
                case SOA -> {
                    headers.put("X-SOA-TOKEN", token);
                }
                case APIG -> {
                    headers.put("X-APIG-APPID", appId);
                    headers.put("X-APIG-APPKEY", token);
                }
                case AKSK -> {
                    headers.put("X-AKSK-TOKEN", akskToken);
                }
                default -> {}
            }
            
            return headers;
        }
    }
}
