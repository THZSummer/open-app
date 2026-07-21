package com.xxx.it.works.wecode.v2.modules.ability.service.impl;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.ability.dto.admin.AdminAbilityCreateRequest;
import com.xxx.it.works.wecode.v2.modules.ability.dto.admin.AdminAbilityListRequest;
import com.xxx.it.works.wecode.v2.modules.ability.dto.admin.AdminAbilityUpdateRequest;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
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
import org.springframework.transaction.annotation.Transactional;
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

    private final IdGeneratorStrategy idGenerator;

    public AdminAbilityServiceImpl(AbilityMapper abilityMapper,
                                    AbilityPropertyMapper abilityPropertyMapper,
                                    CommonFileService commonFileService,
                                    IdGeneratorStrategy idGenerator) {
        this.abilityMapper = abilityMapper;
        this.abilityPropertyMapper = abilityPropertyMapper;
        this.commonFileService = commonFileService;
        this.idGenerator = idGenerator;
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
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Map<String, Object>> create(AdminAbilityCreateRequest request) {
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
            entity.setHidden(request.getHidden() != null ? request.getHidden() : 1);
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

            // 6. 生成主键 ID 后插入主表（表无 AUTO_INCREMENT，由 IdGenerator 生成）
            entity.setId(idGenerator.nextId());
            abilityMapper.insert(entity);
            Long abilityId = entity.getId();

            // 7. 写属性表 — 图标
            if (StringUtils.hasText(request.getIconBatchId())) {
                AbilityProperty iconProp = new AbilityProperty();
                iconProp.setId(idGenerator.nextId());
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
                diagramProp.setId(idGenerator.nextId());
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

            // 9. 构造响应数据（含创建记录的关键信息）
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("abilityType", request.getAbilityType());
            resultData.put("nameCn", request.getNameCn());
            resultData.put("id", String.valueOf(abilityId));
            resultData.put("createTime", now);

            log.info("Ability created successfully: type={}, nameCn={}, id={}",
                    request.getAbilityType(), request.getNameCn(), abilityId);

            return ApiResponse.success(resultData);
        } catch (Exception e) {
            log.error("Failed to create ability", e);
            return ApiResponse.error("500", "创建能力失败", "Failed to create ability");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> update(Long id, AdminAbilityUpdateRequest request) {
        try {
            // 1. 查找已有记录
            AbilityEntity existing = abilityMapper.selectByPrimaryKey(id);
            if (existing == null) {
                return ApiResponse.error("404", "能力记录不存在", "Ability record not found");
            }

            // 2. abilityType 不可修改 — 忽略请求中的 abilityType
            // （AdminAbilityUpdateRequest 无 abilityType 字段，无需处理）

            // 3. 字段校验（仅校验传入的字段）
            // nameCn/nameEn/descCn/descEn 的 @Size 校验已在 Request DTO 上由 @Valid 处理

            // 4. entryUrl 格式校验（若传入新值）
            if (request.getEntryUrl() != null) {
                if (!request.getEntryUrl().matches("^https?://.*$")) {
                    return ApiResponse.error("400", "访问地址格式不正确，需以http/https开头",
                            "Invalid entry URL format, must start with http:// or https://");
                }
                if (request.getEntryUrl().length() > 1000) {
                    return ApiResponse.error("400", "访问地址不能超过1000字符",
                            "Entry URL must not exceed 1000 characters");
                }
            }

            // 5. loadType 联动校验
            // 如果请求中传了 loadType，用请求值；否则用数据库现有值
            Integer effectiveLoadType = request.getLoadType() != null ? request.getLoadType() : existing.getLoadType();
            String effectiveEntryUrl = request.getEntryUrl() != null ? request.getEntryUrl() : existing.getEntryUrl();
            String effectiveRoutePath = request.getRoutePath() != null ? request.getRoutePath() : existing.getRoutePath();
            String effectiveAliasName = request.getAliasName() != null ? request.getAliasName() : existing.getAliasName();

            if (effectiveLoadType == 2) {
                if (!StringUtils.hasText(effectiveEntryUrl)
                        || !StringUtils.hasText(effectiveRoutePath)
                        || !StringUtils.hasText(effectiveAliasName)) {
                    return ApiResponse.error("400", "微前端加载模式下 entryUrl/routePath/aliasName 三要素必填",
                            "entryUrl/routePath/aliasName are required when loadType=2");
                }
            }

            // 6. 构建主表更新实体（仅更新传入的字段）
            AbilityEntity entity = new AbilityEntity();
            entity.setId(id);
            entity.setAbilityNameCn(request.getNameCn());
            entity.setAbilityNameEn(request.getNameEn());
            entity.setAbilityDescCn(request.getDescCn());
            entity.setAbilityDescEn(request.getDescEn());
            entity.setOrderNum(request.getOrderNum());
            entity.setEntryUrl(request.getEntryUrl());
            entity.setHidden(request.getHidden());
            entity.setRoutePath(request.getRoutePath());
            entity.setAliasName(request.getAliasName());
            entity.setRequireRelease(request.getRequireRelease());
            entity.setLoadType(request.getLoadType());

            Date now = new Date();
            entity.setLastUpdateBy("admin");

            // 乐观锁：仅当客户端传入 lastUpdateTime 时设置，用于 Mapper WHERE 条件
            Date clientLastUpdateTime = request.getLastUpdateTime();
            if (clientLastUpdateTime != null) {
                entity.setLastUpdateTime(clientLastUpdateTime);
            }
            // 注：last_update_time 的 SET 值由 Mapper XML 统一使用 NOW()，
            // 实体中的 lastUpdateTime 仅用于 WHERE 乐观锁条件

            // 7. 执行主表更新
            int affected = abilityMapper.updateByPrimaryKeySelective(entity);

            // 乐观锁冲突检测
            if (clientLastUpdateTime != null && affected == 0) {
                // 二次确认：如果 affected == 0 是因为没有字段要更新（部分更新全不传），这不是冲突
                boolean hasUpdateFields = request.getNameCn() != null
                        || request.getNameEn() != null
                        || request.getDescCn() != null
                        || request.getDescEn() != null
                        || request.getOrderNum() != null
                        || request.getEntryUrl() != null
                        || request.getHidden() != null
                        || request.getRoutePath() != null
                        || request.getAliasName() != null
                        || request.getRequireRelease() != null
                        || request.getLoadType() != null;
                if (hasUpdateFields) {
                    return ApiResponse.error("409", "数据已被修改，请刷新后重试",
                            "Data has been modified, please refresh and try again");
                }
            }

            // 8. 更新属性表 — 图标（若传入新值）
            if (request.getIconBatchId() != null) {
                upsertProperty(id, AbilityPropertyEnum.ICON.getPropertyName(),
                        request.getIconBatchId(), now);
            }

            // 9. 更新属性表 — 示意图（若传入新值）
            if (request.getDiagramBatchId() != null) {
                upsertProperty(id, AbilityPropertyEnum.EXAMPLE_DIAGRAM.getPropertyName(),
                        request.getDiagramBatchId(), now);
            }

            log.info("Ability updated successfully: id={}", id);

            return ApiResponse.success();
        } catch (Exception e) {
            log.error("Failed to update ability, id={}", id, e);
            return ApiResponse.error("500", "更新能力失败", "Failed to update ability");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> delete(Long id) {
        try {
            // 1. 检查能力是否存在
            AbilityEntity existing = abilityMapper.selectByPrimaryKey(id);
            if (existing == null) {
                return ApiResponse.error("404", "能力记录不存在", "Ability record not found");
            }

            Integer abilityType = existing.getAbilityType();

            // 2. 删除属性表记录
            abilityPropertyMapper.deleteByParentId(id);

            // 3. 删除主表记录
            abilityMapper.deleteByAbilityType(abilityType);

            log.info("Ability deleted successfully: type={}, nameCn={}, id={}",
                    abilityType, existing.getAbilityNameCn(), id);

            return ApiResponse.success();
        } catch (Exception e) {
            log.error("Failed to delete ability, id={}", id, e);
            return ApiResponse.error("500", "删除能力失败", "Failed to delete ability");
        }
    }

    /**
     * 更新或插入属性记录
     *
     * <p>如果指定属性已存在则更新，不存在则插入。</p>
     *
     * @param parentId     能力ID
     * @param propertyName 属性名
     * @param propertyValue 属性值
     * @param now          当前时间
     */
    private void upsertProperty(Long parentId, String propertyName, String propertyValue, Date now) {
        AbilityProperty existingProp = abilityPropertyMapper.selectByParentIdAndPropertyName(parentId, propertyName);
        if (existingProp != null) {
            // 更新已有属性
            AbilityProperty updateProp = new AbilityProperty();
            updateProp.setId(existingProp.getId());
            updateProp.setPropertyValue(propertyValue);
            updateProp.setLastUpdateBy("admin");
            updateProp.setLastUpdateTime(now);
            abilityPropertyMapper.updateByPrimaryKeySelective(updateProp);
        } else {
            // 插入新属性
            AbilityProperty newProp = new AbilityProperty();
            newProp.setId(idGenerator.nextId());
            newProp.setParentId(parentId);
            newProp.setPropertyName(propertyName);
            newProp.setPropertyValue(propertyValue);
            newProp.setStatus(1);
            newProp.setCreateBy("admin");
            newProp.setCreateTime(now);
            newProp.setLastUpdateBy("admin");
            newProp.setLastUpdateTime(now);
            abilityPropertyMapper.insert(newProp);
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
                    .id(abilityId)
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
