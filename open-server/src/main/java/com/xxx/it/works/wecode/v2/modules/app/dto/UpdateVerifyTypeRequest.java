package com.xxx.it.works.wecode.v2.modules.app.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 更新认证方式请求
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Data
public class UpdateVerifyTypeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "认证方式不能为空")
    private List<Integer> verifyType;

    private String apiSecret;
}
