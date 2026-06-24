package com.xxx.it.works.wecode.v2.modules.app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 绑定 EAMAP 请求
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Data
public class BindEamapRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "EAMAP编码不能为空")
    private String eamapAppCode;
}
