package com.xxx.it.works.wecode.v2.modules.lookup.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * 分页请求基础DTO
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Schema(description = "分页请求参数")
public class PageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 页码，默认1
     */
    @Min(value = 1, message = "页码必须大于等于1")
    @Schema(description = "页码，默认1",
            defaultValue = "1",
            example = "1")
    private Integer pageNum = 1;

    /**
     * 每页条数，默认10，最大100
     */
    @Min(value = 1, message = "每页条数必须大于等于1")
    @Max(value = 100, message = "每页条数不能超过100")
    @Schema(description = "每页条数，默认10，最大100",
            defaultValue = "10",
            example = "10")
    private Integer pageSize = 10;
}
