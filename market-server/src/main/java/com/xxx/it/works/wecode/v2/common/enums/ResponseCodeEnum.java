package com.xxx.it.works.wecode.v2.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应码枚举
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum {

    // 成功
    SUCCESS("200", "操作成功", "Success"),

    // 客户端错误 - 参数
    PARAM_ERROR("400", "参数错误", "Parameter error"),

    // 业务错误 - 状态问题
    STATUS_CANNOT_DELETE("40003", "状态为有效，无法删除。请先将数据设置为失效状态", "Status is effective, cannot delete. Please set status to ineffective first"),
    ITEM_STATUS_CANNOT_DELETE("40004", "状态为有效，无法删除。请先将项设置为失效状态", "Status is effective, cannot delete. Please set status to ineffective first"),

    // 资源不存在
    NOT_FOUND("40401", "资源不存在", "Resource not found"),
    ITEM_NOT_FOUND("40402", "项不存在", "Item not found"),

    // 资源已存在
    ALREADY_EXISTS("40901", "资源已存在", "Resource already exists"),
    ITEM_CODE_EXISTS("40902", "项编码已存在", "Item code already exists"),

    // SDK版本 - 状态流转
    SDK_VERSION_DEPRECATED_CANNOT_EDIT("40005", "已废弃版本不可编辑", "Deprecated version cannot be edited"),
    SDK_VERSION_ALREADY_DEPRECATED("40006", "该版本已是废弃状态", "Version is already deprecated"),
    SDK_VERSION_ALREADY_PUBLISHED("40007", "该版本已是发布状态", "Version is already published"),

    // SDK版本 - 资源不存在
    SDK_VERSION_NOT_FOUND("40403", "SDK版本不存在", "SDK version not found"),
    SDK_PERMISSION_NOT_FOUND("40404", "关联的权限不存在", "Related permission not found"),

    // SDK版本 - 资源已存在
    SDK_VERSION_ALREADY_EXISTS("40903", "该版本号与权限的组合已存在", "SDK version with this permission already exists"),

    // 服务器错误
    INTERNAL_ERROR("50000", "服务器内部错误", "Internal server error");

    /**
     * 响应码
     */
    private final String code;

    /**
     * 中文消息
     */
    private final String messageZh;

    /**
     * 英文消息
     */
    private final String messageEn;
}