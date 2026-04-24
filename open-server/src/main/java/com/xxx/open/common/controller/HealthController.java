package com.xxx.open.common.controller;

import com.xxx.open.common.context.UserContextHolder;
import com.xxx.open.common.model.ApiResponse;
import com.xxx.open.common.model.UserContext;
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
 * <p>提供简单的健康检查接口，不依赖数据库和 Redis</p>
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
        data.put("service", "open-server");
        data.put("timestamp", LocalDateTime.now());
        data.put("version", "1.0.0");
        return ApiResponse.success(data);
    }

    @Operation(summary = "用户信息", description = "返回当前用户上下文信息")
    @GetMapping("/user-info")
    public ApiResponse<Map<String, Object>> userInfo() {
        UserContext userContext = UserContextHolder.get();
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userContext.getUserId());
        data.put("userName", userContext.getUserName());
        data.put("authType", userContext.getAuthType());
        return ApiResponse.success(data);
    }
}
