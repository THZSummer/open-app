package com.xxx.it.works.wecode.v2.modules.app.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用实体
 *
 * <p>对应表 openplatform_app_t</p>
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Data
public class App implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 应用ID（业务ID，对外暴露）
     */
    private String appId;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 图标文件ID
     */
    private String iconId;

    /**
     * 应用中文名
     */
    private String appNameCn;

    /**
     * 应用英文名
     */
    private String appNameEn;

    /**
     * 应用中文描述
     */
    private String appDescCn;

    /**
     * 应用英文描述
     */
    private String appDescEn;

    /**
     * 应用类型（参见 AppTypeEnum）
     */
    private Integer appType;

    /**
     * 应用子类型（参见 AppSubTypeEnum）
     */
    private Integer appSubType;

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
