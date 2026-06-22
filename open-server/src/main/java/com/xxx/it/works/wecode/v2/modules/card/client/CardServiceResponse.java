package com.xxx.it.works.wecode.v2.modules.card.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 卡片服务统一响应封装
 *
 * <p>卡片服务查询和修改接口的响应都遵循此结构：</p>
 * <ul>
 *   <li>成功：{@code status=1, data} 有值，{@code error=null}</li>
 *   <li>失败：{@code status=0, data=null, error} 有值</li>
 * </ul>
 *
 * <p>用法示例：</p>
 * <pre>
 * CardServiceResponse&lt;CardServicePeriodDTO&gt; resp = cardServiceClient.queryCardPeriod(tenantId, clientId);
 * if (!resp.isSuccess()) {
 *     CardServiceError err = resp.getError();
 *     throw new BusinessException(String.valueOf(err.getCode()), err.getUserMessageZh(), err.getUserMessageEn());
 * }
 * CardServicePeriodDTO data = resp.getData();
 * </pre>
 *
 * @param <T> data 字段的实际类型（查询为 {@link CardServicePeriodDTO}，修改为 {@link String}）
 * @author Spec Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardServiceResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态码：1=成功，0=失败
     */
    private Integer status;

    /**
     * 成功时有值，失败时为 null
     */
    private T data;

    /**
     * 失败时有值，成功时为 null
     */
    private CardServiceError error;

    /**
     * 判断响应是否成功
     */
    public boolean isSuccess() {
        return status != null && status == 1;
    }
}
