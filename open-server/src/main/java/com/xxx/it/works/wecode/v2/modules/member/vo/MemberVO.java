package com.xxx.it.works.wecode.v2.modules.member.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 成员 VO
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Data
public class MemberVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    private String id;

    /**
     * 账号ID
     */
    private String accountId;

    /**
     * W3 账号
     */
    private String w3Account;

    /**
     * 中文名
     */
    private String memberNameCn;

    /**
     * 英文名
     */
    private String memberNameEn;

    /**
     * 成员类型
     */
    private Integer memberType;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
