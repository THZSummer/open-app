package com.xxx.it.works.wecode.v2.modules.card.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.card.dto.CardSettingResponse;
import com.xxx.it.works.wecode.v2.modules.card.dto.UpdateCardPeriodRequest;
import com.xxx.it.works.wecode.v2.modules.card.service.CardSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 卡片设置 Controller
 *
 * <p>接口编号：#TBD-CS01 / #TBD-CS02（待与 PermissionController 的 #27~#43 体系统一分配正式编号）</p>
 *
 * <p>端点：</p>
 * <ul>
 *   <li>{@code #TBD-CS01} GET  /service/open/v2/apps/{appId}/card-settings（查询）</li>
 *   <li>{@code #TBD-CS02} PUT  /service/open/v2/apps/{appId}/card-settings（更新周期，periodType 区分失效/删除）</li>
 * </ul>
 *
 * <p>统一鉴权：{@code AppContextResolver.resolveAndValidate(appId)}（在 Service 层调用）</p>
 *
 * @author Spec Agent
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/service/open/v2")
@Tag(name = "卡片设置", description = "应用卡片周期设置（失效/删除）")
public class CardSettingController {

    private final CardSettingService cardSettingService;

    /**
     * #TBD-CS01 查询卡片设置
     *
     * <p>返回应用当前的失效周期和删除周期（可能为 null，表示卡片服务未配置）。</p>
     */
    @GetMapping("/apps/{appId}/card-settings")
    @Operation(summary = "#TBD-CS01 查询卡片设置",
            description = "返回应用的失效周期（expirationDays）和删除周期（deletionDays），两字段可能为 null 表示卡片服务未配置")
    public ApiResponse<CardSettingResponse> getCardSetting(
            @Parameter(description = "应用外部业务 ID（openplatform_app_t.appId）")
            @PathVariable String appId) {

        log.info("GET /apps/{}/card-settings", appId);

        CardSettingResponse data = cardSettingService.getCardSetting(appId);
        return ApiResponse.success(data);
    }

    /**
     * #TBD-CS02 更新卡片周期（失效/删除合并）
     *
     * <p>通过 {@code periodType} 区分更新的是"失效周期"还是"删除周期"：</p>
     * <ul>
     *   <li>{@code periodType=1}：更新失效周期（{@code periodDays ∈ [1,7]}）</li>
     *   <li>{@code periodType=0}：更新删除周期（{@code periodDays ∈ [1,30]}）</li>
     * </ul>
     *
     * <p>返回空 data，前端保存成功后会重新 GET 回填。</p>
     */
    @PutMapping("/apps/{appId}/card-settings")
    @Operation(summary = "#TBD-CS02 更新卡片周期",
            description = "通过 periodType 区分失效（1, 1~7 天）/ 删除（0, 1~30 天）周期")
    public ApiResponse<Void> updateCardPeriod(
            @Parameter(description = "应用外部业务 ID（openplatform_app_t.appId）")
            @PathVariable String appId,
            @Valid @RequestBody UpdateCardPeriodRequest request) {

        log.info("PUT /apps/{}/card-settings, periodType={}, periodDays={}",
                appId, request.getPeriodType(), request.getPeriodDays());

        return cardSettingService.updateCardPeriod(appId, request);
    }
}
