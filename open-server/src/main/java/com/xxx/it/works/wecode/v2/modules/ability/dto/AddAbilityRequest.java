package com.xxx.it.works.wecode.v2.modules.ability.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 添加能力请求
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Data
public class AddAbilityRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "能力类型不能为空")
    private Integer abilityType;
}
