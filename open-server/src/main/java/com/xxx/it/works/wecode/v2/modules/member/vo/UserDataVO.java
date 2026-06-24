package com.xxx.it.works.wecode.v2.modules.member.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户搜索结果 VO
 *
 * @author SDDU Build Agent
 * @date 2026-06-14
 */
@Data
public class UserDataVO implements Serializable {

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
     * 部门名称
     */
    private String deptName;

    /**
     * W3 账号
     */
    private String w3Account;
}
