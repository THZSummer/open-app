package com.xxx.it.works.wecode.v2.modules.callback.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 回调详情响应
 *
 * <p>用于回调详情接口返回</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
public class CallbackResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 回调ID
     */
    private String id;

    /**
     * 中文名称
     */
    private String nameCn;

    /**
     * 英文名称
     */
    private String nameEn;

    /**
     * 所属分类ID
     */
    private String categoryId;

    /**
     * 所属分类名称
     */
    private String categoryName;

    /**
     * 状态：0=草稿, 1=待审, 2=已发布, 3=已下线
     */
    private Integer status;

    /**
     * 权限信息
     */
    private PermissionDto permission;

    /**
     * 扩展属性列表（KV 模式）
     */
    private List<CallbackPropertyDto> properties;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 最后更新时间
     */
    private Date lastUpdateTime;

    /**
     * 最后更新人
     */
    private String lastUpdateBy;

    public CallbackResponse(String id, String nameCn, String nameEn, String categoryId, String categoryName, Integer status, PermissionDto permission, List<CallbackPropertyDto> properties, Date createTime, String createBy, Date lastUpdateTime, String lastUpdateBy) {
        this.id = id;
        this.nameCn = nameCn;
        this.nameEn = nameEn;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.status = status;
        this.permission = permission;
        this.properties = properties;
        this.createTime = createTime;
        this.createBy = createBy;
        this.lastUpdateTime = lastUpdateTime;
        this.lastUpdateBy = lastUpdateBy;
    }
}
