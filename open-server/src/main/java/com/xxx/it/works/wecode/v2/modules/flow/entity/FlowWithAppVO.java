package com.xxx.it.works.wecode.v2.modules.flow.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FlowWithAppVO extends Flow {

    private static final long serialVersionUID = 1L;

    private String appNameCn;
    private String hisAppId;
}
