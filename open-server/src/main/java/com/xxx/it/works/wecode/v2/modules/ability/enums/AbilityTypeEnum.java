package com.xxx.it.works.wecode.v2.modules.ability.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 能力类型枚举
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor
public enum AbilityTypeEnum {

    GROUP_TOP(1, "群置顶服务"),
    GROUP_NOTIFICATION(2, "群通知服务"),
    LINK_ENHANCEMENT(3, "链接增强服务"),
    P2P_NOTIFICATION(4, "点对点通知服务"),
    WE_CODE(5, "we码"),
    GROUP_JOIN_NOTIFICATION(6, "应用入群通知"),
    ASSISTANT_SQUARE_CARD(7, "助手广场卡片");

    private final int code;
    private final String desc;

    public static boolean isValidCode(Integer code) {
        if (code == null) {
            return false;
        }
        for (AbilityTypeEnum value : values()) {
            if (value.code == code) {
                return true;
            }
        }
        return false;
    }
}
