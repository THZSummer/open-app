package com.xxx.it.works.wecode.v2.modules.chatbotbindtab.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 绑定/解绑机器人账号请求 DTO
 *
 * <p>accountId 后端通过通讯录 API 校验有效性，此处仅做非空校验</p>
 *
 * @author SDDU Build Agent
 * @version 2.0.0
 */
@Data
public class ChatbotBindRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 应用业务 ID（openplatform_app_t.app_id） */
    @NotBlank(message = "appId 不能为空")
    private String appId;

    /** 机器人账号 ID */
    @NotBlank(message = "accountId 不能为空")
    private String accountId;
}
