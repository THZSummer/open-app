package com.xxx.api.scope.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("用户授权实体测试")
class UserAuthorizationTest {

    @Nested
    @DisplayName("getScopeList 测试")
    class GetScopeListTests {

        @Test
        @DisplayName("正常JSON数组")
        void testNormalJsonArray() {
            UserAuthorization auth = new UserAuthorization();
            auth.setScopes("[\"scope1\",\"scope2\",\"scope3\"]");

            List<String> result = auth.getScopeList();

            assertEquals(3, result.size());
            assertTrue(result.contains("scope1"));
            assertTrue(result.contains("scope2"));
            assertTrue(result.contains("scope3"));
        }

        @Test
        @DisplayName("单个元素")
        void testSingleElement() {
            UserAuthorization auth = new UserAuthorization();
            auth.setScopes("[\"scope1\"]");

            List<String> result = auth.getScopeList();

            assertEquals(1, result.size());
            assertTrue(result.contains("scope1"));
        }

        @Test
        @DisplayName("空数组")
        void testEmptyArray() {
            UserAuthorization auth = new UserAuthorization();
            auth.setScopes("[]");

            List<String> result = auth.getScopeList();

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("scopes为null")
        void testNullScopes() {
            UserAuthorization auth = new UserAuthorization();
            auth.setScopes(null);

            List<String> result = auth.getScopeList();

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("scopes为空字符串")
        void testEmptyScopes() {
            UserAuthorization auth = new UserAuthorization();
            auth.setScopes("");

            List<String> result = auth.getScopeList();

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("无效JSON格式")
        void testInvalidJsonFormat() {
            UserAuthorization auth = new UserAuthorization();
            auth.setScopes("invalid json");

            List<String> result = auth.getScopeList();

            assertTrue(result.isEmpty());
        }
    }
}
