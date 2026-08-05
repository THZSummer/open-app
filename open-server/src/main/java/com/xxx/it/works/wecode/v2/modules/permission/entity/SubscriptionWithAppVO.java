package com.xxx.it.works.wecode.v2.modules.permission.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SubscriptionWithAppVO extends Subscription {

    private static final long serialVersionUID = 1L;

    private String appNameCn;
    private String hisAppId;
}
