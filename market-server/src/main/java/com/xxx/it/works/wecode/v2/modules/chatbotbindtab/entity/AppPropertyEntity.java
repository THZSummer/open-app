package com.xxx.it.works.wecode.v2.modules.chatbotbindtab.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 应用属性实体（对应表 openplatform_app_p_t）
 *
 * <p>DB 主键为雪花 ID（Long），返回前端时必须转为 String 防止 JS 精度丢失</p>
 *
 * @author SDDU Build Agent
 * @version 2.0.0
 */
@Data
public class AppPropertyEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（雪花 ID） */
    private Long id;

    /** 应用主键 ID（FK -> openplatform_app_t.id） */
    private Long parentId;

    /** 属性键 */
    private String propertyName;

    /** 属性值 */
    private String propertyValue;

    /** 租户 ID */
    private String tenantId;

    /** 状态：0=失效 1=有效 */
    private Integer status;

    /** 创建时间 */
    private Date createTime;

    /** 创建人 */
    private String createBy;

    /** 最后更新时间 */
    private Date lastUpdateTime;

    /** 最后更新人 */
    private String lastUpdateBy;
}
