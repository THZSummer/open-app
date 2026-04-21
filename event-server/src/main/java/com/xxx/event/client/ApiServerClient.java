package com.xxx.event.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.event.common.model.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * API Server 客户端
 * 
 * <p>调用 api-server 的数据查询接口，获取权限、订阅等数据</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiServerClient {

    @Value("${api-server.url:http://localhost:18081}")
    private String apiServerUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 查询权限详情
     * 
     * @param scope 权限标识
     * @return 权限详情
     */
    public Map<String, Object> getPermissionByScope(String scope) {
        try {
            String url = apiServerUrl + "/gateway/permissions/detail?scope=" + scope;
            log.debug("查询权限详情: scope={}, url={}", scope, url);

            String response = restTemplate.getForObject(url, String.class);
            ApiResponse<Map<String, Object>> apiResponse = objectMapper.readValue(
                    response,
                    new TypeReference<ApiResponse<Map<String, Object>>>() {}
            );

            if ("200".equals(apiResponse.getCode())) {
                return apiResponse.getData();
            }

            log.warn("查询权限详情失败: scope={}, code={}", scope, apiResponse.getCode());
            return null;

        } catch (Exception e) {
            log.error("查询权限详情失败: scope={}", scope, e);
            return null;
        }
    }

    /**
     * 查询订阅某权限的所有应用
     * 
     * @param scope 权限标识
     * @return 应用ID列表
     */
    public List<String> getSubscribedApps(String scope) {
        try {
            String url = apiServerUrl + "/gateway/permissions/subscribers?scope=" + scope;
            log.debug("查询订阅应用列表: scope={}, url={}", scope, url);

            String response = restTemplate.getForObject(url, String.class);
            ApiResponse<List<String>> apiResponse = objectMapper.readValue(
                    response,
                    new TypeReference<ApiResponse<List<String>>>() {}
            );

            if ("200".equals(apiResponse.getCode())) {
                return apiResponse.getData();
            }

            log.warn("查询订阅应用列表失败: scope={}, code={}", scope, apiResponse.getCode());
            return List.of();

        } catch (Exception e) {
            log.error("查询订阅应用列表失败: scope={}", scope, e);
            return List.of();
        }
    }

    /**
     * 查询应用的订阅配置
     * 
     * @param appId 应用ID
     * @param scope 权限标识
     * @return 订阅配置
     */
    public Map<String, Object> getSubscriptionConfig(String appId, String scope) {
        try {
            String url = apiServerUrl + "/gateway/subscriptions/config?appId=" + appId + "&scope=" + scope;
            log.debug("查询订阅配置: appId={}, scope={}, url={}", appId, scope, url);

            String response = restTemplate.getForObject(url, String.class);
            ApiResponse<Map<String, Object>> apiResponse = objectMapper.readValue(
                    response,
                    new TypeReference<ApiResponse<Map<String, Object>>>() {}
            );

            if ("200".equals(apiResponse.getCode())) {
                return apiResponse.getData();
            }

            log.warn("查询订阅配置失败: appId={}, scope={}, code={}", appId, scope, apiResponse.getCode());
            return Map.of();

        } catch (Exception e) {
            log.error("查询订阅配置失败: appId={}, scope={}", appId, scope, e);
            return Map.of();
        }
    }

    /**
     * 权限校验
     * 
     * @param appId 应用ID
     * @param scope 权限标识
     * @return 是否授权
     */
    public boolean checkPermission(String appId, String scope) {
        try {
            String url = apiServerUrl + "/gateway/permissions/check?appId=" + appId + "&scope=" + scope;
            log.debug("权限校验: appId={}, scope={}, url={}", appId, scope, url);

            String response = restTemplate.getForObject(url, String.class);
            ApiResponse<Map<String, Object>> apiResponse = objectMapper.readValue(
                    response,
                    new TypeReference<ApiResponse<Map<String, Object>>>() {}
            );

            if ("200".equals(apiResponse.getCode())) {
                Map<String, Object> data = apiResponse.getData();
                return data != null && Boolean.TRUE.equals(data.get("authorized"));
            }

            return false;

        } catch (Exception e) {
            log.error("权限校验失败: appId={}, scope={}", appId, scope, e);
            return false;
        }
    }
}
