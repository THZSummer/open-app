package com.xxx.api.modules.app.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AppEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String appId;
    private String tenantId;
    private String iconId;
    private String appNameCn;
    private String appNameEn;
    private String appDescCn;
    private String appDescEn;
    private Integer appType;
    private Integer appSubType;
    private Integer status;
    private String createBy;
    private LocalDateTime createTime;
    private String lastUpdateBy;
    private LocalDateTime lastUpdateTime;
}
