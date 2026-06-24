package com.xxx.it.works.wecode.v2.modules.debug;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.common.security.PlatformAdminPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 调试代理 Controller
 * <p>
 * 接收前端测试运行请求并转发至 connector-api 内部测试接口
 * API: POST /service/open/v2/flows/{flowId}/versions/{versionId}/debug
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/service/open/v2/flows")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Tag(name = "调试代理", description = "测试运行代理，转发至 connector-api 内部测试接口")
public class OpDebugProxyController {

    private final OpDebugProxyService debugProxyService;

    /**
     * 测试运行
     * <p>
     * POST /service/open/v2/flows/{flowId}/versions/{versionId}/debug
     * 接收前端请求（含 triggerData（新 v5.5 字段格式）+ credentials）
     * triggerData 使用新的字段名：authConfig/inputContract/outputContract/rateLimitConfig
     * 转发至 connector-api 测试接口
     * 透传 connector-api 返回的 ExecutionResult
     * </p>
     */
    @PostMapping("/{flowId}/versions/{versionId}/debug")
    @PlatformAdminPermission
    @Operation(summary = "测试运行",
               description = "接收前端测试运行请求并转发至 connector-api 内部测试接口")
    public ApiResponse<Map<String, Object>> testRun(
            @Parameter(description = "连接流ID")
            @PathVariable Long flowId,
            @Parameter(description = "版本ID")
            @PathVariable Long versionId,
            @RequestBody TestRunRequest request) {

        log.info("POST /service/open/v2/flows/{}/versions/{}/debug", flowId, versionId);

        return debugProxyService.forwardTestRun(
                flowId,
                versionId,
                request != null ? request.getTriggerData() : null,
                request != null ? request.getCredentials() : null
        );
    }

    /**
     * 测试运行请求体
     */
    @Data
    public static class TestRunRequest {
        /** 模拟触发数据 */
        private Map<String, Object> triggerData;

        /** 凭证 (按 connectorVersionId 分组) */
        private Map<String, Map<String, String>> credentials;
    }
}