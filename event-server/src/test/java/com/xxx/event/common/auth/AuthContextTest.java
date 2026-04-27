package com.xxx.event.common.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 认证上下文测试
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 * @since 2026-04-27
 */
class AuthContextTest {

    @Test
    void testNoAuth() {
        AuthContext context = AuthContext.noAuth();
        
        assertEquals(AuthType.NONE, context.getAuthType());
        assertNull(context.getAuthCredentials());
        assertFalse(context.requiresAuth());
        assertTrue(context.isValid());
    }

    @Test
    void testAppTypeA() {
        AuthContext context = AuthContext.appTypeA("token-a");
        
        assertEquals(AuthType.APP_TYPE_A, context.getAuthType());
        assertEquals("token-a", context.getAuthCredentials());
        assertTrue(context.requiresAuth());
        assertTrue(context.isValid());
    }

    @Test
    void testAppTypeB() {
        AuthContext context = AuthContext.appTypeB("token-b");
        
        assertEquals(AuthType.APP_TYPE_B, context.getAuthType());
        assertEquals("token-b", context.getAuthCredentials());
        assertTrue(context.requiresAuth());
        assertTrue(context.isValid());
    }

    @Test
    void testBearerToken() {
        AuthContext context = AuthContext.bearerToken("bearer-token");
        
        assertEquals(AuthType.BEARER_TOKEN, context.getAuthType());
        assertEquals("bearer-token", context.getAuthCredentials());
        assertTrue(context.requiresAuth());
        assertTrue(context.isValid());
    }

    @Test
    void testAKSK() {
        AuthContext context = AuthContext.aksk("access-key");
        
        assertEquals(AuthType.AKSK, context.getAuthType());
        assertEquals("access-key", context.getAuthCredentials());
        assertTrue(context.requiresAuth());
        assertTrue(context.isValid());
    }

    @Test
    void testRequiresAuth() {
        // 免认证
        assertFalse(AuthContext.noAuth().requiresAuth());
        
        // 需要认证
        assertTrue(AuthContext.bearerToken("token").requiresAuth());
    }

    @Test
    void testIsValid() {
        // 免认证模式有效
        assertTrue(AuthContext.noAuth().isValid());
        
        // 有凭证有效
        assertTrue(AuthContext.bearerToken("token").isValid());
        
        // 无认证类型无效
        AuthContext noType = AuthContext.builder()
                .authCredentials("token")
                .build();
        assertFalse(noType.isValid());
        
        // 需要认证但无凭证无效
        AuthContext noCred = AuthContext.builder()
                .authType(AuthType.BEARER_TOKEN)
                .authCredentials(null)
                .build();
        assertFalse(noCred.isValid());
        
        // 空凭证无效
        AuthContext emptyCred = AuthContext.builder()
                .authType(AuthType.BEARER_TOKEN)
                .authCredentials("  ")
                .build();
        assertFalse(emptyCred.isValid());
    }

    @Test
    void testBuilder() {
        AuthContext context = AuthContext.builder()
                .authType(AuthType.AKSK)
                .authCredentials("test-key")
                .build();
        
        assertEquals(AuthType.AKSK, context.getAuthType());
        assertEquals("test-key", context.getAuthCredentials());
    }
}
