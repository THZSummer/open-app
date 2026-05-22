package com.xxx.it.works.wecode.v2.modules.lookup.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * LookUp项实体类
 *
 * <p>对应表 openplatform_lookup_item_t</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class LookUpItemEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项ID，主键
     */
    private Long itemId;

    /**
     * 分类ID，外键
     */
    private Long classifyId;

    /**
     * 项编码
     */
    private String itemCode;

    /**
     * 项名称
     */
    private String itemName;

    /**
     * 项值
     */
    private String itemValue;

    /**
     * 排序序号
     */
    private Integer itemIndex;

    /**
     * 项描述
     */
    private String itemDesc;

    /**
     * 扩展属性1
     */
    private String itemAttr1;

    /**
     * 扩展属性2
     */
    private String itemAttr2;

    /**
     * 扩展属性3
     */
    private String itemAttr3;

    /**
     * 扩展属性4
     */
    private String itemAttr4;

    /**
     * 扩展属性5
     */
    private String itemAttr5;

    /**
     * 扩展属性6
     */
    private String itemAttr6;

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
     * 分类名称（非数据库字段，查询时JOIN获取）
     */
    private String classifyName;
}
