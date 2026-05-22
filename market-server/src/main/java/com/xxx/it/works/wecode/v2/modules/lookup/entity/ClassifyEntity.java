package com.xxx.it.works.wecode.v2.modules.lookup.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 分类实体类
 *
 * <p>对应表 openplatform_lookup_classify_t</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ClassifyEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分类ID，主键
     */
    private Long classifyId;

    /**
     * 分类编码
     */
    private String classifyCode;

    /**
     * 分类名称
     */
    private String classifyName;

    /**
     * 路径，用于层级归类
     */
    private String path;

    /**
     * 分类描述
     */
    private String classifyDesc;

    /**
     * 状态：0-失效，1-有效
     */
    private Integer status;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 最后更新人
     */
    private String lastUpdateBy;

    /**
     * 最后更新时间
     */
    private Date lastUpdateTime;

    /**
     * 项数量（非数据库字段，查询时统计）
     */
    private Integer itemCount;
}
