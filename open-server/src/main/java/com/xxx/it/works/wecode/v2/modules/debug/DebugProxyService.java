package com.xxx.it.works.wecode.v2.modules.debug;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 调试代理服务
 * <p>
 * 接收前端测试运行请求并转发至 connector-api 内部测试接口
 * </p>
 */
@Service
public class DebugProxyService {

    private static final Logger log = LoggerFactory.getLogger(DebugProxyService.class);

    private final RestTemplate restTemplate;

    @Value("${connector-api.base-url:http://localhost:18180}")
    private String connectorApiBaseUrl;

    public DebugProxyService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 转发测试运行请求到 connector-api
     *
     * @param flowId 连接流ID
     * @param mockTriggerData 模拟触发数据
     * @param credentials 凭证 (按 connectorVersionId 分组)
     * @return 转发结果
     */
    public ApiResponse<Map<String, Object>> forwardTestRun(
            Long flowId,
            Map<String, Object> mockTriggerData,
            Map<String, Map<String, String>> credentials) {

        String url = connectorApiBaseUrl + "/api/v1/internal/test-run/" + flowId;

        log.info("Forwarding test run to connector-api: flowId={}, url={}", flowId, url);

        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("mockTriggerData", mockTriggerData != null ? mockTriggerData : new HashMap<>());
            requestBody.put("credentials", credentials != null ? credentials : new HashMap<>());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // 转发请求
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> result = response.getBody();
            log.info("Test run forwarded successfully: flowId={}, status={}", flowId,
                    result != null ? result.get("status") : "unknown");

            return ApiResponse.success(result);

        } catch (Exception e) {
            log.error("Failed to forward test run: flowId={}, error={}", flowId, e.getMessage());
            return ApiResponse.error("500",
                    "测试运行转发失败: " + e.getMessage(),
                    "Test run forwarding failed: " + e.getMessage());
        }
    }
}