package com.xxx.it.works.wecode.v2.modules.lookup.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.common.security.AuthRole;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.item.ItemCreateDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.item.ItemQueryDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.item.ItemUpdateDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.service.LookUpItemService;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.common.PageVO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.item.ItemDetailVO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.item.ItemListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * LookUp项管理 Controller
 *
 * <p>提供LookUp项的CRUD接口</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/service/open/v2/lookup")
@Tag(name = "LookUp项管理", description = "LookUp项管理接口")
public class LookUpItemController {

    @Autowired
    private LookUpItemService lookUpItemService;

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
    @AuthRole
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

        return lookUpItemService.getItemList(queryDTO);
    }

    /**
     * 新增LookUp项
     *
     * @param classifyId 分类ID
     * @param createDTO 新增数据
     * @return 操作结果
     */
    @PostMapping("/classify/{classifyId}/items")
    @AuthRole
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
    @AuthRole
    @Operation(summary = "编辑LookUp项",
               description = "编辑LookUp项信息，项编码不可修改")
    public ApiResponse<Void> updateItem(
            @Parameter(description = "项ID", required = true)
            @PathVariable Long itemId,
            @Valid @RequestBody ItemUpdateDTO updateDTO) {

        log.info("Update item, itemId={}", itemId);

        if (itemId == null || itemId <= 0) {
            throw new IllegalArgumentException("项ID必须是正整数");
        }

        return lookUpItemService.updateItem(itemId, updateDTO);
    }

    /**
     * 删除LookUp项
     *
     * @param itemId 项ID
     * @return 操作结果
     */
    @DeleteMapping("/items/{itemId}")
    @AuthRole
    @Operation(summary = "删除LookUp项",
               description = "删除指定的LookUp项")
    public ApiResponse<Void> deleteItem(
            @Parameter(description = "项ID", required = true)
            @PathVariable Long itemId) {

        log.info("Delete item, itemId={}", itemId);

        if (itemId == null || itemId <= 0) {
            throw new IllegalArgumentException("项ID必须是正整数");
        }

        return lookUpItemService.deleteItem(itemId);
    }

    /**
     * 获取LookUp项详情
     *
     * @param itemId 项ID
     * @return 项详情
     */
    @GetMapping("/items/{itemId}")
    @AuthRole
    @Operation(summary = "获取LookUp项详情",
               description = "获取LookUp项完整详情，包含分类名称和扩展属性")
    public ApiResponse<ItemDetailVO> getItemDetail(
            @Parameter(description = "项ID", required = true)
            @PathVariable Long itemId) {

        log.info("Get item detail, itemId={}", itemId);

        if (itemId == null || itemId <= 0) {
            throw new IllegalArgumentException("项ID必须是正整数");
        }

        return lookUpItemService.getItemDetail(itemId);
    }
}