package com.xxx.it.works.wecode.v2.modules.card.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 卡片周期更新请求
 *
 * <p>用于 PUT /service/open/v2/apps/{appId}/card-settings 请求体。</p>
 * <p>通过 {@code periodType} 区分更新的是"失效周期"还是"删除周期"，
 * {@code periodDays} 携带具体天数。</p>
 *
 * <p><b>校验规则（动态，按 periodType 决定范围）：</b></p>
 * <ul>
 *   <li>{@code periodType = 1}（失效）：{@code 1 ≤ periodDays ≤ 7}</li>
 *   <li>{@code periodType = 0}（删除）：{@code 1 ≤ periodDays ≤ 30}</li>
 * </ul>
 *
 * <p>注：范围校验依赖 {@code periodType}，无法用静态 @Min/@Max 表达，
 * 由 {@code CardSettingService} 做动态校验。</p>
 *
 * @author Spec Agent
 * @version 1.0.0
 */
@Data
public class UpdateCardPeriodRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 周期类型
     *
     * <ul>
     *   <li>{@code 0} = 定期删除周期（范围 1~30 天）</li>
     *   <li>{@code 1} = 定期失效周期（范围 1~7 天）</li>
     * </ul>
     */
    @NotNull(message = "periodType 不能为空")
    private Integer periodType;

    /**
     * 周期天数
     *
     * <p>具体范围由 {@code periodType} 决定（见类级注释）。</p>
     */
    @NotNull(message = "periodDays 不能为空")
    private Integer periodDays;

    // ===== 常量 =====

    /** 定期删除周期 */
    public static final int PERIOD_TYPE_DELETION = 0;

    /** 定期失效周期 */
    public static final int PERIOD_TYPE_EXPIRATION = 1;

    /** 失效周期下限 */
    public static final int EXPIRATION_MIN = 1;

    /** 失效周期上限 */
    public static final int EXPIRATION_MAX = 7;

    /** 删除周期下限 */
    public static final int DELETION_MIN = 1;

    /** 删除周期上限 */
    public static final int DELETION_MAX = 30;
}
