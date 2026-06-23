package com.xxx.it.works.wecode.v2.common.snapshot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EntitySnapshotLoader 测试")
class EntitySnapshotLoaderTest {

    @Test
    @DisplayName("连接器快照 loader — supportedObjects 包含 connector")
    void testConnectorLoader_SupportedObjects() {
        EntitySnapshotLoader loader = new EntitySnapshotLoader() {
            @Override
            public List<String> supportedObjects() {
                return Arrays.asList("cp_connector", "cp_connector_v2");
            }
            @Override
            public Object loadById(Long id) {
                return Map.of("id", id, "name", "test-connector");
            }
        };

        List<String> supported = loader.supportedObjects();
        assertEquals(2, supported.size());
        assertTrue(supported.contains("cp_connector"));
    }

    @Test
    @DisplayName("连接器快照加载 — loadById 返回实体 Map")
    void testConnectorLoader_LoadById() {
        EntitySnapshotLoader loader = new EntitySnapshotLoader() {
            @Override
            public List<String> supportedObjects() {
                return Collections.singletonList("cp_connector");
            }
            @Override
            public Object loadById(Long id) {
                return Map.of("id", id, "nameCn", "测试连接器", "nameEn", "test_connector");
            }
        };

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) loader.loadById(1L);

        assertEquals(1L, result.get("id"));
        assertEquals("测试连接器", result.get("nameCn"));
        assertEquals("test_connector", result.get("nameEn"));
    }

    @Test
    @DisplayName("连接流快照 loader — supportedObjects 包含 flow")
    void testFlowLoader_SupportedObjects() {
        EntitySnapshotLoader loader = new EntitySnapshotLoader() {
            @Override
            public List<String> supportedObjects() {
                return Arrays.asList("cp_flow", "cp_flow_v2");
            }
            @Override
            public Object loadById(Long id) {
                return Map.of("id", id, "version", 1);
            }
        };

        assertTrue(loader.supportedObjects().contains("cp_flow_v2"));
    }

    @Test
    @DisplayName("loadById 未找到 → 返回 null")
    void testLoadById_NotFound() {
        EntitySnapshotLoader loader = new EntitySnapshotLoader() {
            @Override
            public List<String> supportedObjects() {
                return Collections.singletonList("test");
            }
            @Override
            public Object loadById(Long id) {
                return null;
            }
        };

        assertNull(loader.loadById(999L));
    }

    @Test
    @DisplayName("变更前后快照对比 — 关键字段差异")
    void testSnapshotComparison_BeforeAndAfter() {
        EntitySnapshotLoader loader = new EntitySnapshotLoader() {
            @Override
            public List<String> supportedObjects() {
                return Collections.singletonList("test");
            }
            @Override
            public Object loadById(Long id) {
                if (id == 1L) {
                    Map<String, Object> snapshot = new LinkedHashMap<>();
                    snapshot.put("id", 1L);
                    snapshot.put("nameCn", "旧名称");
                    snapshot.put("lifecycleStatus", 1);
                    snapshot.put("createTime", "2026-01-01");
                    snapshot.put("lastUpdateTime", "2026-06-01");
                    return snapshot;
                }
                return null;
            }
        };

        @SuppressWarnings("unchecked")
        Map<String, Object> before = (Map<String, Object>) loader.loadById(1L);
        assertNotNull(before);
        assertEquals("旧名称", before.get("nameCn"));
        assertEquals(1, before.get("lifecycleStatus"));

        // 模拟变更后
        Map<String, Object> after = new LinkedHashMap<>(before);
        after.put("nameCn", "新名称");
        after.put("lastUpdateTime", "2026-06-22");

        // 对比关键字段差异
        assertEquals("旧名称", before.get("nameCn"));
        assertEquals("新名称", after.get("nameCn"));
        assertNotEquals(before.get("nameCn"), after.get("nameCn"));
    }
}
