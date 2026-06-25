package com.xxx.it.works.wecode.v2.modules.card.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 卡片服务查询响应 data 部分
 *
 * <p>对应卡片服务 GET 接口返回的 {@code data} 字段：</p>
 * <pre>
 * {
 *   "expirationPeriod": 15,
 *   "deletionPeriod": 7
 * }
 * </pre>
 *
 * <p>字段名与 open-server 对外响应的差异（在 {@code CardSettingService} 中映射）：</p>
 * <ul>
 *   <li>{@code expirationPeriod} → open-server {@code expirationDays}</li>
 *   <li>{@code deletionPeriod} → open-server {@code deletionDays}</li>
 * </ul>
 *
 * @author Spec Agent
 * @version 1.0.0
 * @see CardServiceResponse
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardServicePeriodDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 定期失效周期（天），卡片服务字段名
     */
    private Integer expirationPeriod;

    /**
     * 定期删除周期（天），卡片服务字段名
     */
    private Integer deletionPeriod;
}
