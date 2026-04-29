package com.xxx.it.works.wecode.v2.modules.event.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.event.dto.*;
import com.xxx.it.works.wecode.v2.modules.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 事件管理 Controller
 *
 * <p>提供事件注册、编辑、删除、撤回等接口</p>
 * <p>接口编号：#15 ~ #20</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/service/open/v2/events")
@RequiredArgsConstructor
@Tag(name = "事件管理", description = "事件资源管理接口")
public class EventController {

    private final EventService eventService;

    /**
     * #15 获取事件列表（分页）
     *
     * <p>返回事件列表，支持按分类过滤和分页</p>
     *
     * @param categoryId 分类ID过滤（可选）
     * @param status 状态过滤（可选）
     * @param keyword 搜索关键词（可选）
     * @param curPage 当前页码
     * @param pageSize 每页大小
     * @return 事件列表
     */
    @GetMapping
    @Operation(summary = "#15 获取事件列表",
               description = "返回事件列表，支持按分类过滤，支持分页参数")
    public ApiResponse<List<EventListResponse>> getEventList(
            @Parameter(description = "分类ID过滤")
            @RequestParam(required = false) String categoryId,
            @Parameter(description = "状态过滤（0=草稿, 1=待审, 2=已发布, 3=已下线）")
            @RequestParam(required = false) Integer status,
            @Parameter(description = "搜索关键词（名称、Scope、Topic）")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "当前页码，默认 1")
            @RequestParam(required = false) Integer curPage,
            @Parameter(description = "每页大小，默认 20")
            @RequestParam(required = false) Integer pageSize) {

        log.info("Get event list: categoryId={}, status={}, keyword={}, curPage={}, pageSize={}",
                categoryId, status, keyword, curPage, pageSize);

        return eventService.getEventList(categoryId, status, keyword, curPage, pageSize);
    }

    /**
     * #16 获取事件详情
     *
     * <p>返回事件详情及权限信息、属性</p>
     *
     * @param id 事件ID
     * @return 事件详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "#16 获取事件详情",
               description = "返回事件详情及权限信息、属性")
    public ApiResponse<EventResponse> getEventById(
            @Parameter(description = "事件ID")
            @PathVariable String id) {

        log.info("Get event detail: id={}", id);

        EventResponse response = eventService.getEventById(parseId(id));
        return ApiResponse.success(response);
    }

    /**
     * #17 注册事件
     *
     * <p>注册事件成功，同时创建权限资源，Topic 唯一性校验</p>
     *
     * @param request 创建请求
     * @return 事件响应
     */
    @PostMapping
    @Operation(summary = "#17 注册事件",
               description = "注册事件成功，同时创建权限资源，Topic 唯一性校验")
    public ApiResponse<EventResponse> createEvent(
            @Valid @RequestBody EventCreateRequest request) {

        log.info("Register event: nameCn={}, topic={}, scope={}",
                request.getNameCn(), request.getTopic(), request.getPermission().getScope());

        EventResponse response = eventService.createEvent(request);
        return ApiResponse.success(response);
    }

    /**
     * #18 更新事件
     *
     * <p>更新事件成功</p>
     *
     * @param id 事件ID
     * @param request 更新请求
     * @return 事件响应
     */
    @PutMapping("/{id}")
    @Operation(summary = "#18 更新事件",
               description = "更新事件成功")
    public ApiResponse<EventResponse> updateEvent(
            @Parameter(description = "事件ID")
            @PathVariable String id,
            @Valid @RequestBody EventUpdateRequest request) {

        log.info("Update event: id={}, nameCn={}", id, request.getNameCn());

        EventResponse response = eventService.updateEvent(parseId(id), request);
        return ApiResponse.success(response);
    }

    /**
     * #19 删除事件
     *
     * <p>删除事件，检查订阅关系</p>
     *
     * @param id 事件ID
     * @return 成功响应
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "#19 删除事件",
               description = "删除事件，检查订阅关系")
    public ApiResponse<Void> deleteEvent(
            @Parameter(description = "事件ID")
            @PathVariable String id) {

        log.info("Delete event: id={}", id);

        eventService.deleteEvent(parseId(id));
        return ApiResponse.success();
    }

    /**
     * #20 撤回事件
     *
     * <p>撤回审核中的事件</p>
     *
     * @param id 事件ID
     * @return 事件响应
     */
    @PostMapping("/{id}/withdraw")
    @Operation(summary = "#20 撤回事件",
               description = "撤回审核中的事件")
    public ApiResponse<EventResponse> withdrawEvent(
            @Parameter(description = "事件ID")
            @PathVariable String id) {

        log.info("Withdraw event: id={}", id);

        EventResponse response = eventService.withdrawEvent(parseId(id));
        return ApiResponse.success(response);
    }

    // ==================== Private methods ====================

    /**
     * 解析 ID
     */
    private Long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid ID format: " + id);
        }
    }
}
