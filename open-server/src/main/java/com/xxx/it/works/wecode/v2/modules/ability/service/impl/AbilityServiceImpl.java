package com.xxx.it.works.wecode.v2.modules.ability.service.impl;

import com.xxx.it.works.wecode.v2.common.constants.CommonConstants;
import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.enums.ResponseCodeEnum;
import com.xxx.it.works.wecode.v2.common.enums.StatusEnum;
import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.common.file.service.FileV2Service;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.modules.ability.constants.AbilityPropertyConstants;
import com.xxx.it.works.wecode.v2.modules.ability.dto.AddAbilityRequest;
import com.xxx.it.works.wecode.v2.modules.ability.entity.Ability;
import com.xxx.it.works.wecode.v2.modules.ability.entity.AbilityProperty;
import com.xxx.it.works.wecode.v2.modules.ability.entity.AppAbilityRelation;
import com.xxx.it.works.wecode.v2.modules.ability.enums.AbilityTypeEnum;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AbilityMapper;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AbilityPropertyMapper;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AppAbilityRelationMapper;
import com.xxx.it.works.wecode.v2.modules.ability.service.AbilityService;
import com.xxx.it.works.wecode.v2.modules.ability.vo.AbilityVO;
import com.xxx.it.works.wecode.v2.modules.ability.vo.AppAbilityDetailVO;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContext;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContextResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 能力服务实现
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Slf4j
@Service
public class AbilityServiceImpl implements AbilityService {

    @Autowired
    private AppAbilityRelationMapper appAbilityRelationMapper;

    @Autowired
    private AbilityMapper abilityMapper;

    @Autowired
    private AbilityPropertyMapper abilityPropertyMapper;

    @Autowired
    private AppContextResolver appContextResolver;

    @Autowired
    private FileV2Service fileV2Service;

    @Autowired
    private IdGeneratorStrategy idGenerator;

    @Override
    public List<AbilityVO> getAbilityList(String appId) {
        // 校验应用存在 + 当前用户是成员（resolveAndValidate 内含）
        AppContext ctx = appContextResolver.resolveAndValidate(appId);
        Long internalAppId = ctx.getInternalId();

        // 从能力主表查询所有能力（过滤掉应用入群通知，不在列表展示）
        List<Ability> abilities = abilityMapper.selectAll().stream()
                .filter(a -> Objects.nonNull(a.getAbilityType()) && !Objects.equals(a.getAbilityType(), AbilityTypeEnum.GROUP_JOIN_NOTIFICATION.getCode()))
                .toList();

        // 批量查询所有能力的属性（icon 等）
        List<Long> abilityIds = abilities.stream().map(Ability::getId).collect(Collectors.toList());
        Map<Long, List<AbilityProperty>> propsMap = loadPropsMap(abilityIds);

        // 查询当前应用已订阅的能力类型
        List<AppAbilityRelation> subscribed = appAbilityRelationMapper.selectByAppId(internalAppId);
        Set<Integer> subscribedTypes = subscribed.stream()
                .map(AppAbilityRelation::getAbilityType)
                .collect(Collectors.toSet());

        List<AbilityVO> list = new ArrayList<>();
        for (Ability ability : abilities) {
            AbilityVO vo = new AbilityVO();
            vo.setAbilityId(String.valueOf(ability.getId()));
            vo.setAbilityType(ability.getAbilityType());
            vo.setNameCn(ability.getAbilityNameCn());
            vo.setNameEn(ability.getAbilityNameEn());
            vo.setDescCn(ability.getAbilityDescCn());
            vo.setDescEn(ability.getAbilityDescEn());
            vo.setSubscribed(subscribedTypes.contains(ability.getAbilityType()));
            vo.setOrderNum(ability.getOrderNum());

            List<AbilityProperty> props = propsMap.getOrDefault(ability.getId(), Collections.emptyList());
            for (AbilityProperty p : props) {
                if (AbilityPropertyConstants.PROP_ICON.equals(p.getPropertyName()) && StringUtils.hasText(p.getPropertyValue())) {
                    vo.setIconUrl(fileV2Service.queryFileUrl(p.getPropertyValue()));
                } else if (AbilityPropertyConstants.PROP_ILLUSTRATION.equals(p.getPropertyName()) && StringUtils.hasText(p.getPropertyValue())) {
                    vo.setDiagramUrl(fileV2Service.queryFileUrl(p.getPropertyValue()));
                }
            }

            list.add(vo);
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addAbility(String appId, AddAbilityRequest request) {
        AppContext ctx = appContextResolver.resolveAndValidate(appId);
        Long internalAppId = ctx.getInternalId();

        Integer abilityType = request.getAbilityType();
        if (!AbilityTypeEnum.isValidCode(abilityType)) {
            throw new BusinessException(
                    ResponseCodeEnum.ABILITY_TYPE_INVALID.getCode(),
                    ResponseCodeEnum.ABILITY_TYPE_INVALID.getMessageZh(),
                    ResponseCodeEnum.ABILITY_TYPE_INVALID.getMessageEn()
            );
        }

        // 校验是否已订阅
        if (Objects.nonNull(appAbilityRelationMapper.selectByAppIdAndAbilityType(internalAppId, abilityType))) {
            throw new BusinessException(
                    ResponseCodeEnum.ABILITY_ALREADY_SUBSCRIBED.getCode(),
                    ResponseCodeEnum.ABILITY_ALREADY_SUBSCRIBED.getMessageZh(),
                    ResponseCodeEnum.ABILITY_ALREADY_SUBSCRIBED.getMessageEn()
            );
        }

        // 从能力主表查询 abilityId
        Ability ability = abilityMapper.selectAll().stream()
                .filter(a -> a.getAbilityType().equals(abilityType))
                .findFirst().orElse(null);
        if (ability == null) {
            throw new BusinessException(
                    ResponseCodeEnum.ABILITY_TYPE_INVALID.getCode(),
                    ResponseCodeEnum.ABILITY_TYPE_INVALID.getMessageZh(),
                    ResponseCodeEnum.ABILITY_TYPE_INVALID.getMessageEn()
            );
        }
        Long abilityId = ability.getId();

        AppAbilityRelation relation = new AppAbilityRelation();
        relation.setId(idGenerator.nextId());
        relation.setAppId(internalAppId);
        relation.setAbilityId(abilityId);
        relation.setAbilityType(abilityType);
        relation.setTenantId(CommonConstants.DEFAULT_TENANT_ID);
        String currentUser = UserContextHolder.getUserId();
        relation.setStatus(StatusEnum.ENABLED.getCode());
        relation.setCreateBy(currentUser);
        relation.setLastUpdateBy(currentUser);
        appAbilityRelationMapper.insert(relation);
        log.info("Ability subscribed successfully: appId={}, abilityType={}", appId, abilityType);

        // 订阅能力后自动订阅 API/事件权限（预留扩展点）
        autoSubscribeAfterAbility(appId, abilityType);
    }

    /**
     * 订阅能力后自动订阅 API/事件权限
     *
     * <p>预留扩展点，当前为空实现，后续根据能力类型自动订阅对应的 API/事件权限</p>
     *
     * @param appId       外部应用 ID
     * @param abilityType 能力类型（见 {@link AbilityTypeEnum}）
     */
    private void autoSubscribeAfterAbility(String appId, Integer abilityType) {
        // TODO 后续实现：根据 abilityType 自动订阅 API/事件权限
    }

    @Override
    public List<AppAbilityDetailVO> getSubscribedAbilities(String appId) {
        AppContext ctx = appContextResolver.resolveAndValidate(appId);
        Long internalAppId = ctx.getInternalId();

        // 查询已订阅的关联关系（过滤掉应用入群通知，不在列表展示）
        List<AppAbilityRelation> relations = appAbilityRelationMapper.selectByAppId(internalAppId).stream()
                .filter(r -> Objects.nonNull(r.getAbilityType()) && !Objects.equals(r.getAbilityType(), AbilityTypeEnum.GROUP_JOIN_NOTIFICATION.getCode()))
                .collect(Collectors.toList());

        // 从能力主表查所有能力，构建 type -> Ability 映射
        List<Ability> allAbilities = abilityMapper.selectAll();
        Map<Integer, Ability> typeMap = allAbilities.stream()
                .collect(Collectors.toMap(Ability::getAbilityType, a -> a, (a, b) -> a));

        // 批量查询所有能力的属性（icon 等）
        List<Long> abilityIds = allAbilities.stream().map(Ability::getId).collect(Collectors.toList());
        Map<Long, List<AbilityProperty>> propsMap = loadPropsMap(abilityIds);

        List<AppAbilityDetailVO> list = new ArrayList<>();
        for (AppAbilityRelation r : relations) {
            AppAbilityDetailVO vo = new AppAbilityDetailVO();
            vo.setId(String.valueOf(r.getId()));
            vo.setAbilityType(r.getAbilityType());

            Ability ability = typeMap.get(r.getAbilityType());
            if (Objects.isNull(ability)) {
                continue;
            }
            vo.setAbilityId(String.valueOf(ability.getId()));
            vo.setNameCn(ability.getAbilityNameCn());
            vo.setNameEn(ability.getAbilityNameEn());
            vo.setOrderNum(ability.getOrderNum());

            List<AbilityProperty> props = propsMap.getOrDefault(ability.getId(), Collections.emptyList());
            for (AbilityProperty p : props) {
                if (AbilityPropertyConstants.PROP_ICON.equals(p.getPropertyName()) && StringUtils.hasText(p.getPropertyValue())) {
                    vo.setIconUrl(fileV2Service.queryFileUrl(p.getPropertyValue()));
                }
            }

            list.add(vo);
        }
        // 按 VO 的 orderNum 排序
        list.sort(Comparator.comparingInt(AppAbilityDetailVO::getOrderNum));
        return list;
    }

    /**
     * 批量查询指定能力的属性，按 parentId 分组
     *
     * @param abilityIds 能力 ID 列表
     * @return parentId -> 属性列表 映射（空列表或 null 入参返回空 Map）
     */
    private Map<Long, List<AbilityProperty>> loadPropsMap(List<Long> abilityIds) {
        Map<Long, List<AbilityProperty>> propsMap = new HashMap<>();
        if (abilityIds != null && !abilityIds.isEmpty()) {
            List<AbilityProperty> allProps = abilityPropertyMapper.selectByParentIds(abilityIds);
            for (AbilityProperty p : allProps) {
                propsMap.computeIfAbsent(p.getParentId(), k -> new ArrayList<>()).add(p);
            }
        }
        return propsMap;
    }
}
