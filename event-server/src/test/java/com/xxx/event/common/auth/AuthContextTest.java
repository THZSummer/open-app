package com.xxx.event.common.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 认证上下文测试
 * 
 * <p>测试新的认证机制：</p>
 * <ul>
 *   <li>使用 appId + authType 自动获取凭证</li>
 *   <li>authCredentials 字段保留用于向后兼容，但已弃用</li>
 * </ul>
 * 
 * @author SDDU Build Agent
 * @version 3.0.0
 * @since 2026-04-27
 */
class AuthContextTest {

    @Test
    void testNoAuth_WithAppId() {
        AuthContext context = AuthContext.noAuth("app-001");
        
        assertEquals("app-001", context.getAppId());
        assertEquals(AuthTypeEnum.NONE, context.getAuthType());
        assertFalse(context.requiresAuth());
        assertTrue(context.isValid());
    }

    @Test
    void testOf() {
        AuthContext context = AuthContext.of("app-002", AuthTypeEnum.COOKIE);
        
        assertEquals("app-002", context.getAppId());
        assertEquals(AuthTypeEnum.COOKIE, context.getAuthType());
        assertTrue(context.requiresAuth());
        assertTrue(context.isValid());
    }

    @Test
    void testCookie() {
        AuthContext context = AuthContext.cookie("app-003");
        
        assertEquals("app-003", context.getAppId());
        assertEquals(AuthTypeEnum.COOKIE, context.getAuthType());
        assertTrue(context.requiresAuth());
        assertTrue(context.isValid());
    }

    @Test
    void testSoa() {
        AuthContext context = AuthContext.soa("app-004");
        
        assertEquals("app-004", context.getAppId());
        assertEquals(AuthTypeEnum.SOA, context.getAuthType());
        assertTrue(context.requiresAuth());
        assertTrue(context.isValid());
    }

    @Test
    void testIam() {
        AuthContext context = AuthContext.iam("app-005");
        
        assertEquals("app-005", context.getAppId());
        assertEquals(AuthTypeEnum.IAM, context.getAuthType());
        assertTrue(context.requiresAuth());
        assertTrue(context.isValid());
    }

    @Test
    void testAKSK() {
        AuthContext context = AuthContext.aksk("app-006");
        
        assertEquals("app-006", context.getAppId());
        assertEquals(AuthTypeEnum.AKSK, context.getAuthType());
        assertTrue(context.requiresAuth());
        assertTrue(context.isValid());
    }

    @Test
    void testRequiresAuth() {

        // 免认证
        assertFalse(AuthContext.noAuth("app-001").requiresAuth());
        
        // 需要认证
        assertTrue(AuthContext.iam("app-002").requiresAuth());
    }

    @Test
    void testIsValid() {

        // 免认证模式有效
        assertTrue(AuthContext.noAuth("app-001").isValid());
        
        // 有 appId 有效
        assertTrue(AuthContext.iam("app-002").isValid());
        
        // 无认证类型无效
        AuthContext noType = AuthContext.builder()
                .appId("app-003")
                .build();
        assertFalse(noType.isValid());
        
        // 无 appId 无效
        AuthContext noAppId = AuthContext.builder()
                .authType(AuthTypeEnum.IAM)
                .build();
        assertFalse(noAppId.isValid());
        
        // 空 appId 无效
        AuthContext emptyAppId = AuthContext.builder()
                .appId("")
                .authType(AuthTypeEnum.IAM)
                .build();
        assertFalse(emptyAppId.isValid());
    }

    @Test
    void testBuilder() {
        AuthContext context = AuthContext.builder()
                .appId("app-007")
                .authType(AuthTypeEnum.AKSK)
                .build();
        
        assertEquals("app-007", context.getAppId());
        assertEquals(AuthTypeEnum.AKSK, context.getAuthType());
        assertTrue(context.isValid());
    }

    @Test
    void testDeprecatedMethods() {

        // 测试向后兼容的方法
        AuthContext context1 = AuthContext.noAuth();
        assertEquals(AuthTypeEnum.NONE, context1.getAuthType());
        
        AuthContext context2 = AuthContext.of(AuthTypeEnum.COOKIE, "old-token");
        assertEquals(AuthTypeEnum.COOKIE, context2.getAuthType());
        assertEquals("old-token", context2.getAuthCredentials());
    }
}