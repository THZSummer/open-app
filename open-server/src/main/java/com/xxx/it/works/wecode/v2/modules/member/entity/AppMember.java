package com.xxx.it.works.wecode.v2.modules.member.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用成员实体
 *
 * <p>对应表 openplatform_app_member_t</p>
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Data
public class AppMember implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 应用内部ID
     */
    private Long appId;

    /**
     * 成员中文名
     */
    private String memberNameCn;

    /**
     * 成员英文名
     */
    private String memberNameEn;

    /**
     * 账号ID（WeLink ID）
     */
    private String accountId;

    /**
     * 成员类型：0=Developer, 1=Owner, 2=Admin（参见 MemberTypeEnum）
     */
    private Integer memberType;

    /**
     * 状态：0=禁用, 1=启用（参见 StatusEnum）
     */
    private Integer status;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 最后更新人
     */
    private String lastUpdateBy;

    /**
     * 最后更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUpdateTime;
}
