package com.xxx.open.modules.permission.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 权限申请请求
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class PermissionSubscribeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 权限ID列表（支持批量提交）
     */
    @NotEmpty(message = "权限ID列表不能为空")
    private List<String> permissionIds;
}
