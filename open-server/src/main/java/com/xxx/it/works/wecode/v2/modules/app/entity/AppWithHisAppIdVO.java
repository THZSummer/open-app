package com.xxx.it.works.wecode.v2.modules.app.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AppWithHisAppIdVO extends App {

    private static final long serialVersionUID = 1L;

    private String hisAppId;
}
