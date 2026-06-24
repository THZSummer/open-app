package com.xxx.it.works.wecode.v2.modules.ability.controller;

import com.xxx.it.works.wecode.v2.common.annotation.AuditLog;
import com.xxx.it.works.wecode.v2.common.enums.OperateEnum;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.ability.dto.AddAbilityRequest;
import com.xxx.it.works.wecode.v2.modules.ability.service.AbilityService;
import com.xxx.it.works.wecode.v2.modules.ability.vo.AbilityVO;
import com.xxx.it.works.wecode.v2.modules.ability.vo.AppAbilityDetailVO;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 能力管理控制器
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Slf4j
@RestController
@Validated
@RequestMapping("/service/open/v2/ability")
public class AbilityController {

    @Autowired
    private AbilityService abilityService;

    /**
     * 3.1 能力列表
     */
    @GetMapping("/list")
    public ApiResponse<List<AbilityVO>> getAbilityList(
            @RequestParam @NotBlank String appId) {
        List<AbilityVO> list = abilityService.getAbilityList(appId);
        return ApiResponse.success(list);
    }

    /**
     * 3.2 添加能力
     */
    @AuditLog(value = OperateEnum.ADD_APP_ABILITY)
    @PostMapping("")
    public ApiResponse<Void> addAbility(
            @RequestParam @NotBlank String appId,
            @RequestBody @Validated AddAbilityRequest request) {
        abilityService.addAbility(appId, request);
        return ApiResponse.success();
    }

    /**
     * 3.3 已订阅能力列表
     */
    @GetMapping("/subscribed")
    public ApiResponse<List<AppAbilityDetailVO>> getSubscribedAbilities(
            @RequestParam @NotBlank String appId) {
        List<AppAbilityDetailVO> list = abilityService.getSubscribedAbilities(appId);
        return ApiResponse.success(list);
    }
}
