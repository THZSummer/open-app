package com.xxx.it.works.wecode.v2.modules.card.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 卡片设置查询响应
 *
 * <p>用于 GET /service/open/v2/apps/{appId}/card-settings 响应。</p>
 * <p>两字段均可为 null，表示卡片服务未配置。</p>
 *
 * <p>字段来源：卡片服务的 expirationPeriod / deletionPeriod，
 * 由 {@code CardSettingService} 在映射时完成字段名转换。</p>
 *
 * @author Spec Agent
 * @version 1.0.0
 * @see com.xxx.it.works.wecode.v2.modules.card.client.CardServicePeriodDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardSettingResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 定期失效时间（天）
     *
     * <p>可能为任意整数（例如系统默认 14），前端展示实际值。</p>
     * <p>用户可配置范围为 1~7，超过此范围的值前端编辑态会自动裁剪到 max。</p>
     */
    private Integer expirationDays;

    /**
     * 定期删除时间（天）
     *
     * <p>可能为任意整数（例如系统默认 7），前端展示实际值。</p>
     * <p>用户可配置范围为 1~30，超过此范围的值前端编辑态会自动裁剪到 max。</p>
     */
    private Integer deletionDays;
}
