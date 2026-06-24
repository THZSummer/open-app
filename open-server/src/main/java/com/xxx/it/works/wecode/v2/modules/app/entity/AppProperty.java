package com.xxx.it.works.wecode.v2.modules.app.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 应用属性实体
 *
 * <p>对应表 {@code openplatform_app_p_t}（KV 模式存储应用扩展属性）。</p>
 *
 * <p>本实体仅供只读查询使用（不建写入逻辑），主要场景：</p>
 * <ul>
 *   <li>卡片设置板块：按 {@code parent_id + property_name='eamap_app_code'} 查 {@code property_value}
 *       作为调用卡片服务时的 {@code clientId}</li>
 * </ul>
 *
 * <p>字段说明：</p>
 * <ul>
 *   <li>{@code parentId}：关联 {@code openplatform_app_t.id}（应用主键 ID）</li>
 *   <li>{@code propertyName}：属性名（如 {@code eamap_app_code}）</li>
 *   <li>{@code propertyValue}：属性值</li>
 *   <li>{@code status}：0=禁用，1=启用</li>
 * </ul>
 *
 * @author Spec Agent
 * @version 1.0.0
 */
@Data
public class AppProperty implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 属性 ID
     */
    private Long id;

    /**
     * 关联应用主表 ID（openplatform_app_t.id）
     */
    private Long parentId;

    /**
     * 属性名称（如 eamap_app_code）
     */
    private String propertyName;

    /**
     * 属性值
     */
    private String propertyValue;

    /**
     * 状态：0=禁用，1=启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 最后更新时间
     */
    private Date lastUpdateTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 最后更新人
     */
    private String lastUpdateBy;
}
