package com.xxx.it.works.wecode.v2.modules.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 转移 Owner 请求
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Data
public class TransferOwnerRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "目标账号不能为空")
    private String toAccountId;
}
