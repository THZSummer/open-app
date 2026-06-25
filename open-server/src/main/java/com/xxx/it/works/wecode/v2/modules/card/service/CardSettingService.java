package com.xxx.it.works.wecode.v2.modules.card.service;

import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.app.entity.AppProperty;
import com.xxx.it.works.wecode.v2.modules.app.mapper.AppPropertyMapper;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContext;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContextResolver;
import com.xxx.it.works.wecode.v2.modules.card.client.CardServiceClient;
import com.xxx.it.works.wecode.v2.modules.card.client.CardServicePeriodDTO;
import com.xxx.it.works.wecode.v2.modules.card.client.CardServiceResponse;
import com.xxx.it.works.wecode.v2.modules.card.dto.CardSettingResponse;
import com.xxx.it.works.wecode.v2.modules.card.dto.UpdateCardPeriodRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 卡片设置 Service
 *
 * <p>职责：权限校验 + clientId 查询 + tenantId 获取 + 参数校验 + 调用卡片服务 + 字段映射 + 异常兜底。</p>
 *
 * <p>关键约束：</p>
 * <ul>
 *   <li>不持久化任何卡片设置数据（SoT 在卡片服务）</li>
 *   <li>{@code clientId} 通过 {@code AppPropertyMapper} 从 {@code openplatform_app_p_t} 表查 {@code eamap_app_code} 属性</li>
 *   <li>{@code tenantId} 通过工具类获取（TODO：当前占位为配置文件 {@code card-service.default-tenant-id}）</li>
 *   <li>{@code periodDays} 范围由 {@code periodType} 动态决定（1~7 或 1~30）</li>
 * </ul>
 *
 * @author Spec Agent
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardSettingService {

    /** 应用属性名：eamap 应用标识，作为卡片服务的 clientId */
    private static final String PROPERTY_EAMAP_APP_CODE = "eamap_app_code";

    private final AppContextResolver appContextResolver;
    private final AppPropertyMapper appPropertyMapper;
    private final CardServiceClient cardServiceClient;

    /**
     * tenantId 占位配置
     *
     * <p>TODO：卡片服务的 tenantId 应由专门的工具类获取（见 OQ-12）。
     * 当前占位为配置文件 {@code card-service.default-tenant-id}，
     * 由人工二开时替换为真实工具类调用。</p>
     */
    @Value("${card-service.default-tenant-id:}")
    private String defaultTenantId;

    /**
     * 查询卡片设置
     *
     * @param appId 应用外部业务 ID（path 参数）
     * @return {@link CardSettingResponse}（字段可能为 null，表示卡片服务未配置）
     */
    public CardSettingResponse getCardSetting(String appId) {
        log.info("getCardSetting appId={}", appId);

        // 1. 权限校验 + 拿到 AppContext
        AppContext appContext = appContextResolver.resolveAndValidate(appId);
        Long internalId = appContext.getInternalId();

        // 2. 查 clientId
        String clientId = queryEamapAppCode(internalId);

        // 3. 取 tenantId
        String tenantId = getCurrentTenantId();

        // 4. 调卡片服务
        CardServiceResponse<CardServicePeriodDTO> resp = cardServiceClient.queryCardPeriod(tenantId, clientId);
        if (!resp.isSuccess()) {
            // 正常情况下 CardServiceClientImpl 已经抛异常；Stub 不会走到这里
            throw new BusinessException("500", "卡片服务返回失败", "Card service returned failure");
        }

        // 5. 字段映射
        CardServicePeriodDTO data = resp.getData();
        return CardSettingResponse.builder()
                .expirationDays(data != null ? data.getExpirationPeriod() : null)
                .deletionDays(data != null ? data.getDeletionPeriod() : null)
                .build();
    }

    /**
     * 更新卡片周期（失效/删除合并）
     *
     * @param appId   应用外部业务 ID（path 参数）
     * @param request 周期更新请求（periodType + periodDays）
     * @return 空成功响应（前端保存成功后会重新 GET 回填）
     */
    public ApiResponse<Void> updateCardPeriod(String appId, @Valid UpdateCardPeriodRequest request) {
        log.info("updateCardPeriod appId={}, periodType={}, periodDays={}",
                appId, request.getPeriodType(), request.getPeriodDays());

        // 1. 权限校验
        AppContext appContext = appContextResolver.resolveAndValidate(appId);
        Long internalId = appContext.getInternalId();

        // 2. 动态校验
        validatePeriodRequest(request);

        // 3. 查 clientId
        String clientId = queryEamapAppCode(internalId);

        // 4. 取 tenantId
        String tenantId = getCurrentTenantId();

        // 5. 调卡片服务（CardServiceClientImpl 内部处理异常；Stub 直接返回 success）
        cardServiceClient.updateCardPeriod(tenantId, clientId,
                request.getPeriodType(), request.getPeriodDays());

        return ApiResponse.success();
    }

    // ===== 私有方法 =====

    /**
     * 查询应用的 eamap_app_code 属性值（作为 clientId）
     *
     * @throws BusinessException 400 如果属性不存在
     */
    private String queryEamapAppCode(Long appId) {
        AppProperty property = appPropertyMapper.selectByAppIdAndPropertyName(appId, PROPERTY_EAMAP_APP_CODE);
        if (property == null || property.getPropertyValue() == null || property.getPropertyValue().isEmpty()) {
            throw new BusinessException("400",
                    "应用缺少 eamap_app_code 属性",
                    "App missing eamap_app_code property");
        }
        return property.getPropertyValue();
    }

    /**
     * 获取当前租户 ID
     *
     * <p>TODO（OQ-12）：此处为占位实现，从配置文件读取。
     * 应由人工二开时替换为真实的 tenantId 工具类调用。</p>
     *
     * @throws BusinessException 500 如果 tenantId 未配置
     */
    private String getCurrentTenantId() {
        if (defaultTenantId == null || defaultTenantId.isEmpty()) {
            throw new BusinessException("500",
                    "租户 ID 未配置，请在 application.yml 中设置 card-service.default-tenant-id",
                    "Tenant ID not configured");
        }
        return defaultTenantId;
    }

    /**
     * 动态校验 periodType + periodDays
     *
     * <p>规则：</p>
     * <ul>
     *   <li>periodType 必须为 0（删除）或 1（失效），否则 400</li>
     *   <li>periodType=1（失效）：1 ≤ periodDays ≤ 7</li>
     *   <li>periodType=0（删除）：1 ≤ periodDays ≤ 30</li>
     * </ul>
     */
    private void validatePeriodRequest(UpdateCardPeriodRequest request) {
        Integer type = request.getPeriodType();
        Integer days = request.getPeriodDays();

        if (type == null || (type != UpdateCardPeriodRequest.PERIOD_TYPE_DELETION
                && type != UpdateCardPeriodRequest.PERIOD_TYPE_EXPIRATION)) {
            throw new BusinessException("400",
                    "参数校验失败：periodType 必须为 0 或 1",
                    "Validation failed: periodType must be 0 or 1");
        }

        if (days == null) {
            throw new BusinessException("400",
                    "参数校验失败：periodDays",
                    "Validation failed: periodDays");
        }

        int min, max;
        String fieldDesc;
        if (type == UpdateCardPeriodRequest.PERIOD_TYPE_EXPIRATION) {
            min = UpdateCardPeriodRequest.EXPIRATION_MIN;
            max = UpdateCardPeriodRequest.EXPIRATION_MAX;
            fieldDesc = "定期失效时间";
        } else {
            min = UpdateCardPeriodRequest.DELETION_MIN;
            max = UpdateCardPeriodRequest.DELETION_MAX;
            fieldDesc = "定期删除时间";
        }

        if (days < min || days > max) {
            throw new BusinessException("400",
                    String.format("参数校验失败：%s 必须在 %d~%d 之间", fieldDesc, min, max),
                    String.format("Validation failed: %s must be between %d and %d", fieldDesc, min, max));
        }
    }
}
