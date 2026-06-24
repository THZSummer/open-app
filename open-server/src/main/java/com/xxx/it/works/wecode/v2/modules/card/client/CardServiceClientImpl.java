package com.xxx.it.works.wecode.v2.modules.card.client;

import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * 卡片服务客户端 HTTP 实现
 *
 * <p>激活条件：配置了 {@code card-service.base-url} 时生效。</p>
 *
 * <p>异常处理：</p>
 * <ul>
 *   <li>网络异常 / 超时：抛 {@code BusinessException("502", "卡片服务暂时不可用", "Card service unavailable")}</li>
 *   <li>卡片服务返回 {@code status=0}：透传 {@code error.code / error.userMessageZh / error.userMessageEn}</li>
 * </ul>
 *
 * @author Spec Agent
 * @version 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "card-service.base-url")
public class CardServiceClientImpl implements CardServiceClient {

    private final RestTemplate restTemplate;

    @Value("${card-service.base-url}")
    private String baseUrl;

    @Value("${card-service.period-path:/interactive/card/businesscenter/period/setting/v1}")
    private String periodPath;

    @Value("${card-service.timeout-ms:5000}")
    private int timeoutMs;

    public CardServiceClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public CardServiceResponse<CardServicePeriodDTO> queryCardPeriod(String tenantId, String clientId) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + periodPath)
                .queryParam("tenantId", tenantId)
                .queryParam("clientId", clientId)
                .toUriString();

        log.info("Card service query: url={}, tenantId={}, clientId={}", url, tenantId, clientId);

        try {
            ResponseEntity<CardServiceResponse<CardServicePeriodDTO>> resp = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<CardServiceResponse<CardServicePeriodDTO>>() {}
            );

            CardServiceResponse<CardServicePeriodDTO> body = resp.getBody();
            if (body == null) {
                throw new BusinessException("502", "卡片服务返回空响应", "Card service returned empty response");
            }
            if (!body.isSuccess()) {
                throwOnServiceError(body.getError());
            }
            return body;
        } catch (ResourceAccessException e) {
            log.error("Card service network error: tenantId={}, clientId={}, error={}",
                    tenantId, clientId, e.getMessage());
            throw new BusinessException("502", "卡片服务暂时不可用", "Card service unavailable", e);
        }
    }

    @Override
    public CardServiceResponse<String> updateCardPeriod(String tenantId, String clientId,
                                                         int periodType, int period) {
        String url = baseUrl + periodPath;

        Map<String, Object> body = new HashMap<>();
        body.put("tenantId", tenantId);
        body.put("clientId", clientId);
        body.put("periodType", periodType);
        body.put("period", period);   // 注意：卡片服务字段名是 period，不是 periodDays

        log.info("Card service update: url={}, tenantId={}, clientId={}, periodType={}, period={}",
                url, tenantId, clientId, periodType, period);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<CardServiceResponse<String>> resp = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    new ParameterizedTypeReference<CardServiceResponse<String>>() {}
            );

            CardServiceResponse<String> respBody = resp.getBody();
            if (respBody == null) {
                throw new BusinessException("502", "卡片服务返回空响应", "Card service returned empty response");
            }
            if (!respBody.isSuccess()) {
                throwOnServiceError(respBody.getError());
            }
            return respBody;
        } catch (ResourceAccessException e) {
            log.error("Card service network error: tenantId={}, clientId={}, periodType={}, period={}, error={}",
                    tenantId, clientId, periodType, period, e.getMessage());
            throw new BusinessException("502", "卡片服务暂时不可用", "Card service unavailable", e);
        }
    }

    /**
     * 卡片服务返回 status=0 时，透传错误码和消息
     */
    private void throwOnServiceError(CardServiceError error) {
        if (error == null) {
            throw new BusinessException("500", "卡片服务返回未知错误", "Card service unknown error");
        }
        String code = error.getCode() != null ? String.valueOf(error.getCode()) : "500";
        String zh = error.getUserMessageZh() != null ? error.getUserMessageZh() : "卡片服务返回错误";
        String en = error.getUserMessageEn() != null ? error.getUserMessageEn() : "Card service error";
        throw new BusinessException(code, zh, en);
    }
}
