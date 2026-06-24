package com.xxx.it.works.wecode.v2.modules.card.client;

/**
 * 卡片服务客户端接口
 *
 * <p>封装对卡片服务（外部独立服务）的所有 HTTP 调用。</p>
 * <p>卡片服务原始接口：</p>
 * <ul>
 *   <li>查询：GET {@code /interactive/card/businesscenter/period/setting/v1?tenantId=X&clientId=Y}</li>
 *   <li>修改：PUT {@code /interactive/card/businesscenter/period/setting/v1}
 *       body: {@code {tenantId, clientId, periodType, period}}</li>
 * </ul>
 *
 * <p>所有方法返回 {@link CardServiceResponse}，调用方需检查 {@code isSuccess()}，
 * 失败时根据 {@code error} 字段决定对外表现（透传业务错误 / 包装为 502）。</p>
 *
 * <p>实现类：</p>
 * <ul>
 *   <li>{@code CardServiceClientStub}：默认实现，返回固定值，便于联调前跑通</li>
 *   <li>{@code CardServiceClientImpl}：真实 HTTP 实现（配置 {@code card-service.base-url} 后启用）</li>
 * </ul>
 *
 * @author Spec Agent
 * @version 1.0.0
 */
public interface CardServiceClient {

    /**
     * 查询应用卡片周期设置
     *
     * @param tenantId 租户 ID（从当前上下文获取）
     * @param clientId 应用客户端 ID（openplatform_app_p_t 表的 eamap_app_code）
     * @return 卡片服务响应，成功时 data 为 {@link CardServicePeriodDTO}
     */
    CardServiceResponse<CardServicePeriodDTO> queryCardPeriod(String tenantId, String clientId);

    /**
     * 更新卡片周期
     *
     * @param tenantId   租户 ID
     * @param clientId   应用客户端 ID
     * @param periodType 周期类型：0=删除周期，1=失效周期
     * @param period     周期天数（卡片服务字段名；open-server 内部叫 periodDays）
     * @return 卡片服务响应，成功时 data 为 "success" 字符串
     */
    CardServiceResponse<String> updateCardPeriod(String tenantId, String clientId,
                                                  int periodType, int period);
}
