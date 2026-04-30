package com.xxx.event.common.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 认证类型测试
 * 
 * @author SDDU Build Agent
 * @version 3.0.0
 * @since 2026-04-27
 */
class AuthTypeTest {

    @Test
    void testFromCode() {

        // 正常情况
        assertEquals(AuthTypeEnum.COOKIE, AuthTypeEnum.fromCode(0));
        assertEquals(AuthTypeEnum.SOA, AuthTypeEnum.fromCode(1));
        assertEquals(AuthTypeEnum.AKSK, AuthTypeEnum.fromCode(5));
        assertEquals(AuthTypeEnum.IAM, AuthTypeEnum.fromCode(3));
        assertEquals(AuthTypeEnum.NONE, AuthTypeEnum.fromCode(4));
        
        // 边界情况
        assertNull(AuthTypeEnum.fromCode(null));
        assertNull(AuthTypeEnum.fromCode(999));
    }

    @Test
    void testIsAppCredentials() {
        assertTrue(AuthTypeEnum.COOKIE.isAppCredentials());
        assertTrue(AuthTypeEnum.SOA.isAppCredentials());
        assertFalse(AuthTypeEnum.AKSK.isAppCredentials());
        assertFalse(AuthTypeEnum.NONE.isAppCredentials());
    }

    @Test
    void testRequiresAuth() {
        assertFalse(AuthTypeEnum.NONE.requiresAuth());
        assertTrue(AuthTypeEnum.COOKIE.requiresAuth());
        assertTrue(AuthTypeEnum.AKSK.requiresAuth());
        assertTrue(AuthTypeEnum.IAM.requiresAuth());
    }
}