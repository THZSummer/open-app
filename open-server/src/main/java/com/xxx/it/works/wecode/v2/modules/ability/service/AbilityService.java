package com.xxx.it.works.wecode.v2.modules.ability.service;

import com.xxx.it.works.wecode.v2.modules.ability.dto.AddAbilityRequest;
import com.xxx.it.works.wecode.v2.modules.ability.vo.AbilityVO;
import com.xxx.it.works.wecode.v2.modules.ability.vo.AppAbilityDetailVO;

import java.util.List;

/**
 * 能力服务接口
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
public interface AbilityService {

    List<AbilityVO> getAbilityList(String appId);

    void addAbility(String appId, AddAbilityRequest request);

    List<AppAbilityDetailVO> getSubscribedAbilities(String appId);
}
