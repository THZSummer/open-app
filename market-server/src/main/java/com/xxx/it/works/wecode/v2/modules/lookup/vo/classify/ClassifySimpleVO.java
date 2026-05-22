package com.xxx.it.works.wecode.v2.modules.lookup.vo.classify;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 分类简要信息VO（用于在LookUp项列表中展示）
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Schema(description = "分类简要信息")
public class ClassifySimpleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分类ID
     */
    @Schema(description = "分类ID",
            example = "1")
    private Long classifyId;

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
}
