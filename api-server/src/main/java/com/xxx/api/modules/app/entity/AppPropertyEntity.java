package com.xxx.api.modules.app.entity;

import lombok.Data;
import java.io.Serializable;

@Data
public class AppPropertyEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long parentId;
    private String propertyName;
    private String propertyValue;
    private String tenantId;
    private Integer status;
}
