package com.xxx.event.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.event.common.model.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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

    @Value("${api-server.auth.enabled:true}")
    private boolean authEnabled;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 获取 API Server 认证凭证（预留）
     * 
     * <p>返回 HTTP 头字段名和值</p>
     * <p>约定的认证类型：SOA</p>
     * <p>TODO: 实际项目中应调用凭证服务或应用管理系统获取</p>
     * 
     * @return HTTP 头字段映射（key=头字段名，value=头字段值）
     */
    private Map<String, String> getApiServerCredential() {

        // TODO: 实际实现示例
        // 1. 调用凭证服务获取凭证
        //    CredentialService credentialService = ...;
        //    String token = credentialService.getCredential("api-server", "SOA");
        //    return Map.of("X-SOA-TOKEN", token);
        
        // 2. 或调用应用管理系统
        //    AppCredentialClient client = ...;
        //    String token = client.getSoaCredential("api-server");
        //    return Map.of("X-SOA-TOKEN", token);
        
        log.warn("[Reserved] API Server credential method not implemented yet");
        return Map.of();  // 返回空 Map
    }

    /**
     * 为请求添加认证凭证
     * 
     * <p>调用 api-server 必须携带认证凭证</p>
     * 
     * @param headers HTTP请求头
     */
    private void applyAuth(HttpHeaders headers) {
        if (!authEnabled) {
            log.warn("API Server auth disabled, this may cause call failure");
            return;
        }
        
        // 获取凭证（头字段名和值）
        Map<String, String> credentials = getApiServerCredential();
        
        if (credentials.isEmpty()) {
            log.error("Failed to get API Server auth credentials, please implement getApiServerCredential() method");
            return;
        }
        
        // 直接设置所有头字段
        for (Map.Entry<String, String> entry : credentials.entrySet()) {
            String headerName = entry.getKey();
            String headerValue = entry.getValue();
            
            if (headerValue != null && !headerValue.isEmpty()) {
                headers.set(headerName, headerValue);
                log.debug("Added API Server auth header: {}={}", headerName, headerValue);
            } else {
                log.warn("Auth header value is empty, skip setting: {}", headerName);
            }
        }
    }

    /**
     * 查询权限详情
     * 
     * @param scope 权限标识
     * @return 权限详情
     */
    public Map<String, Object> getPermissionByScope(String scope) {
        try {
            String url = apiServerUrl + "/gateway/permissions/detail?scope=" + scope;
            log.debug("Query permission detail: scope={}, url={}", scope, url);

            // 创建请求头并添加凭证
            HttpHeaders headers = new HttpHeaders();
            applyAuth(headers);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                String.class
            );
            
            ApiResponse<Map<String, Object>> apiResponse = objectMapper.readValue(
                    response.getBody(),
                    new TypeReference<ApiResponse<Map<String, Object>>>() {}
            );

            if ("200".equals(apiResponse.getCode())) {
                return apiResponse.getData();
            }

            log.warn("Query permission detail failed: scope={}, code={}", scope, apiResponse.getCode());
            return null;

        } catch (Exception e) {
            log.error("Query permission detail failed: scope={}", scope, e);
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
            log.debug("Query subscribed apps list: scope={}, url={}", scope, url);

            // 创建请求头并添加凭证
            HttpHeaders headers = new HttpHeaders();
            applyAuth(headers);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                String.class
            );
            
            ApiResponse<List<String>> apiResponse = objectMapper.readValue(
                    response.getBody(),
                    new TypeReference<ApiResponse<List<String>>>() {}
            );

            if ("200".equals(apiResponse.getCode())) {
                return apiResponse.getData();
            }

            log.warn("Query subscribed apps list failed: scope={}, code={}", scope, apiResponse.getCode());
            return List.of();

        } catch (Exception e) {
            log.error("Query subscribed apps list failed: scope={}", scope, e);
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
            log.debug("Query subscription config: appId={}, scope={}, url={}", appId, scope, url);

            // 创建请求头并添加凭证
            HttpHeaders headers = new HttpHeaders();
            applyAuth(headers);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                String.class
            );
            
            ApiResponse<Map<String, Object>> apiResponse = objectMapper.readValue(
                    response.getBody(),
                    new TypeReference<ApiResponse<Map<String, Object>>>() {}
            );

            if ("200".equals(apiResponse.getCode())) {
                return apiResponse.getData();
            }

            log.warn("Query subscription config failed: appId={}, scope={}, code={}", appId, scope, apiResponse.getCode());
            return Map.of();

        } catch (Exception e) {
            log.error("Query subscription config failed: appId={}, scope={}", appId, scope, e);
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
            log.debug("Permission check: appId={}, scope={}, url={}", appId, scope, url);

            // 创建请求头并添加凭证
            HttpHeaders headers = new HttpHeaders();
            applyAuth(headers);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                String.class
            );
            
            ApiResponse<Map<String, Object>> apiResponse = objectMapper.readValue(
                    response.getBody(),
                    new TypeReference<ApiResponse<Map<String, Object>>>() {}
            );

            if ("200".equals(apiResponse.getCode())) {
                Map<String, Object> data = apiResponse.getData();
                return data != null && Boolean.TRUE.equals(data.get("authorized"));
            }

            return false;

        } catch (Exception e) {
            log.error("Permission check failed: appId={}, scope={}", appId, scope, e);
            return false;
        }
    }
}
