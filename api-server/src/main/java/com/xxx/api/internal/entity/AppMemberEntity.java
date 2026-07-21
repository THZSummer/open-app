package com.xxx.api.internal.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AppMemberEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String tenantId;
    private Long appId;
    private String memberNameCn;
    private String memberNameEn;
    private String accountId;
    private Integer memberType;
    private Integer status;
    private String createBy;
    private LocalDateTime createTime;
    private String lastUpdateBy;
    private LocalDateTime lastUpdateTime;
}
