package com.xxx.event.common.auth;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 认证类型测试
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 * @since 2026-04-27
 */
class AuthTypeTest {

    @Test
    void testFromCode() {
        // 正常情况
        assertEquals(AuthType.APP_TYPE_A, AuthType.fromCode(0));
        assertEquals(AuthType.APP_TYPE_B, AuthType.fromCode(1));
        assertEquals(AuthType.AKSK, AuthType.fromCode(5));
        assertEquals(AuthType.BEARER_TOKEN, AuthType.fromCode(3));
        assertEquals(AuthType.NONE, AuthType.fromCode(4));
        
        // 边界情况
        assertNull(AuthType.fromCode(null));
        assertNull(AuthType.fromCode(999));
    }

    @Test
    void testIsAppCredentials() {
        assertTrue(AuthType.APP_TYPE_A.isAppCredentials());
        assertTrue(AuthType.APP_TYPE_B.isAppCredentials());
        assertFalse(AuthType.AKSK.isAppCredentials());
        assertFalse(AuthType.NONE.isAppCredentials());
    }

    @Test
    void testRequiresAuth() {
        assertFalse(AuthType.NONE.requiresAuth());
        assertTrue(AuthType.APP_TYPE_A.requiresAuth());
        assertTrue(AuthType.AKSK.requiresAuth());
        assertTrue(AuthType.BEARER_TOKEN.requiresAuth());
    }
}
