package com.xxx.api.internal.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户角色查询请求
 *
 * <p>嵌入能力方查询用户在指定应用中的角色。
 * appId 和 hisAppId 至少二选一，同时传入时优先按 appId 匹配。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserRoleQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 平台应用ID（与 hisAppId 至少二选一）
     */
    private String appId;

    /**
     * 外部应用编码 hisAppId（与 appId 至少二选一）
     */
    private String hisAppId;

    /**
     * 用户账号（必填）
     */
    private String userAccount;
}
