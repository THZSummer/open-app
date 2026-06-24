package com.xxx.it.works.wecode.v2.modules.app.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * EAMAP 实体
 *
 * <p>对应表 openplatform_eamap_t</p>
 */
@Data
public class Eamap implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * EAMAP编码
     */
    private String eamapAppCode;

    /**
     * 中文名称
     */
    private String nameCn;

    /**
     * 英文名称
     */
    private String nameEn;

    /**
     * 负责人账号ID
     */
    private String ownerAccountId;

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
