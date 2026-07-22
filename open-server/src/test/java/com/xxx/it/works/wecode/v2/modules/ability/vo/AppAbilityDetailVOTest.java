package com.xxx.it.works.wecode.v2.modules.ability.vo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AppAbilityDetailVO Serialization Test")
class AppAbilityDetailVOTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    @DisplayName("序列化 — 全部字段有值")
    void testSerialize_AllFields() throws Exception {
        AppAbilityDetailVO vo = new AppAbilityDetailVO();
        vo.setId("1");
        vo.setAbilityId("10");
        vo.setAbilityType(1);
        vo.setNameCn("群置顶服务");
        vo.setNameEn("Group Top");
        vo.setIconUrl("http://example.com/icon.png");
        vo.setOrderNum(1);
        vo.setEntryUrl("http://example.com/micro-app");
        vo.setRoutePath("/group-top");
        vo.setAliasName("groupTopApp");
        vo.setRequireRelease(0);
        vo.setLoadType(2);

        String json = mapper.writeValueAsString(vo);

        assertTrue(json.contains("\"entryUrl\":\"http://example.com/micro-app\""));
        assertTrue(json.contains("\"routePath\":\"/group-top\""));
        assertTrue(json.contains("\"aliasName\":\"groupTopApp\""));
        assertTrue(json.contains("\"requireRelease\":0"));
        assertTrue(json.contains("\"loadType\":2"));
    }

    @Test
    @DisplayName("序列化 — 新增字段为 null")
    void testSerialize_NullFields() throws Exception {
        AppAbilityDetailVO vo = new AppAbilityDetailVO();
        vo.setId("1");
        vo.setAbilityId("10");
        vo.setAbilityType(1);
        vo.setNameCn("群置顶服务");
        vo.setNameEn("Group Top");
        vo.setOrderNum(1);

        String json = mapper.writeValueAsString(vo);

        assertTrue(json.contains("\"entryUrl\":null"));
        assertTrue(json.contains("\"routePath\":null"));
        assertTrue(json.contains("\"aliasName\":null"));
        assertTrue(json.contains("\"requireRelease\":null"));
        assertTrue(json.contains("\"loadType\":null"));
    }

    @Test
    @DisplayName("反序列化 — 全部字段")
    void testDeserialize_AllFields() throws Exception {
        String json = """
                {
                    "id": "1",
                    "abilityId": "10",
                    "abilityType": 1,
                    "nameCn": "群置顶服务",
                    "nameEn": "Group Top",
                    "iconUrl": "http://example.com/icon.png",
                    "orderNum": 1,
                    "entryUrl": "http://example.com/entry",
                    "routePath": "/test",
                    "aliasName": "testApp",
                    "requireRelease": 0,
                    "loadType": 2
                }
                """;

        AppAbilityDetailVO vo = mapper.readValue(json, AppAbilityDetailVO.class);

        assertEquals("1", vo.getId());
        assertEquals(Integer.valueOf(1), vo.getAbilityType());
        assertEquals("http://example.com/entry", vo.getEntryUrl());
        assertEquals("/test", vo.getRoutePath());
        assertEquals("testApp", vo.getAliasName());
        assertEquals(Integer.valueOf(0), vo.getRequireRelease());
        assertEquals(Integer.valueOf(2), vo.getLoadType());
    }

    @Test
    @DisplayName("反序列化 — 无新增字段（向后兼容）")
    void testDeserialize_BackwardCompatible() throws Exception {
        String json = """
                {
                    "id": "1",
                    "abilityId": "10",
                    "abilityType": 1,
                    "nameCn": "群置顶服务",
                    "nameEn": "Group Top",
                    "orderNum": 1
                }
                """;

        AppAbilityDetailVO vo = mapper.readValue(json, AppAbilityDetailVO.class);

        assertEquals("1", vo.getId());
        assertEquals(Integer.valueOf(1), vo.getAbilityType());
        assertNull(vo.getEntryUrl());
        assertNull(vo.getRoutePath());
        assertNull(vo.getAliasName());
        assertNull(vo.getRequireRelease());
        assertNull(vo.getLoadType());
    }

    @Test
    @DisplayName("新增字段 getter/setter 正常")
    void testGettersAndSetters() {
        AppAbilityDetailVO vo = new AppAbilityDetailVO();

        vo.setEntryUrl("http://example.com/entry");
        vo.setRoutePath("/route");
        vo.setAliasName("myApp");
        vo.setRequireRelease(1);
        vo.setLoadType(1);

        assertEquals("http://example.com/entry", vo.getEntryUrl());
        assertEquals("/route", vo.getRoutePath());
        assertEquals("myApp", vo.getAliasName());
        assertEquals(Integer.valueOf(1), vo.getRequireRelease());
        assertEquals(Integer.valueOf(1), vo.getLoadType());
    }
}
