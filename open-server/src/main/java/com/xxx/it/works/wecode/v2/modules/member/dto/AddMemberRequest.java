package com.xxx.it.works.wecode.v2.modules.member.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 添加成员请求
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Data
public class AddMemberRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "成员账号列表不能为空")
    private List<String> accountIds;

    @NotNull(message = "角色不能为空")
    private Integer role;
}
