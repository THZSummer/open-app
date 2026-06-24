package com.xxx.it.works.wecode.v2.modules.employee.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 人员信息实体
 *
 * <p>对应表 openplatform_employee_t，存储 welinkId → w3Account 等人员映射</p>
 *
 * @author SDDU Build Agent
 */
@Data
public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String welinkId;
    private String w3Account;
    private String chineseName;
    private String englishName;
    private String department;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime lastUpdateTime;
}
