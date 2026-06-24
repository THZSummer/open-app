package com.xxx.it.works.wecode.v2.modules.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 更新应用请求
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Data
public class UpdateAppRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "应用中文名不能为空")
    @Size(max = 255, message = "应用中文名不能超过255个字符")
    private String nameCn;

    @NotBlank(message = "应用英文名不能为空")
    @Size(max = 255, message = "应用英文名不能超过255个字符")
    private String nameEn;

    @NotBlank(message = "应用图标不能为空")
    private String iconId;

    @Size(max = 2000, message = "应用中文描述不能超过2000个字符")
    private String descCn;

    @Size(max = 2000, message = "应用英文描述不能超过2000个字符")
    private String descEn;

    private List<String> diagramIdList;
}
