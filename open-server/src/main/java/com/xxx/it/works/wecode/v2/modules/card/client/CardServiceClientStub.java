package com.xxx.it.works.wecode.v2.modules.card.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 卡片服务客户端 Stub 实现
 *
 * <p>用于联调前本地跑通，不调用真实卡片服务。</p>
 *
 * <p>行为：</p>
 * <ul>
 *   <li>{@code queryCardPeriod}: 返回内存中的当前值（首次返回系统默认 {@code expiration=14, deletion=7}）</li>
 *   <li>{@code updateCardPeriod}: 更新内存中的对应周期，返回 {@code status=1, data="success"}</li>
 * </ul>
 *
 * <p>激活条件：容器中没有其他 {@link CardServiceClient} Bean 时生效（{@code @ConditionalOnMissingBean}）。
 * 真实 HTTP 实现 {@code CardServiceClientImpl} 配置 {@code card-service.base-url} 后会替换本 Stub。</p>
 *
 * @author Spec Agent
 * @version 1.0.0
 */
@Slf4j
@Component
@ConditionalOnMissingBean(CardServiceClientImpl.class)
public class CardServiceClientStub implements CardServiceClient {

    /** 系统默认失效周期（天） */
    private static final int DEFAULT_EXPIRATION = 14;

    /** 系统默认删除周期（天） */
    private static final int DEFAULT_DELETION = 7;

    /** 内存数据：tenantId+clientId → {expiration, deletion} */
    private final Map<String, int[]> store = new ConcurrentHashMap<>();

    @Override
    public CardServiceResponse<CardServicePeriodDTO> queryCardPeriod(String tenantId, String clientId) {
        log.info("[STUB] queryCardPeriod tenantId={}, clientId={}", tenantId, clientId);

        int[] values = store.computeIfAbsent(key(tenantId, clientId),
                k -> new int[]{DEFAULT_EXPIRATION, DEFAULT_DELETION});

        CardServicePeriodDTO data = CardServicePeriodDTO.builder()
                .expirationPeriod(values[0])
                .deletionPeriod(values[1])
                .build();

        return CardServiceResponse.<CardServicePeriodDTO>builder()
                .status(1)
                .data(data)
                .error(null)
                .build();
    }

    @Override
    public CardServiceResponse<String> updateCardPeriod(String tenantId, String clientId,
                                                         int periodType, int period) {
        log.info("[STUB] updateCardPeriod tenantId={}, clientId={}, periodType={}, period={}",
                tenantId, clientId, periodType, period);

        int[] values = store.computeIfAbsent(key(tenantId, clientId),
                k -> new int[]{DEFAULT_EXPIRATION, DEFAULT_DELETION});

        if (periodType == 1) {
            values[0] = period;
        } else if (periodType == 0) {
            values[1] = period;
        }

        return CardServiceResponse.<String>builder()
                .status(1)
                .data("success")
                .error(null)
                .build();
    }

    private String key(String tenantId, String clientId) {
        return tenantId + ":" + clientId;
    }
}
