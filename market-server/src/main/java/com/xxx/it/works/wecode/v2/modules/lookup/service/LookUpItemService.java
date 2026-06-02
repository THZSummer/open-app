package com.xxx.it.works.wecode.v2.modules.lookup.service;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.item.ItemCreateDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.item.ItemQueryDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.item.ItemUpdateDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.common.PageVO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.item.ItemDetailVO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.item.ItemListVO;

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
     * @param queryDTO 查询条件
     * @return 分页列表
     */
    ApiResponse<PageVO<ItemListVO>> getItemList(ItemQueryDTO queryDTO);

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
}