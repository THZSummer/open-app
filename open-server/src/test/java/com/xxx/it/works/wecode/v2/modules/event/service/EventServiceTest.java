package com.xxx.it.works.wecode.v2.modules.event.service;

import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.modules.category.entity.Category;
import com.xxx.it.works.wecode.v2.modules.category.mapper.CategoryMapper;
import com.xxx.it.works.wecode.v2.modules.event.dto.EventCreateRequest;
import com.xxx.it.works.wecode.v2.modules.event.dto.EventResponse;
import com.xxx.it.works.wecode.v2.modules.event.dto.EventUpdateRequest;
import com.xxx.it.works.wecode.v2.modules.event.dto.PermissionDto;
import com.xxx.it.works.wecode.v2.modules.event.entity.Event;
import com.xxx.it.works.wecode.v2.modules.event.entity.Permission;
import com.xxx.it.works.wecode.v2.modules.event.mapper.EventMapper;
import com.xxx.it.works.wecode.v2.modules.event.mapper.EventPropertyMapper;
import com.xxx.it.works.wecode.v2.modules.event.mapper.PermissionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * EventService 测试类
 *
 * 测试部分字段更新功能
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventMapper eventMapper;

    @Mock
    private EventPropertyMapper eventPropertyMapper;

    @Mock
    private PermissionMapper permissionMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private EventService eventService;

    private Event existingEvent;
    private Category category;

    @BeforeEach
    void setUp() {

        // 准备测试数据
        existingEvent = new Event();
        existingEvent.setId(100L);
        existingEvent.setNameCn("原始中文名称");
        existingEvent.setNameEn("Original English Name");
        existingEvent.setTopic("test.event.update");
        existingEvent.setCategoryId(2L);
        existingEvent.setStatus(1); // 待审状态
        existingEvent.setCreateTime(new Date());
        existingEvent.setLastUpdateTime(new Date());

        category = new Category();
        category.setId(2L);
        category.setNameCn("测试分类");
        category.setNameEn("Test Category");
    }

    @Test
    @DisplayName("部分字段更新 - 只更新中文名称")
    void testPartialUpdate_OnlyNameCn() {

        // Given: 只提供中文名称
        EventUpdateRequest request = new EventUpdateRequest();
        request.setNameCn("新的中文名称");

        // nameEn 为 null

        when(eventMapper.selectById(100L)).thenReturn(existingEvent);
        when(eventMapper.update(any(Event.class))).thenReturn(1);
        when(eventMapper.selectByIdWithCategoryName(100L)).thenReturn(existingEvent);
        when(permissionMapper.selectByResource("event", 100L)).thenReturn(null);
        when(eventPropertyMapper.selectByParentId(100L)).thenReturn(List.of());

        // When
        EventResponse response = eventService.updateEvent(100L, request);

        // Then: 验证只更新了 nameCn，nameEn 保持不变
        verify(eventMapper).update(argThat(event ->
            "新的中文名称".equals(event.getNameCn()) &&
            "Original English Name".equals(event.getNameEn())
        ));
        assertNotNull(response);
    }

    @Test
    @DisplayName("部分字段更新 - 只更新英文名称")
    void testPartialUpdate_OnlyNameEn() {

        // Given: 只提供英文名称
        EventUpdateRequest request = new EventUpdateRequest();
        request.setNameEn("New English Name");

        // nameCn 为 null

        when(eventMapper.selectById(100L)).thenReturn(existingEvent);
        when(eventMapper.update(any(Event.class))).thenReturn(1);
        when(eventMapper.selectByIdWithCategoryName(100L)).thenReturn(existingEvent);
        when(permissionMapper.selectByResource("event", 100L)).thenReturn(null);
        when(eventPropertyMapper.selectByParentId(100L)).thenReturn(List.of());

        // When
        EventResponse response = eventService.updateEvent(100L, request);

        // Then: 验证只更新了 nameEn，nameCn 保持不变
        verify(eventMapper).update(argThat(event ->
            "原始中文名称".equals(event.getNameCn()) &&
            "New English Name".equals(event.getNameEn())
        ));
        assertNotNull(response);
    }

    @Test
    @DisplayName("部分字段更新 - 同时更新多个字段")
    void testPartialUpdate_MultipleFields() {

        // Given: 提供中文名称和英文名称
        EventUpdateRequest request = new EventUpdateRequest();
        request.setNameCn("新中文名称");
        request.setNameEn("New Name");
        request.setCategoryId("3");

        Category newCategory = new Category();
        newCategory.setId(3L);
        newCategory.setNameCn("新分类");

        when(eventMapper.selectById(100L)).thenReturn(existingEvent);
        when(categoryMapper.selectById(3L)).thenReturn(newCategory);
        when(eventMapper.update(any(Event.class))).thenReturn(1);
        when(eventMapper.selectByIdWithCategoryName(100L)).thenReturn(existingEvent);
        when(permissionMapper.selectByResource("event", 100L)).thenReturn(null);
        when(eventPropertyMapper.selectByParentId(100L)).thenReturn(List.of());

        // When
        EventResponse response = eventService.updateEvent(100L, request);

        // Then: 验证所有提供的字段都被更新
        verify(eventMapper).update(argThat(event ->
            "新中文名称".equals(event.getNameCn()) &&
            "New Name".equals(event.getNameEn()) &&
            event.getCategoryId().equals(3L)
        ));
        assertNotNull(response);
    }

    @Test
    @DisplayName("部分字段更新 - 空字符串应被忽略")
    void testPartialUpdate_EmptyStringShouldBeIgnored() {

        // Given: 提供空字符串
        EventUpdateRequest request = new EventUpdateRequest();
        request.setNameCn("  "); // 空格字符串
        request.setNameEn("Valid Name");

        when(eventMapper.selectById(100L)).thenReturn(existingEvent);
        when(eventMapper.update(any(Event.class))).thenReturn(1);
        when(eventMapper.selectByIdWithCategoryName(100L)).thenReturn(existingEvent);
        when(permissionMapper.selectByResource("event", 100L)).thenReturn(null);
        when(eventPropertyMapper.selectByParentId(100L)).thenReturn(List.of());

        // When
        EventResponse response = eventService.updateEvent(100L, request);

        // Then: nameCn 应保持不变，只有 nameEn 被更新
        verify(eventMapper).update(argThat(event ->
            "原始中文名称".equals(event.getNameCn()) &&
            "Valid Name".equals(event.getNameEn())
        ));
        assertNotNull(response);
    }

    @Test
    @DisplayName("部分字段更新 - 更新分类ID")
    void testPartialUpdate_CategoryId() {

        // Given: 只更新分类ID
        EventUpdateRequest request = new EventUpdateRequest();
        request.setCategoryId("3");

        Category newCategory = new Category();
        newCategory.setId(3L);

        when(eventMapper.selectById(100L)).thenReturn(existingEvent);
        when(categoryMapper.selectById(3L)).thenReturn(newCategory);
        when(eventMapper.update(any(Event.class))).thenReturn(1);
        when(eventMapper.selectByIdWithCategoryName(100L)).thenReturn(existingEvent);
        when(permissionMapper.selectByResource("event", 100L)).thenReturn(null);
        when(eventPropertyMapper.selectByParentId(100L)).thenReturn(List.of());

        // When
        EventResponse response = eventService.updateEvent(100L, request);

        // Then: 只有 categoryId 被更新，名称保持不变
        verify(eventMapper).update(argThat(event ->
            "原始中文名称".equals(event.getNameCn()) &&
            "Original English Name".equals(event.getNameEn()) &&
            event.getCategoryId().equals(3L)
        ));
        assertNotNull(response);
    }

    @Test
    @DisplayName("部分字段更新 - 更新 Topic 和 Scope")
    void testCreatePermissionScope_InvalidFormat() {
        EventCreateRequest request = new EventCreateRequest();
        request.setNameCn("Test Event");
        request.setNameEn("Test Event");
        request.setTopic("test.event.invalid");
        request.setCategoryId("2");

        PermissionDto permissionDto = new PermissionDto();
        permissionDto.setNameCn("Test Permission");
        permissionDto.setNameEn("Test Permission");
        permissionDto.setScope("event:1test:demo");
        request.setPermission(permissionDto);

        when(categoryMapper.selectById(2L)).thenReturn(category);
        when(eventMapper.selectByTopic("test.event.invalid")).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                eventService.createEvent(request));

        assertEquals("400", exception.getCode());
        verify(permissionMapper, never()).selectByScope(anyString());
        verify(eventMapper, never()).insert(any(Event.class));
    }

    @Test
    @DisplayName("Partial update - update Topic and Scope")
    void testPartialUpdate_TopicAndScope() {
        EventUpdateRequest request = new EventUpdateRequest();
        request.setTopic("test.event.changed");

        PermissionDto permissionDto = new PermissionDto();
        permissionDto.setScope("event:test:changed");
        request.setPermission(permissionDto);

        Permission permission = new Permission();
        permission.setId(200L);
        permission.setNameCn("原始权限中文名称");
        permission.setNameEn("Original Permission Name");
        permission.setScope("event:test:update");
        permission.setResourceType("event");
        permission.setResourceId(100L);
        permission.setCategoryId(2L);
        permission.setStatus(1);

        when(eventMapper.selectById(100L)).thenReturn(existingEvent);
        when(eventMapper.selectByTopic("test.event.changed")).thenReturn(null);
        when(eventMapper.update(any(Event.class))).thenReturn(1);
        when(permissionMapper.selectByResource("event", 100L)).thenReturn(permission);
        when(permissionMapper.selectByScope("event:test:changed")).thenReturn(null);
        when(permissionMapper.update(any(Permission.class))).thenReturn(1);
        when(eventMapper.selectByIdWithCategoryName(100L)).thenReturn(existingEvent);
        when(eventPropertyMapper.selectByParentId(100L)).thenReturn(List.of());

        EventResponse response = eventService.updateEvent(100L, request);

        verify(eventMapper).update(argThat(event ->
                "test.event.changed".equals(event.getTopic())));
        verify(permissionMapper).update(argThat(updatedPermission ->
                "event:test:changed".equals(updatedPermission.getScope())));
        assertNotNull(response);
    }

    @Test
    @DisplayName("Update event scope should reject invalid format")
    void testUpdatePermissionScope_InvalidFormat() {
        EventUpdateRequest request = new EventUpdateRequest();
        PermissionDto permissionDto = new PermissionDto();
        permissionDto.setScope("api:test:changed");
        request.setPermission(permissionDto);

        Permission permission = new Permission();
        permission.setId(200L);
        permission.setScope("event:test:old");

        when(eventMapper.selectById(100L)).thenReturn(existingEvent);
        when(eventMapper.update(any(Event.class))).thenReturn(1);
        when(permissionMapper.selectByResource("event", 100L)).thenReturn(permission);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                eventService.updateEvent(100L, request));

        assertEquals("400", exception.getCode());
        verify(permissionMapper, never()).selectByScope(anyString());
        verify(permissionMapper, never()).update(any(Permission.class));
    }

    @Test
    @DisplayName("Update non-existent event should throw")
    void testUpdateNonExistentEvent() {

        // Given
        EventUpdateRequest request = new EventUpdateRequest();
        request.setNameCn("New Name");

        when(eventMapper.selectById(999L)).thenReturn(null);

        // When & Then
        assertThrows(BusinessException.class, () -> {
            eventService.updateEvent(999L, request);
        });
    }

    @Test
    @DisplayName("Update published event should throw")
    void testUpdatePublishedEvent() {

        // Given
        Event publishedEvent = new Event();
        publishedEvent.setId(100L);
        publishedEvent.setStatus(2); // 已发布状态

        EventUpdateRequest request = new EventUpdateRequest();
        request.setNameCn("New Name");

        when(eventMapper.selectById(100L)).thenReturn(publishedEvent);

        // When & Then
        assertThrows(BusinessException.class, () -> {
            eventService.updateEvent(100L, request);
        });
    }
}
