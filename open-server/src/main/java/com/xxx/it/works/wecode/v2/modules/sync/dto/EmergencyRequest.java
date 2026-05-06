package com.xxx.it.works.wecode.v2.modules.sync.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 应急接口请求
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class EmergencyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订阅关系数据列表
     */
    private List<SubscriptionData> subscriptions;
}
