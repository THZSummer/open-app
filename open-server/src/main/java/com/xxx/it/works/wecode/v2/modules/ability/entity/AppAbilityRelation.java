package com.xxx.it.works.wecode.v2.modules.ability.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用能力关联实体
 *
 * <p>对应表 openplatform_app_ability_relation_t</p>
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Data
public class AppAbilityRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 应用内部ID
     */
    private Long appId;

    /**
     * 能力ID
     */
    private Long abilityId;

    /**
     * 能力类型（参见 AbilityTypeEnum）
     */
    private Integer abilityType;

    /**
     * 租户ID
     */
    private String tenantId;

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
