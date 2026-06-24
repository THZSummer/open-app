package com.xxx.it.works.wecode.v2.modules.member.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 成员类型枚举
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor
public enum MemberTypeEnum {

    /**
     * 开发者
     */
    DEVELOPER(0, "开发者", 1),

    /**
     * 管理员
     */
    ADMIN(2, "管理员", 2),

    /**
     * Owner
     */
    OWNER(1, "Owner", 3);

    /**
     * 枚举编码（数据库存储值）
     */
    private final int code;
    /**
     * 中文描述
     */
    private final String desc;
    /**
     * 角色优先级：Owner(3) > Admin(2) > Developer(1)
     */
    private final int priority;

    /**
     * 根据 code 查找枚举，未匹配返回 null
     */
    public static MemberTypeEnum fromCode(int code) {
        for (MemberTypeEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }
}
