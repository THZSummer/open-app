package com.xxx.it.works.wecode.v2.modules.chatbotbindtab.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 通讯录 API 响应 DTO
 *
 * @author SDDU Build Agent
 * @version 2.0.0
 */
@Data
public class WeContactResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 响应码："0"=成功 */
    private String code;

    /** 响应消息 */
    private String message;

    /** 用户信息列表 */
    private List<UserInfo> users;

    /**
     * 用户信息
     */
    @Data
    public static class UserInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 个人账号 */
        private String personAccount;

        /** 工作 ID */
        private String workId;

        /** 中文名 */
        private String chineseName;

        /** 英文名 */
        private String englishName;

        /** 用户类型：4=机器人, 5=业务助手, 10=个人助手 */
        private Integer userType;

        /** 用户状态 */
        private Integer userStatus;
    }
}
