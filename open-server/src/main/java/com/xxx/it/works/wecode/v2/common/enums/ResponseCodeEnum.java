package com.xxx.it.works.wecode.v2.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应码枚举
 *
 * <p>应用管理模块业务错误码定义</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum {

    // ===== 通用成功 =====
    SUCCESS("200", "成功", "success"),

    // ===== 客户端错误 4xx =====
    BAD_REQUEST("400", "请求参数错误", "Bad Request"),
    UNAUTHORIZED("401", "未授权", "Unauthorized"),
    FORBIDDEN("403", "禁止访问", "Forbidden"),
    NOT_FOUND("404", "资源不存在", "Not Found"),

    // ===== 应用管理业务错误 (1.x 接口) =====
    APP_NOT_FOUND("404100", "应用不存在", "Application not found"),
    APP_ACCESS_DENIED("403100", "无权访问应用", "No access to the application"),
    APP_NAME_DUPLICATED("409100", "应用名称已存在", "Application name already exists"),
    APP_PARAM_ERROR("400100", "应用参数错误", "Application parameter error"),
    EAMAP_ALREADY_BOUND("409102", "EAMAP已被其他应用绑定", "EAMAP already bound by another application"),
    EAMAP_NOT_FOUND("400103", "EAMAP编码不存在", "EAMAP code not found"),
    NOT_EAMAP_OWNER("403104", "当前用户不是EAMAP的owner", "Current user is not the EAMAP owner"),
    ICON_NOT_FOUND("400101", "图标ID不存在", "Icon ID not found"),
    VERIFY_TYPE_INVALID("400102", "认证方式非法", "Invalid verify type"),
    VERIFY_TYPE_MUTUALLY_EXCLUSIVE("400108", "SOAHeader和SOAURL不可同时选择", "SOAHeader and SOAURL cannot be selected at the same time"),
    VERIFY_TYPE_SINGLE_ONLY("400110", "当前仅支持选择一种认证方式", "Only one verify type is allowed"),
    API_SECRET_FORMAT_ERROR("400106", "apiSecret格式错误", "apiSecret format error"),
    API_SECRET_REQUIRED("400107", "数字签名必须输入apiSecret", "apiSecret is required for digital signature"),
    APP_TYPE_NOT_SUPPORTED("409103", "应用类型不支持此操作", "Application type does not support this operation"),
    APP_IDENTITY_NOT_FOUND("404101", "应用凭证不存在", "App identity not found"),
    EMPLOYEE_NOT_FOUND("404102", "员工信息不存在", "Employee info not found"),

    // ===== 成员管理业务错误 (2.x 接口) =====
    MEMBER_ACCOUNT_INVALID("400200", "成员账号无效", "Member account invalid"),
    NO_ADD_PERMISSION("403200", "当前用户角色无添加成员权限", "Current user role has no permission to add members"),
    MEMBER_ALREADY_EXISTS("409200", "成员已存在", "Member already exists"),
    MEMBER_NOT_FOUND("404200", "成员不存在", "Member not found"),
    CANNOT_DELETE_OWNER("409201", "不能删除Owner", "Cannot delete Owner"),
    NO_ADD_ROLE_PERMISSION("403201", "当前用户角色无添加该角色成员的权限", "No permission to add member with this role"),
    NO_DELETE_PERMISSION("403202", "当前用户角色无删除成员权限", "No permission to delete member"),
    NO_DELETE_ROLE_PERMISSION("403203", "当前用户角色无删除该角色成员的权限", "No permission to delete member with this role"),
    NO_TRANSFER_PERMISSION("403204", "仅Owner可操作", "Only the Owner can perform this action"),
    CANNOT_TRANSFER_TO_SELF("409202", "不能将Owner转移给自己", "Cannot transfer Owner to yourself"),
    TARGET_NOT_MEMBER("409203", "目标用户不是应用成员", "Target user is not an application member"),
    CANNOT_ADD_OWNER("409204", "不能直接添加Owner", "Cannot directly add Owner"),
    SOURCE_NOT_OWNER("409205", "源账户不是当前Owner", "Source account is not the current Owner"),
    MEMBER_ALREADY_HAS_ROLE("409206", "该成员已拥有此角色", "Member already has this role"),
    NO_MEMBER_OPERATION_PERMISSION("403207", "当前用户无权限执行此成员操作", "No permission to perform this member operation"),

    // ===== 能力管理业务错误 (3.x 接口) =====
    ABILITY_TYPE_INVALID("400104", "能力类型非法", "Invalid ability type"),
    ABILITY_ALREADY_SUBSCRIBED("409400", "能力已订阅", "Ability already subscribed"),
    NO_ABILITY_PERMISSION("403201", "非成员无权限", "No permission for ability operation"),

    // ===== 版本管理业务错误 (4.x 接口) =====
    VERSION_CODE_FORMAT_ERROR("400105", "版本号格式错误", "Version code format error"),
    VERSION_CODE_DUPLICATED("409300", "版本号已存在", "Version code already exists"),
    VERSION_PENDING_EXISTS("409301", "存在待发布、审批中或审批未通过的版本", "A pending, reviewing or rejected version already exists"),
    VERSION_STATUS_NOT_EDITABLE("409303", "当前状态非待发布，不可编辑", "Current status is not pending, cannot edit"),
    VERSION_DELETE_NOT_ALLOWED("409305", "仅待发布或审批未通过版本可删除", "Only pending or rejected versions can be deleted"),
    VERSION_CODE_NOT_INCREMENTAL("409306", "新版本的版本号必须比历史其他版本的版本号大", "The new version code must be greater than all existing version codes"),
    VERSION_NOT_FOUND("404300", "版本不存在", "Version not found"),
    VERSION_WITHDRAW_REVIEW_ONLY("409307", "仅审批中版本可撤回", "Only under-review versions can be withdrawn"),

    // ===== 服务端错误 5xx =====
    INTERNAL_ERROR("500", "服务器内部错误", "Internal Server Error"),
    EVENT_SEND_FAILED("500101", "事件发送失败", "Event send failed"),

    // ===== 文件服务业务错误 =====
    FILE_EMPTY("400300", "文件不能为空", "File cannot be empty"),
    FILE_DIR_CREATE_FAILED("500300", "创建上传目录失败", "Failed to create upload directory"),
    FILE_SAVE_FAILED("500301", "文件保存失败", "Failed to save file");

    private final String code;
    private final String messageZh;
    private final String messageEn;


    public String getCode() { return code; }
    public String getMessageZh() { return messageZh; }
    public String getMessageEn() { return messageEn; }
}
