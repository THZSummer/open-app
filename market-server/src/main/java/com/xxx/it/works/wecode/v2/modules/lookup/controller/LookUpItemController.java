package com.xxx.it.works.wecode.v2.modules.lookup.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.common.security.PlatformAdminPermission;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.item.ItemCreateDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.item.ItemQueryDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.item.ItemUpdateDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.service.LookUpItemService;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.common.ImportResultVO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.common.PageVO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.item.ItemDetailVO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.item.ItemListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * LookUp项管理 Controller
 *
 * <p>提供LookUp项的CRUD接口和批量导入导出功能</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/lookup")
@RequiredArgsConstructor
@Tag(name = "LookUp项管理", description = "LookUp项管理接口")
public class LookUpItemController {

    private final LookUpItemService lookUpItemService;

    // ==================== LookUp项 CRUD ====================

    /**
     * 查询LookUp项列表
     *
     * @param classifyId 分类ID
     * @param itemCode 项编码，模糊匹配
     * @param itemName 项名称，模糊匹配
     * @param status 状态：0-失效，1-有效
     * @param pageNum 页码，默认1
     * @param pageSize 每页条数，默认10
     * @return 分页列表
     */
    @GetMapping("/classify/{classifyId}/items")
    @Operation(summary = "查询LookUp项列表",
               description = "根据分类ID查询LookUp项列表，支持分页和条件筛选")
    public ApiResponse<PageVO<ItemListVO>> getItemList(
            @Parameter(description = "分类ID", required = true)
            @PathVariable Long classifyId,
            @Parameter(description = "项编码，模糊匹配")
            @RequestParam(required = false) String itemCode,
            @Parameter(description = "项名称，模糊匹配")
            @RequestParam(required = false) String itemName,
            @Parameter(description = "状态：0-失效(INACTIVE)，1-有效(ACTIVE)，对应 StatusEnum")
            @RequestParam(required = false) Integer status,
            @Parameter(description = "页码，默认1")
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数，默认10")
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {

        log.info("Get item list, classifyId={}, itemCode={}, itemName={}, status={}, pageNum={}, pageSize={}",
                classifyId, itemCode, itemName, status, pageNum, pageSize);

        ItemQueryDTO queryDTO = new ItemQueryDTO();
        queryDTO.setClassifyId(classifyId);
        queryDTO.setItemCode(itemCode);
        queryDTO.setItemName(itemName);
        queryDTO.setStatus(status);
        queryDTO.setPageNum(pageNum);
        queryDTO.setPageSize(pageSize);

        return lookUpItemService.getItemList(classifyId, queryDTO);
    }

    /**
     * 新增LookUp项
     *
     * @param classifyId 分类ID
     * @param createDTO 新增数据
     * @return 操作结果
     */
    @PostMapping("/classify/{classifyId}/items")
    @PlatformAdminPermission
    @Operation(summary = "新增LookUp项",
               description = "在指定分类下新增LookUp项，项编码在同一分类下唯一")
    public ApiResponse<Void> createItem(
            @Parameter(description = "分类ID", required = true)
            @PathVariable Long classifyId,
            @Valid @RequestBody ItemCreateDTO createDTO) {

        log.info("Create item, classifyId={}, itemCode={}", classifyId, createDTO.getItemCode());

        return lookUpItemService.createItem(classifyId, createDTO);
    }

    /**
     * 编辑LookUp项
     *
     * @param itemId 项ID
     * @param updateDTO 更新数据
     * @return 操作结果
     */
    @PutMapping("/items/{itemId}")
    @PlatformAdminPermission
    @Operation(summary = "编辑LookUp项",
               description = "编辑LookUp项信息，项编码不可修改")
    public ApiResponse<Void> updateItem(
            @Parameter(description = "项ID", required = true)
            @PathVariable Long itemId,
            @Valid @RequestBody ItemUpdateDTO updateDTO) {

        log.info("Update item, itemId={}", itemId);

        return lookUpItemService.updateItem(itemId, updateDTO);
    }

    /**
     * 删除LookUp项
     *
     * @param itemId 项ID
     * @return 操作结果
     */
    @DeleteMapping("/items/{itemId}")
    @PlatformAdminPermission
    @Operation(summary = "删除LookUp项",
               description = "删除指定的LookUp项")
    public ApiResponse<Void> deleteItem(
            @Parameter(description = "项ID", required = true)
            @PathVariable Long itemId) {

        log.info("Delete item, itemId={}", itemId);

        return lookUpItemService.deleteItem(itemId);
    }

    /**
     * 获取LookUp项详情
     *
     * @param itemId 项ID
     * @return 项详情
     */
    @GetMapping("/items/{itemId}")
    @Operation(summary = "获取LookUp项详情",
               description = "获取LookUp项完整详情，包含分类名称和扩展属性")
    public ApiResponse<ItemDetailVO> getItemDetail(
            @Parameter(description = "项ID", required = true)
            @PathVariable Long itemId) {

        log.info("Get item detail, itemId={}", itemId);

        return lookUpItemService.getItemDetail(itemId);
    }

    // ==================== 批量操作 ====================

    /**
     * 批量导入LookUp项
     *
     * @param classifyId 分类ID
     * @param file Excel文件
     * @return 导入结果
     */
    @PostMapping("/import")
    @PlatformAdminPermission
    @Operation(summary = "批量导入LookUp项",
               description = "从Excel文件批量导入LookUp项，支持.xlsx和.xls格式，单次最多5000条")
    public ApiResponse<ImportResultVO> importItems(
            @Parameter(description = "分类ID", required = true)
            @RequestParam Long classifyId,
            @Parameter(description = "Excel文件", required = true)
            @RequestParam MultipartFile file) {

        log.info("Import items, classifyId={}, fileName={}", classifyId, file.getOriginalFilename());

        return lookUpItemService.importItems(classifyId, file);
    }

    /**
     * 异步批量导入LookUp项
     *
     * @param classifyId 分类ID
     * @param file Excel文件
     * @return 任务ID，用于查询导入进度
     */
    @PostMapping("/import/async")
    @PlatformAdminPermission
    @Operation(summary = "异步批量导入LookUp项",
               description = "从Excel文件异步批量导入LookUp项，立即返回任务ID，支持.xlsx和.xls格式，单次最多5000条")
    public ApiResponse<Long> importItemsAsync(
            @Parameter(description = "分类ID", required = true)
            @RequestParam Long classifyId,
            @Parameter(description = "Excel文件", required = true)
            @RequestParam MultipartFile file) {

        log.info("Import items async, classifyId={}, fileName={}", classifyId, file.getOriginalFilename());

        return lookUpItemService.importItemsAsync(classifyId, file);
    }

    /**
     * 导出LookUp项
     *
     * @param classifyId 分类ID（可选）
     * @param status 状态筛选（可选）
     * @param itemCode 项编码模糊匹配（可选）
     * @param itemName 项名称模糊匹配（可选）
     * @param response HTTP响应
     */
    @GetMapping("/export")
    @PlatformAdminPermission
    @Operation(summary = "导出LookUp项",
               description = "导出LookUp项到Excel文件，支持条件筛选，单次最多10000条")
    public void exportItems(
            @Parameter(description = "分类ID")
            @RequestParam(required = false) Long classifyId,
            @Parameter(description = "状态：0-失效(INACTIVE)，1-有效(ACTIVE)，对应 StatusEnum")
            @RequestParam(required = false) Integer status,
            @Parameter(description = "项编码，模糊匹配")
            @RequestParam(required = false) String itemCode,
            @Parameter(description = "项名称，模糊匹配")
            @RequestParam(required = false) String itemName,
            HttpServletResponse response) {

        log.info("Export items, classifyId={}, status={}, itemCode={}, itemName={}",
                classifyId, status, itemCode, itemName);

        lookUpItemService.exportItems(classifyId, status, itemCode, itemName, response);
    }

    /**
     * 异步导出LookUp项
     *
     * @param classifyId 分类ID（可选）
     * @param status 状态筛选（可选）
     * @param itemCode 项编码模糊匹配（可选）
     * @param itemName 项名称模糊匹配（可选）
     * @return 任务ID
     */
    @PostMapping("/export/async")
    @PlatformAdminPermission
    @Operation(summary = "异步导出LookUp项",
               description = "异步导出LookUp项到Excel文件，立即返回任务ID，支持条件筛选，单次最多10000条")
    public ApiResponse<Long> exportItemsAsync(
            @Parameter(description = "分类ID")
            @RequestParam(required = false) Long classifyId,
            @Parameter(description = "状态：0-失效(INACTIVE)，1-有效(ACTIVE)，对应 StatusEnum")
            @RequestParam(required = false) Integer status,
            @Parameter(description = "项编码，模糊匹配")
            @RequestParam(required = false) String itemCode,
            @Parameter(description = "项名称，模糊匹配")
            @RequestParam(required = false) String itemName) {

        log.info("Export items async, classifyId={}, status={}, itemCode={}, itemName={}",
                classifyId, status, itemCode, itemName);

        return lookUpItemService.exportItemsAsync(classifyId, status, itemCode, itemName);
    }

    /**
     * 下载导入模板
     *
     * @param response HTTP响应
     */
    @GetMapping("/import/template")
    @PlatformAdminPermission
    @Operation(summary = "下载导入模板",
               description = "下载LookUp项批量导入的Excel模板文件")
    public void downloadTemplate(HttpServletResponse response) {
        log.info("Download import template");

        lookUpItemService.downloadTemplate(response);
    }
}
