package com.xxx.it.works.wecode.v2.modules.lookup.service;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.item.ItemCreateDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.item.ItemQueryDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.item.ItemUpdateDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.common.ImportResultVO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.common.PageVO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.item.ItemDetailVO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.item.ItemListVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * LookUp项服务接口
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
public interface LookUpItemService {

    /**
     * 查询LookUp项列表
     *
     * @param classifyId 分类ID
     * @param queryDTO 查询条件
     * @return 分页列表
     */
    ApiResponse<PageVO<ItemListVO>> getItemList(Long classifyId, ItemQueryDTO queryDTO);

    /**
     * 新增LookUp项
     *
     * @param classifyId 分类ID
     * @param createDTO 新增数据
     * @return 操作结果
     */
    ApiResponse<Void> createItem(Long classifyId, ItemCreateDTO createDTO);

    /**
     * 编辑LookUp项
     *
     * @param itemId 项ID
     * @param updateDTO 更新数据
     * @return 操作结果
     */
    ApiResponse<Void> updateItem(Long itemId, ItemUpdateDTO updateDTO);

    /**
     * 删除LookUp项
     *
     * @param itemId 项ID
     * @return 操作结果
     */
    ApiResponse<Void> deleteItem(Long itemId);

    /**
     * 获取LookUp项详情
     *
     * @param itemId 项ID
     * @return 项详情
     */
    ApiResponse<ItemDetailVO> getItemDetail(Long itemId);

    /**
     * 批量导入LookUp项
     *
     * @param classifyId 分类ID
     * @param file Excel文件
     * @return 导入结果
     */
    ApiResponse<ImportResultVO> importItems(Long classifyId, MultipartFile file);

    /**
     * 异步批量导入LookUp项
     *
     * @param classifyId 分类ID
     * @param file Excel文件
     * @return 任务ID，用于查询导入进度
     */
    ApiResponse<Long> importItemsAsync(Long classifyId, MultipartFile file);

    /**
     * 导出LookUp项
     *
     * @param classifyId 分类ID
     * @param status 状态
     * @param itemCode 项编码
     * @param itemName 项名称
     * @param response HTTP响应
     */
    void exportItems(Long classifyId, Integer status, String itemCode, String itemName, HttpServletResponse response);

    /**
     * 异步导出LookUp项
     *
     * @param classifyId 分类ID
     * @param status 状态
     * @param itemCode 项编码
     * @param itemName 项名称
     * @return 任务ID
     */
    ApiResponse<Long> exportItemsAsync(Long classifyId, Integer status, String itemCode, String itemName);

    /**
     * 下载导入模板
     *
     * @param response HTTP响应
     */
    void downloadTemplate(HttpServletResponse response);
}
