package com.xxx.it.works.wecode.v2.modules.ability.service.impl;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.ability.dto.admin.AdminAbilityListRequest;
import com.xxx.it.works.wecode.v2.modules.ability.entity.AbilityEntity;
import com.xxx.it.works.wecode.v2.modules.ability.entity.AbilityProperty;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AbilityMapper;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AbilityPropertyMapper;
import com.xxx.it.works.wecode.v2.modules.ability.service.AdminAbilityService;
import com.xxx.it.works.wecode.v2.modules.ability.vo.admin.AdminAbilityVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    /**
     * 图标 URL 前缀
     */
    private static final String FILE_URL_PREFIX = "/ability-files/";

    /**
     * 属性表 key：图标
     */
    private static final String PROP_ICON = "icon";

    /**
     * 属性表 key：示意图
     */
    private static final String PROP_DIAGRAM = "diagram";

    private final AbilityMapper abilityMapper;

    private final AbilityPropertyMapper abilityPropertyMapper;

    public AdminAbilityServiceImpl(AbilityMapper abilityMapper, AbilityPropertyMapper abilityPropertyMapper) {
        this.abilityMapper = abilityMapper;
        this.abilityPropertyMapper = abilityPropertyMapper;
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

        // 按 parentId + propertyName 建立映射
        Map<Long, Map<String, String>> propertyMap = new HashMap<>();
        for (AbilityProperty prop : properties) {
            propertyMap.computeIfAbsent(prop.getParentId(), k -> new HashMap<>())
                    .put(prop.getPropertyName(), prop.getPropertyValue());
        }

        // 组装 VO
        List<AdminAbilityVO> voList = new ArrayList<>(entities.size());
        for (AbilityEntity entity : entities) {
            Map<String, String> props = propertyMap.getOrDefault(entity.getId(), Collections.emptyMap());

            String iconBatchId = props.get(PROP_ICON);
            String diagramBatchId = props.get(PROP_DIAGRAM);

            AdminAbilityVO vo = AdminAbilityVO.builder()
                    .abilityType(entity.getAbilityType())
                    .nameCn(entity.getAbilityNameCn())
                    .nameEn(entity.getAbilityNameEn())
                    .descCn(entity.getAbilityDescCn())
                    .descEn(entity.getAbilityDescEn())
                    .iconUrl(iconBatchId != null ? FILE_URL_PREFIX + iconBatchId : null)
                    .diagramUrl(diagramBatchId != null ? FILE_URL_PREFIX + diagramBatchId : null)
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
