package com.xxx.it.works.wecode.v2.modules.app.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 员工信息 VO
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Data
public class EmployeeInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * WeLink ID
     */
    private String welinkId;

    /**
     * 中文名
     */
    private String memberNameCn;

    /**
     * 英文名
     */
    private String memberNameEn;

    /**
     * W3 账号
     */
    private String w3Account;
}
