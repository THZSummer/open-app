package com.xxx.it.works.wecode.v2.modules.lookup.vo.classify;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 分类详情VO
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Schema(description = "分类详情")
public class ClassifyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分类ID
     */
    @Schema(description = "分类ID",
            example = "1")
    private String classifyId;

    /**
     * 分类编码
     */
    @Schema(description = "分类编码",
            example = "USER_TYPE")
    private String classifyCode;

    /**
     * 分类名称
     */
    @Schema(description = "分类名称",
            example = "用户类型")
    private String classifyName;

    /**
     * 路径
     */
    @Schema(description = "路径",
            example = "system/user")
    private String path;

    /**
     * 分类描述
     */
    @Schema(description = "分类描述",
            example = "用户身份分类")
    private String classifyDesc;

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
            example = "2024-01-20 11:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date lastUpdateTime;
}
