package com.xxx.open.common.controller;

import com.xxx.open.common.model.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("健康检查控制器测试")
class HealthControllerTest {

    @Test
    @DisplayName("健康检查成功")
    void testHealth() {
        HealthController controller = new HealthController();
        
        ApiResponse<Map<String, Object>> response = controller.health();
        
        assertNotNull(response);
        assertEquals("200", response.getCode());
        assertNotNull(response.getData());
        assertEquals("UP", response.getData().get("status"));
        assertEquals("open-server", response.getData().get("service"));
        assertEquals("1.0.0", response.getData().get("version"));
        assertNotNull(response.getData().get("timestamp"));
    }
}