package com.xxx.it.works.wecode.v2.modules.ability.service.impl;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.ability.dto.admin.AdminAbilityCreateRequest;
import com.xxx.it.works.wecode.v2.modules.ability.dto.admin.AdminAbilityListRequest;
import com.xxx.it.works.wecode.v2.modules.ability.entity.AbilityEntity;
import com.xxx.it.works.wecode.v2.modules.ability.entity.AbilityProperty;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AbilityMapper;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AbilityPropertyMapper;
import com.xxx.it.works.wecode.v2.modules.ability.service.AdminAbilityService;
import com.xxx.it.works.wecode.v2.modules.ability.common.AbilityPropertyEnum;
import com.xxx.it.works.wecode.v2.modules.file.service.CommonFileService;
import com.xxx.it.works.wecode.v2.modules.ability.vo.admin.AdminAbilityVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理面能力服务实现
 *
 * <p>实现能力列表的分页查询、关键字搜索、动态排序、
 * 以及从属性表关联查询图标/示意图 URL。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Service
public class AdminAbilityServiceImpl implements AdminAbilityService {

    private final AbilityMapper abilityMapper;

    private final AbilityPropertyMapper abilityPropertyMapper;

    private final CommonFileService commonFileService;

    public AdminAbilityServiceImpl(AbilityMapper abilityMapper,
                                    AbilityPropertyMapper abilityPropertyMapper,
                                    CommonFileService commonFileService) {
        this.abilityMapper = abilityMapper;
        this.abilityPropertyMapper = abilityPropertyMapper;
        this.commonFileService = commonFileService;
    }

    @Override
    public ApiResponse<List<AdminAbilityVO>> list(AdminAbilityListRequest request) {
        try {
            // 1. 校验参数
            int curPage = request.getCurPage() != null ? Math.max(1, request.getCurPage()) : 1;
            int pageSize = request.getPageSize() != null ? Math.min(100, Math.max(1, request.getPageSize())) : 20;

            // 校验排序字段（白名单防 SQL 注入）
            if (!request.validateSortField()) {
                return ApiResponse.error("400", "不支持的排序字段: " + request.getSortField(), "Unsupported sort field: " + request.getSortField());
            }
            if (!request.validateSortOrder()) {
                return ApiResponse.error("400", "不支持的排序方向", "Unsupported sort order");
            }

            // 2. 计算偏移量
            int offset = (curPage - 1) * pageSize;

            // 3. 查询总数
            long total = abilityMapper.countByKeyword(request.getKeyword());

            // 4. 分页查询
            List<AbilityEntity> entities = abilityMapper.selectPage(
                    request.getKeyword(),
                    request.getSortField(),
                    request.getSortOrder(),
                    offset,
                    pageSize
            );

            // 5. 组装 VO（含属性表查询图标/示意图 URL）
            List<AdminAbilityVO> voList = buildVOList(entities);

            // 6. 构造分页响应
            int totalPages = (int) ((total + pageSize - 1) / pageSize);
            ApiResponse.PageResponse page = ApiResponse.PageResponse.builder()
                    .curPage(curPage)
                    .pageSize(pageSize)
                    .total(total)
                    .totalPages(totalPages)
                    .build();

            return ApiResponse.success(voList, page);
        } catch (Exception e) {
            log.error("Failed to query ability list", e);
            return ApiResponse.error("500", "查询能力列表失败", "Failed to query ability list");
        }
    }

    @Override
    public ApiResponse<Void> create(AdminAbilityCreateRequest request) {
        try {
            // 1. 校验能力类型编码唯一性
            AbilityEntity existing = abilityMapper.selectByAbilityType(request.getAbilityType());
            if (existing != null) {
                return ApiResponse.error("409", "编码已被占用", "Ability type already exists");
            }

            // 2. entryUrl 格式校验
            if (StringUtils.hasText(request.getEntryUrl())) {
                if (!request.getEntryUrl().matches("^https?://.*$")) {
                    return ApiResponse.error("400", "访问地址格式不正确，需以http/https开头",
                            "Invalid entry URL format, must start with http:// or https://");
                }
                if (request.getEntryUrl().length() > 1000) {
                    return ApiResponse.error("400", "访问地址不能超过1000字符",
                            "Entry URL must not exceed 1000 characters");
                }
            }

            // 3. loadType=2 时三要素必填校验
            Integer loadType = request.getLoadType() != null ? request.getLoadType() : 1;
            if (loadType == 2) {
                if (!StringUtils.hasText(request.getEntryUrl())
                        || !StringUtils.hasText(request.getRoutePath())
                        || !StringUtils.hasText(request.getAliasName())) {
                    return ApiResponse.error("400", "微前端加载模式下 entryUrl/routePath/aliasName 三要素必填",
                            "entryUrl/routePath/aliasName are required when loadType=2");
                }
            }

            // 4. orderNum 默认值：若未传则查询当前最大值 + 1
            Integer orderNum = request.getOrderNum();
            if (orderNum == null) {
                Integer maxOrderNum = abilityMapper.selectMaxOrderNum();
                orderNum = maxOrderNum != null ? maxOrderNum + 1 : 1;
            }

            // 5. 构建主表实体
            AbilityEntity entity = new AbilityEntity();
            entity.setAbilityType(request.getAbilityType());
            entity.setAbilityNameCn(request.getNameCn());
            entity.setAbilityNameEn(request.getNameEn());
            entity.setAbilityDescCn(request.getDescCn());
            entity.setAbilityDescEn(request.getDescEn());
            entity.setOrderNum(orderNum);
            entity.setEntryUrl(request.getEntryUrl());
            entity.setHidden(request.getHidden() != null ? request.getHidden() : 0);
            entity.setRoutePath(request.getRoutePath());
            entity.setAliasName(request.getAliasName());
            entity.setRequireRelease(request.getRequireRelease() != null ? request.getRequireRelease() : 0);
            entity.setLoadType(loadType);
            entity.setStatus(1);
            Date now = new Date();
            entity.setCreateBy("admin");
            entity.setCreateTime(now);
            entity.setLastUpdateBy("admin");
            entity.setLastUpdateTime(now);

            // 6. 插入主表（useGeneratedKeys 回填 id）
            abilityMapper.insert(entity);
            Long abilityId = entity.getId();

            // 7. 写属性表 — 图标
            if (StringUtils.hasText(request.getIconBatchId())) {
                AbilityProperty iconProp = new AbilityProperty();
                iconProp.setParentId(abilityId);
                iconProp.setPropertyName(AbilityPropertyEnum.ICON.getPropertyName());
                iconProp.setPropertyValue(request.getIconBatchId());
                iconProp.setStatus(1);
                iconProp.setCreateBy("admin");
                iconProp.setCreateTime(now);
                iconProp.setLastUpdateBy("admin");
                iconProp.setLastUpdateTime(now);
                abilityPropertyMapper.insert(iconProp);
            }

            // 8. 写属性表 — 示意图（可选）
            if (StringUtils.hasText(request.getDiagramBatchId())) {
                AbilityProperty diagramProp = new AbilityProperty();
                diagramProp.setParentId(abilityId);
                diagramProp.setPropertyName(AbilityPropertyEnum.EXAMPLE_DIAGRAM.getPropertyName());
                diagramProp.setPropertyValue(request.getDiagramBatchId());
                diagramProp.setStatus(1);
                diagramProp.setCreateBy("admin");
                diagramProp.setCreateTime(now);
                diagramProp.setLastUpdateBy("admin");
                diagramProp.setLastUpdateTime(now);
                abilityPropertyMapper.insert(diagramProp);
            }

            log.info("Ability created successfully: type={}, nameCn={}, id={}",
                    request.getAbilityType(), request.getNameCn(), abilityId);

            return ApiResponse.success();
        } catch (Exception e) {
            log.error("Failed to create ability", e);
            return ApiResponse.error("500", "创建能力失败", "Failed to create ability");
        }
    }

    /**
     * 构建 VO 列表，从属性表查询图标/示意图 URL
     */
    private List<AdminAbilityVO> buildVOList(List<AbilityEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        // 收集所有能力 ID
        List<Long> abilityIds = entities.stream()
                .map(AbilityEntity::getId)
                .collect(Collectors.toList());

        // 批量查询属性表
        List<AbilityProperty> properties = abilityPropertyMapper.selectByParentIds(abilityIds);

        // 用枚举值查询属性名
        String iconPropName = AbilityPropertyEnum.ICON.getPropertyName();
        String diagramPropName = AbilityPropertyEnum.EXAMPLE_DIAGRAM.getPropertyName();

        // 分别存储原始值 + 拼接后的 URL
        Map<Long, String> iconRawValues = new HashMap<>();
        Map<Long, String> iconUrls = new HashMap<>();
        Map<Long, String> diagramRawValues = new HashMap<>();
        Map<Long, String> diagramUrls = new HashMap<>();

        for (AbilityProperty prop : properties) {
            String propName = prop.getPropertyName();
            String rawValue = prop.getPropertyValue();
            Long parentId = prop.getParentId();

            if (iconPropName.equals(propName)) {
                iconRawValues.put(parentId, rawValue);
                iconUrls.put(parentId, rawValue != null ? commonFileService.getShowUrl(rawValue) : null);
            } else if (diagramPropName.equals(propName)) {
                diagramRawValues.put(parentId, rawValue);
                diagramUrls.put(parentId, rawValue != null ? commonFileService.getShowUrl(rawValue) : null);
            }
        }

        // 组装 VO
        List<AdminAbilityVO> voList = new ArrayList<>(entities.size());
        for (AbilityEntity entity : entities) {
            Long abilityId = entity.getId();

            AdminAbilityVO vo = AdminAbilityVO.builder()
                    .abilityType(entity.getAbilityType())
                    .nameCn(entity.getAbilityNameCn())
                    .nameEn(entity.getAbilityNameEn())
                    .descCn(entity.getAbilityDescCn())
                    .descEn(entity.getAbilityDescEn())
                    .icon(iconRawValues.get(abilityId))
                    .iconUrl(iconUrls.get(abilityId))
                    .exampleDiagram(diagramRawValues.get(abilityId))
                    .exampleDiagramUrl(diagramUrls.get(abilityId))
                    .orderNum(entity.getOrderNum())
                    .entryUrl(entity.getEntryUrl())
                    .hidden(entity.getHidden())
                    .routePath(entity.getRoutePath())
                    .aliasName(entity.getAliasName())
                    .requireRelease(entity.getRequireRelease())
                    .loadType(entity.getLoadType())
                    .createTime(entity.getCreateTime())
                    .updateBy(entity.getLastUpdateBy())
                    .updateTime(entity.getLastUpdateTime())
                    .build();

            voList.add(vo);
        }

        return voList;
    }
}
