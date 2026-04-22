package com.xxx.open.modules.event.service;

import com.xxx.open.common.exception.BusinessException;
import com.xxx.open.common.model.ApiResponse;
import com.xxx.open.common.util.SnowflakeIdGenerator;
import com.xxx.open.modules.category.entity.Category;
import com.xxx.open.modules.category.mapper.CategoryMapper;
import com.xxx.open.modules.event.dto.*;
import com.xxx.open.modules.event.entity.Event;
import com.xxx.open.modules.event.entity.EventProperty;
import com.xxx.open.modules.event.entity.Permission;
import com.xxx.open.modules.event.entity.PermissionProperty;
import com.xxx.open.modules.event.mapper.EventMapper;
import com.xxx.open.modules.event.mapper.EventPropertyMapper;
import com.xxx.open.modules.event.mapper.PermissionMapper;
import com.xxx.open.modules.event.mapper.PermissionPropertyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 事件服务
 * 
 * <p>提供事件注册、编辑、删除、撤回等功能</p>
 * <p>接口编号：#15 ~ #20</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventMapper eventMapper;
    private final EventPropertyMapper eventPropertyMapper;
    private final PermissionMapper permissionMapper;
    private final PermissionPropertyMapper permissionPropertyMapper;
    private final CategoryMapper categoryMapper;
    private final SnowflakeIdGenerator idGenerator;

    // 事件状态常量
    private static final int STATUS_DRAFT = 0;       // 草稿
    private static final int STATUS_PENDING = 1;     // 待审
    private static final int STATUS_PUBLISHED = 2;   // 已发布
    private static final int STATUS_OFFLINE = 3;     // 已下线

    /**
     * #15 获取事件列表（分页）
     * 
     * @param categoryId 分类ID（可选）
     * @param status 状态（可选）
     * @param keyword 搜索关键词（可选）
     * @param curPage 当前页码
     * @param pageSize 每页大小
     * @return 事件列表响应
     */
    public ApiResponse<List<EventListResponse>> getEventList(
            String categoryId, Integer status, String keyword,
            Integer curPage, Integer pageSize) {
        
        // 默认分页参数
        if (curPage == null || curPage < 1) {
            curPage = 1;
        }
        if (pageSize == null || pageSize < 1 || pageSize > 100) {
            pageSize = 20;
        }
        
        // 解析分类ID
        Long categoryIdLong = null;
        if (categoryId != null && !categoryId.isEmpty()) {
            try {
                categoryIdLong = Long.parseLong(categoryId);
            } catch (NumberFormatException e) {
                throw BusinessException.badRequest("分类ID格式错误", "Invalid category ID format");
            }
        }
        
        // 计算偏移量
        int offset = (curPage - 1) * pageSize;
        
        // 查询列表
        List<Event> events = eventMapper.selectList(categoryIdLong, status, keyword, offset, pageSize);
        
        // 统计总数
        long total = eventMapper.countList(categoryIdLong, status, keyword);
        
        // 转换响应
        List<EventListResponse> responseList = events.stream()
                .map(this::convertToListResponse)
                .collect(Collectors.toList());
        
        // 构建分页信息
        ApiResponse.PageResponse page = ApiResponse.PageResponse.builder()
                .curPage(curPage)
                .pageSize(pageSize)
                .total(total)
                .totalPages((int) Math.ceil((double) total / pageSize))
                .build();
        
        return ApiResponse.success(responseList, page);
    }

    /**
     * #16 获取事件详情
     * 
     * @param id 事件ID
     * @return 事件详情响应
     */
    public EventResponse getEventById(Long id) {
        // 查询事件（包含分类名称）
        Event event = eventMapper.selectByIdWithCategoryName(id);
        if (event == null) {
            throw BusinessException.notFound("事件不存在", "Event not found");
        }
        
        // 查询权限
        Permission permission = permissionMapper.selectByResource("event", id);
        
        // 查询事件属性
        List<EventProperty> properties = eventPropertyMapper.selectByParentId(id);
        
        // 转换响应
        return convertToDetailResponse(event, permission, properties);
    }

    /**
     * #17 注册事件
     * 
     * @param request 创建请求
     * @return 事件响应
     */
    @Transactional(rollbackFor = Exception.class)
    public EventResponse createEvent(EventCreateRequest request) {
        // 解析分类ID
        Long categoryId = parseId(request.getCategoryId(), "分类ID");
        
        // 验证分类是否存在
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw BusinessException.notFound("分类不存在", "Category not found");
        }
        
        // 验证 Topic 唯一性
        Event existingEvent = eventMapper.selectByTopic(request.getTopic());
        if (existingEvent != null) {
            throw new BusinessException("409", 
                    "Topic 已存在: " + request.getTopic(), 
                    "Topic already exists: " + request.getTopic());
        }
        
        // 验证 Scope 唯一性
        Permission existingPermission = permissionMapper.selectByScope(request.getPermission().getScope());
        if (existingPermission != null) {
            throw new BusinessException("409", 
                    "Scope 已存在: " + request.getPermission().getScope(), 
                    "Scope already exists: " + request.getPermission().getScope());
        }
        
        // 生成事件ID
        Long eventId = idGenerator.nextId();
        
        // 创建事件实体
        Event event = new Event();
        event.setId(eventId);
        event.setNameCn(request.getNameCn());
        event.setNameEn(request.getNameEn());
        event.setTopic(request.getTopic());
        event.setCategoryId(categoryId);
        event.setStatus(STATUS_PENDING); // 默认待审
        event.setCreateTime(new Date());
        event.setLastUpdateTime(new Date());
        event.setCreateBy("system"); // TODO: 从上下文获取当前用户
        event.setLastUpdateBy("system");
        
        // 保存事件
        eventMapper.insert(event);
        
        // 创建权限
        createPermission(eventId, categoryId, request.getPermission());
        
        // 保存事件属性
        if (request.getProperties() != null && !request.getProperties().isEmpty()) {
            saveEventProperties(eventId, request.getProperties());
        }
        
        log.info("事件注册成功: id={}, topic={}, scope={}", 
                eventId, request.getTopic(), request.getPermission().getScope());
        
        // 返回详情
        return getEventById(eventId);
    }

    /**
     * #18 更新事件
     * 
     * @param id 事件ID
     * @param request 更新请求
     * @return 事件响应
     */
    @Transactional(rollbackFor = Exception.class)
    public EventResponse updateEvent(Long id, EventUpdateRequest request) {
        // 查询事件
        Event event = eventMapper.selectById(id);
        if (event == null) {
            throw BusinessException.notFound("事件不存在", "Event not found");
        }
        
        // 检查事件状态（已发布的事件不能更新核心属性）
        if (event.getStatus() == STATUS_PUBLISHED) {
            throw new BusinessException("403", 
                    "已发布的事件不能更新", 
                    "Published event cannot be updated");
        }
        
        // 解析分类ID
        Long categoryId = event.getCategoryId();
        if (request.getCategoryId() != null && !request.getCategoryId().isEmpty()) {
            categoryId = parseId(request.getCategoryId(), "分类ID");
            // 验证分类是否存在
            Category category = categoryMapper.selectById(categoryId);
            if (category == null) {
                throw BusinessException.notFound("分类不存在", "Category not found");
            }
        }
        
        // 更新事件基本信息（只更新非null字段）
        if (request.getNameCn() != null && !request.getNameCn().trim().isEmpty()) {
            event.setNameCn(request.getNameCn());
        }
        if (request.getNameEn() != null && !request.getNameEn().trim().isEmpty()) {
            event.setNameEn(request.getNameEn());
        }
        if (categoryId != null) {
            event.setCategoryId(categoryId);
        }
        event.setLastUpdateTime(new Date());
        event.setLastUpdateBy("system"); // TODO: 从上下文获取当前用户
        
        // 保存更新
        eventMapper.update(event);
        
        // 更新权限
        if (request.getPermission() != null) {
            updatePermission(id, categoryId, request.getPermission());
        }
        
        // 更新事件属性
        if (request.getProperties() != null) {
            // 删除旧属性
            eventPropertyMapper.deleteByParentId(id);
            // 保存新属性
            if (!request.getProperties().isEmpty()) {
                saveEventProperties(id, request.getProperties());
            }
        }
        
        log.info("事件更新成功: id={}, nameCn={}", id, request.getNameCn());
        
        return getEventById(id);
    }

    /**
     * #19 删除事件
     * 
     * @param id 事件ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteEvent(Long id) {
        // 查询事件
        Event event = eventMapper.selectById(id);
        if (event == null) {
            throw BusinessException.notFound("事件不存在", "Event not found");
        }
        
        // 检查订阅关系
        int subscriptionCount = eventMapper.countSubscriptionsByEventId(id);
        if (subscriptionCount > 0) {
            throw new BusinessException("409", 
                    "事件被 " + subscriptionCount + " 个应用订阅，无法删除", 
                    "Event is subscribed by " + subscriptionCount + " apps, cannot delete");
        }
        
        // 删除权限属性
        Permission permission = permissionMapper.selectByResource("event", id);
        if (permission != null) {
            permissionPropertyMapper.deleteByParentId(permission.getId());
            // 删除权限
            permissionMapper.deleteById(permission.getId());
        }
        
        // 删除事件属性
        eventPropertyMapper.deleteByParentId(id);
        
        // 删除事件
        eventMapper.deleteById(id);
        
        log.info("事件删除成功: id={}", id);
    }

    /**
     * #20 撤回事件
     * 
     * @param id 事件ID
     * @return 事件响应
     */
    @Transactional(rollbackFor = Exception.class)
    public EventResponse withdrawEvent(Long id) {
        // 查询事件
        Event event = eventMapper.selectById(id);
        if (event == null) {
            throw BusinessException.notFound("事件不存在", "Event not found");
        }
        
        // 检查事件状态（只有待审状态可以撤回）
        if (event.getStatus() != STATUS_PENDING) {
            throw new BusinessException("400", 
                    "只有待审状态的事件可以撤回", 
                    "Only pending event can be withdrawn");
        }
        
        // 更新状态为草稿
        event.setStatus(STATUS_DRAFT);
        event.setLastUpdateTime(new Date());
        event.setLastUpdateBy("system"); // TODO: 从上下文获取当前用户
        
        eventMapper.update(event);
        
        log.info("事件撤回成功: id={}, status=draft", id);
        
        return getEventById(id);
    }

    // ==================== 私有方法 ====================

    /**
     * 创建权限
     */
    private void createPermission(Long eventId, Long categoryId, PermissionDto permissionDto) {
        Long permissionId = idGenerator.nextId();
        
        Permission permission = new Permission();
        permission.setId(permissionId);
        permission.setNameCn(permissionDto.getNameCn());
        permission.setNameEn(permissionDto.getNameEn());
        permission.setScope(permissionDto.getScope());
        permission.setResourceType("event");
        permission.setResourceId(eventId);
        permission.setCategoryId(categoryId);
        permission.setStatus(1); // 默认启用
        permission.setCreateTime(new Date());
        permission.setLastUpdateTime(new Date());
        permission.setCreateBy("system");
        permission.setLastUpdateBy("system");
        
        permissionMapper.insert(permission);
        
        // 保存权限属性（审批流程ID等）
        if (permissionDto.getApprovalFlowId() != null && !permissionDto.getApprovalFlowId().isEmpty()) {
            PermissionProperty property = new PermissionProperty();
            property.setId(idGenerator.nextId());
            property.setParentId(permissionId);
            property.setPropertyName("approval_flow_id");
            property.setPropertyValue(permissionDto.getApprovalFlowId());
            property.setStatus(1);
            property.setCreateTime(new Date());
            property.setLastUpdateTime(new Date());
            property.setCreateBy("system");
            property.setLastUpdateBy("system");
            
            permissionPropertyMapper.insert(property);
        }
    }

    /**
     * 更新权限
     */
    private void updatePermission(Long eventId, Long categoryId, PermissionDto permissionDto) {
        Permission permission = permissionMapper.selectByResource("event", eventId);
        if (permission == null) {
            // 不存在则创建
            createPermission(eventId, categoryId, permissionDto);
            return;
        }
        
        // 更新权限基本信息
        permission.setNameCn(permissionDto.getNameCn());
        permission.setNameEn(permissionDto.getNameEn());
        permission.setCategoryId(categoryId);
        permission.setLastUpdateTime(new Date());
        permission.setLastUpdateBy("system");
        
        permissionMapper.update(permission);
        
        // 更新权限属性
        if (permissionDto.getApprovalFlowId() != null) {
            permissionPropertyMapper.deleteByParentId(permission.getId());
            
            if (!permissionDto.getApprovalFlowId().isEmpty()) {
                PermissionProperty property = new PermissionProperty();
                property.setId(idGenerator.nextId());
                property.setParentId(permission.getId());
                property.setPropertyName("approval_flow_id");
                property.setPropertyValue(permissionDto.getApprovalFlowId());
                property.setStatus(1);
                property.setCreateTime(new Date());
                property.setLastUpdateTime(new Date());
                property.setCreateBy("system");
                property.setLastUpdateBy("system");
                
                permissionPropertyMapper.insert(property);
            }
        }
    }

    /**
     * 保存事件属性
     */
    private void saveEventProperties(Long eventId, List<EventPropertyDto> propertyDtos) {
        List<EventProperty> properties = new ArrayList<>();
        Date now = new Date();
        
        for (EventPropertyDto dto : propertyDtos) {
            EventProperty property = new EventProperty();
            property.setId(idGenerator.nextId());
            property.setParentId(eventId);
            property.setPropertyName(dto.getPropertyName());
            property.setPropertyValue(dto.getPropertyValue());
            property.setStatus(1);
            property.setCreateTime(now);
            property.setLastUpdateTime(now);
            property.setCreateBy("system");
            property.setLastUpdateBy("system");
            
            properties.add(property);
        }
        
        eventPropertyMapper.batchInsert(properties);
    }

    /**
     * 转换为列表响应
     */
    private EventListResponse convertToListResponse(Event event) {
        EventListResponse response = new EventListResponse();
        response.setId(String.valueOf(event.getId()));
        response.setNameCn(event.getNameCn());
        response.setNameEn(event.getNameEn());
        response.setTopic(event.getTopic());
        response.setCategoryId(String.valueOf(event.getCategoryId()));
        response.setCategoryName(event.getCategoryName()); // 从 JOIN 查询获取
        response.setStatus(event.getStatus());
        response.setCreateTime(event.getCreateTime());
        
        // 查询权限（简化信息）
        Permission permission = permissionMapper.selectByResource("event", event.getId());
        if (permission != null) {
            EventListResponse.PermissionSimpleDto permDto = new EventListResponse.PermissionSimpleDto();
            permDto.setId(String.valueOf(permission.getId()));
            permDto.setScope(permission.getScope());
            permDto.setStatus(permission.getStatus());
            response.setPermission(permDto);
        }
        
        return response;
    }

    /**
     * 转换为详情响应
     */
    private EventResponse convertToDetailResponse(Event event, Permission permission, List<EventProperty> properties) {
        EventResponse response = new EventResponse();
        response.setId(String.valueOf(event.getId()));
        response.setNameCn(event.getNameCn());
        response.setNameEn(event.getNameEn());
        response.setTopic(event.getTopic());
        response.setCategoryId(String.valueOf(event.getCategoryId()));
        response.setCategoryName(event.getCategoryName()); // 从 JOIN 查询获取
        response.setStatus(event.getStatus());
        response.setCreateTime(event.getCreateTime());
        response.setCreateBy(event.getCreateBy());
        
        // 转换权限
        if (permission != null) {
            PermissionDto permDto = new PermissionDto();
            permDto.setId(String.valueOf(permission.getId()));
            permDto.setNameCn(permission.getNameCn());
            permDto.setNameEn(permission.getNameEn());
            permDto.setScope(permission.getScope());
            permDto.setStatus(permission.getStatus());
            
            // 查询权限属性
            List<PermissionProperty> permProperties = permissionPropertyMapper.selectByParentId(permission.getId());
            for (PermissionProperty prop : permProperties) {
                if ("approval_flow_id".equals(prop.getPropertyName())) {
                    permDto.setApprovalFlowId(prop.getPropertyValue());
                    break;
                }
            }
            
            response.setPermission(permDto);
        }
        
        // 转换属性
        if (properties != null && !properties.isEmpty()) {
            List<EventPropertyDto> propDtos = properties.stream()
                    .map(prop -> {
                        EventPropertyDto dto = new EventPropertyDto();
                        dto.setPropertyName(prop.getPropertyName());
                        dto.setPropertyValue(prop.getPropertyValue());
                        return dto;
                    })
                    .collect(Collectors.toList());
            response.setProperties(propDtos);
        }
        
        return response;
    }

    /**
     * 解析 ID
     */
    private Long parseId(String id, String fieldName) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw BusinessException.badRequest(fieldName + "格式错误", "Invalid " + fieldName + " format");
        }
    }
}
