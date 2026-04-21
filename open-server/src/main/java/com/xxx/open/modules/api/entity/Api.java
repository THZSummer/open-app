package com.xxx.open.modules.api.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * API 资源实体
 * 
 * <p>对应表 openplatform_v2_api_t</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class Api implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * API ID
     */
    private Long id;

    /**
     * 中文名称
     */
    private String nameCn;

    /**
     * 英文名称
     */
    private String nameEn;

    /**
     * API 路径
     */
    private String path;

    /**
     * HTTP 方法
     */
    private String method;

    /**
     * 状态：0=草稿, 1=待审, 2=已发布, 3=已下线
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
