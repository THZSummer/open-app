package com.xxx.it.works.wecode.v2.modules.chatbotbindtab.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 机器人账号响应 VO（API 对外输出，所有 ID 为 String 类型）
 *
 * <p>与 DB Entity 严格分离：DB 用 Long，VO 用 String，防止 JS 精度丢失</p>
 *
 * @author SDDU Build Agent
 * @version 2.0.0
 */
@Data
public class ChatbotAccountVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 记录 ID（雪花 ID -> String） */
    private String id;

    /** 机器人账号 ID（property_value） */
    private String accountId;

    /** 绑定时间 yyyy-MM-dd HH:mm:ss */
    private String bindTime;

    /** 绑定操作人 */
    private String bindBy;
}
