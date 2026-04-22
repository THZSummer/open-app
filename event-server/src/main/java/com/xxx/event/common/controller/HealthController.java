package com.xxx.event.common.controller;

import com.xxx.event.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Tag(name = "健康检查", description = "服务健康检查接口")
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @Operation(summary = "健康检查", description = "返回服务健康状态")
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("service", "event-server");
        data.put("timestamp", LocalDateTime.now());
        data.put("version", "1.0.0");
        return ApiResponse.success(data);
    }
}
