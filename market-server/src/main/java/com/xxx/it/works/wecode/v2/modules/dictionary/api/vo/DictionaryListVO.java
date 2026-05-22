package com.xxx.it.works.wecode.v2.modules.dictionary.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 数据字典列表VO
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Schema(description = "数据字典列表项")
public class DictionaryListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "主键ID",
            example = "1")
    private Long id;

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
     * 语言：1-中文，2-英文
     */
    @Schema(description = "语言：1-中文，2-英文",
            example = "1")
    private Integer language;

    /**
     * 语言名称
     */
    @Schema(description = "语言名称",
            example = "中文")
    private String languageName;

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
            example = "2024-01-20 11:00:00")
    private Date lastUpdateTime;
}
