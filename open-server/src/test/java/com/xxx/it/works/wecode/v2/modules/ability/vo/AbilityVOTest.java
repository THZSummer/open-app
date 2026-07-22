package com.xxx.it.works.wecode.v2.modules.ability.vo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AbilityVO 序列化/反序列化测试
 *
 * <p>验证新增的 5 个字段（entryUrl/routePath/aliasName/requireRelease/loadType）
 * 可正确序列化和反序列化，且 null 安全。
 */
@DisplayName("AbilityVO Serialization Test")
class AbilityVOTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    @DisplayName("序列化 — 全部字段有值")
    void testSerialize_AllFields() throws Exception {
        AbilityVO vo = new AbilityVO();
        vo.setAbilityId("1");
        vo.setAbilityType(1);
        vo.setNameCn("群置顶服务");
        vo.setNameEn("Group Top");
        vo.setDescCn("群置顶描述");
        vo.setDescEn("Group top description");
        vo.setIconUrl("http://example.com/icon.png");
        vo.setDiagramUrl("http://example.com/diagram.png");
        vo.setSubscribed(true);
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
        AbilityVO vo = new AbilityVO();
        vo.setAbilityId("1");
        vo.setAbilityType(1);
        vo.setNameCn("群置顶服务");
        vo.setNameEn("Group Top");
        vo.setSubscribed(false);
        vo.setOrderNum(1);

        String json = mapper.writeValueAsString(vo);

        // 新增字段应序列化为 null，不报错
        assertTrue(json.contains("\"entryUrl\":null"), "entryUrl null 字段应序列化");
        assertTrue(json.contains("\"routePath\":null"), "routePath null 字段应序列化");
        assertTrue(json.contains("\"aliasName\":null"), "aliasName null 字段应序列化");
        assertTrue(json.contains("\"requireRelease\":null"), "requireRelease null 字段应序列化");
        assertTrue(json.contains("\"loadType\":null"), "loadType null 字段应序列化");
    }

    @Test
    @DisplayName("反序列化 — 全部字段")
    void testDeserialize_AllFields() throws Exception {
        String json = """
                {
                    "abilityId": "1",
                    "abilityType": 1,
                    "nameCn": "群置顶服务",
                    "nameEn": "Group Top",
                    "descCn": "描述",
                    "descEn": "Description",
                    "iconUrl": "http://example.com/icon.png",
                    "diagramUrl": "http://example.com/diagram.png",
                    "subscribed": true,
                    "orderNum": 1,
                    "entryUrl": "http://example.com/entry",
                    "routePath": "/test",
                    "aliasName": "testApp",
                    "requireRelease": 0,
                    "loadType": 2
                }
                """;

        AbilityVO vo = mapper.readValue(json, AbilityVO.class);

        assertEquals("1", vo.getAbilityId());
        assertEquals(1, vo.getAbilityType());
        assertEquals("http://example.com/entry", vo.getEntryUrl());
        assertEquals("/test", vo.getRoutePath());
        assertEquals("testApp", vo.getAliasName());
        assertEquals(Integer.valueOf(0), vo.getRequireRelease());
        assertEquals(Integer.valueOf(2), vo.getLoadType());
    }

    @Test
    @DisplayName("反序列化 — 无新增字段（向后兼容）")
    void testDeserialize_BackwardCompatible() throws Exception {
        // 模拟旧版 JSON（无新增字段）
        String json = """
                {
                    "abilityId": "1",
                    "abilityType": 1,
                    "nameCn": "群置顶服务",
                    "nameEn": "Group Top",
                    "subscribed": false,
                    "orderNum": 1
                }
                """;

        AbilityVO vo = mapper.readValue(json, AbilityVO.class);

        assertEquals("1", vo.getAbilityId());
        assertEquals(1, vo.getAbilityType());
        assertNull(vo.getEntryUrl());
        assertNull(vo.getRoutePath());
        assertNull(vo.getAliasName());
        assertNull(vo.getRequireRelease());
        assertNull(vo.getLoadType());
    }

    @Test
    @DisplayName("新增字段 getter/setter 正常")
    void testGettersAndSetters() {
        AbilityVO vo = new AbilityVO();

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
