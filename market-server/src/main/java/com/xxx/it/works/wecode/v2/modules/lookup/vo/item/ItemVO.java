package com.xxx.it.works.wecode.v2.modules.lookup.vo.item;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * LookUp项详情VO
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Schema(description = "LookUp项详情")
public class ItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项ID
     */
    @Schema(description = "项ID",
            example = "1")
    private Long itemId;

    /**
     * 分类ID
     */
    @Schema(description = "分类ID",
            example = "1")
    private Long classifyId;

    /**
     * 分类名称
     */
    @Schema(description = "分类名称",
            example = "用户类型")
    private String classifyName;

    /**
     * 项编码
     */
    @Schema(description = "项编码",
            example = "ADMIN")
    private String itemCode;

    /**
     * 项名称
     */
    @Schema(description = "项名称",
            example = "管理员")
    private String itemName;

    /**
     * 项值
     */
    @Schema(description = "项值",
            example = "1")
    private String itemValue;

    /**
     * 排序序号
     */
    @Schema(description = "排序序号",
            example = "1")
    private Integer itemIndex;

    /**
     * 项描述
     */
    @Schema(description = "项描述",
            example = "系统管理员")
    private String itemDesc;

    /**
     * 扩展属性1
     */
    @Schema(description = "扩展属性1",
            example = "super")
    private String itemAttr1;

    /**
     * 扩展属性2
     */
    @Schema(description = "扩展属性2")
    private String itemAttr2;

    /**
     * 扩展属性3
     */
    @Schema(description = "扩展属性3")
    private String itemAttr3;

    /**
     * 扩展属性4
     */
    @Schema(description = "扩展属性4")
    private String itemAttr4;

    /**
     * 扩展属性5
     */
    @Schema(description = "扩展属性5")
    private String itemAttr5;

    /**
     * 扩展属性6
     */
    @Schema(description = "扩展属性6")
    private String itemAttr6;

    /**
     * 状态：0-失效，1-有效
     */
    @Schema(description = "状态：0-失效，1-有效",
            example = "1")
    private Integer status;

    /**
     * 创建人
     */
    @Schema(description = "创建人",
            example = "admin")
    private String createBy;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间",
            example = "2024-01-15 10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;

    /**
     * 最后更新人
     */
    @Schema(description = "最后更新人",
            example = "admin")
    private String lastUpdateBy;

    /**
     * 最后更新时间
     */
    @Schema(description = "最后更新时间",
            example = "2024-01-15 10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date lastUpdateTime;
}
