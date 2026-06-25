package com.xxx.it.works.wecode.v2.modules.approval.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AppVersionStatusEnum {

    DRAFT(1, "草稿"),
    IN_PROCESS(2, "流程中（待审批）"),
    REJECTED(3, "驳回"),
    APPROVED(4, "审批通过"),
    CANCELLED(5, "取消申请");

    private final int value;
    private final String description;

    public static AppVersionStatusEnum fromValue(int value) {
        for (AppVersionStatusEnum status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        return null;
    }
}
