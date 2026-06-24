package com.xxx.it.works.wecode.v2.modules.version.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用版本实体
 *
 * <p>对应表 openplatform_app_version_t</p>
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Data
public class AppVersion implements Serializable {

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
     * 版本中文描述
     */
    private String versionDescCn;

    /**
     * 版本英文描述
     */
    private String versionDescEn;

    /**
     * 版本号（语义化版本，如 1.0.0）
     */
    private String versionCode;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 状态：1=待发布, 2=审批中, 3=审批未通过, 4=已发布（参见 VersionStatusEnum）
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
