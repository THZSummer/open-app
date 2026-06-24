package com.xxx.it.works.wecode.v2.modules.flow.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 连接流复制响应
 * <p>
 * API #21: POST /service/open/v2/flows/{flowId}/copy
 * 复制全部版本历史
 * </p>
 */
@Data
@Builder
public class FlowCopyResponse {

    /** 新连接流ID（String 格式） */
    private String flowId;

    /** 新连接流名称 */
    private String nameCn;

    /** 新连接流英文名称 */
    private String nameEn;

    /** 生命周期状态 */
    private Integer lifecycleStatus;

    /** 复制的版本数量 */
    private int versionCount;

    /** 提示信息 */
    private String message;
}
