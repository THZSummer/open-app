package com.xxx.it.works.wecode.v2.modules.app.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用凭证实体
 *
 * <p>对应表 openplatform_app_identity_t</p>
 * <p>AK/SK 必须存储在此表中，而非属性表</p>
 *
 * @author SDDU Build Agent
 * @date 2026-06-07
 */
@Data
public class AppIdentity implements Serializable {

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
     * 公钥
     */
    private String publicKey;

    /**
     * 私钥（即 SK，Secret Key）
     */
    private String privateKey;

    /**
     * 密钥版本
     */
    private String keyVersion;

    /**
     * 工具包版本
     */
    private String kitVersion;

    /**
     * Access Key
     */
    private String ak;

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
