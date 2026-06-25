package com.xxx.it.works.wecode.v2.modules.dictionary.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 数据字典详情VO
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Schema(description = "数据字典详情")
public class DictionaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "主键ID",
            example = "1")
    private String id;

    /**
     * 编码
     */
    @Schema(description = "编码",
            example = "USER_STATUS")
    private String code;

    /**
     * 名称
     */
    @Schema(description = "名称",
            example = "用户状态")
    private String name;

    /**
     * 值
     */
    @Schema(description = "值",
            example = "active")
    private String value;

    /**
     * 描述
     */
    @Schema(description = "描述",
            example = "用户账户状态字典")
    private String description;

    /**
     * 路径
     */
    @Schema(description = "路径",
            example = "system/user")
    private String path;

    /**
     * 状态：0-失效，1-有效
     */
    @Schema(description = "状态：0-失效，1-有效",
            example = "1")
    private Integer status;

    /**
     * 状态名称
     */
    @Schema(description = "状态名称",
            example = "有效")
    private String statusName;

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
            example = "2024-01-10 09:00:00")
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
            example = "2024-01-10 09:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date lastUpdateTime;
}