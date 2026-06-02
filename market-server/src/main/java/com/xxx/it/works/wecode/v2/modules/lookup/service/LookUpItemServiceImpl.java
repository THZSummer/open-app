package com.xxx.it.works.wecode.v2.modules.lookup.service;

import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.service.CacheServiceV2;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.common.enums.ResponseCodeEnum;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.item.ItemCreateDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.item.ItemQueryDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.item.ItemUpdateDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.entity.ClassifyEntity;
import com.xxx.it.works.wecode.v2.modules.lookup.entity.LookUpItemEntity;
import com.xxx.it.works.wecode.v2.modules.lookup.enums.StatusEnum;
import com.xxx.it.works.wecode.v2.modules.lookup.mapper.ClassifyMapper;
import com.xxx.it.works.wecode.v2.modules.lookup.mapper.LookUpItemMapper;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.common.PageVO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.item.ItemDetailVO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.item.ItemListVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * LookUp项服务实现
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Service
public class LookUpItemServiceImpl implements LookUpItemService {

    @Autowired
    private ClassifyMapper classifyMapper;
    @Autowired
    private LookUpItemMapper lookUpItemMapper;
    @Autowired
    private IdGeneratorStrategy idGenerator;
    @Autowired
    private CacheServiceV2 cacheService;

    /**
     * 获取LookUp项列表
     *
     * @param queryDTO 查询条件DTO
     * @return 分页结果，包含LookUp项列表和分页信息
     */
    @Override
    public ApiResponse<PageVO<ItemListVO>> getItemList(ItemQueryDTO queryDTO) {
        Long classifyId = queryDTO.getClassifyId();
        log.debug("Get item list, classifyId={}, queryDTO={}", classifyId, queryDTO);

        ClassifyEntity classify = classifyMapper.selectById(classifyId);
        if (classify == null) {
            return ApiResponse.error(ResponseCodeEnum.NOT_FOUND,
                    "分类不存在: " + classifyId,
                    "Classify not found: " + classifyId);
        }

        int offset = (queryDTO.getPageNum() - 1) * queryDTO.getPageSize();
        queryDTO.setOffset(offset);

        if (StringUtils.hasText(queryDTO.getItemCode())) {
            queryDTO.setItemCode("%" + queryDTO.getItemCode() + "%");
        }
        if (StringUtils.hasText(queryDTO.getItemName())) {
            queryDTO.setItemName("%" + queryDTO.getItemName() + "%");
        }

        List<LookUpItemEntity> entityList = lookUpItemMapper.selectList(queryDTO);

        long total = lookUpItemMapper.countList(queryDTO);

        List<ItemListVO> voList = entityList.stream()
                .map(this::convertToListVO)
                .collect(Collectors.toList());

        PageVO<ItemListVO> pageVO = PageVO.of(voList, total, queryDTO.getPageNum(), queryDTO.getPageSize());

        return ApiResponse.success(pageVO);
    }

    /**
     * 创建LookUp项
     *
     * @param classifyId 分类ID
     * @param createDTO  创建DTO
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> createItem(Long classifyId, ItemCreateDTO createDTO) {
        log.info("Create item, classifyId={}, itemCode={}", classifyId, createDTO.getItemCode());

        ClassifyEntity classify = classifyMapper.selectById(classifyId);
        if (classify == null) {
            return ApiResponse.error(ResponseCodeEnum.NOT_FOUND,
                    "分类不存在: " + classifyId,
                    "Classify not found: " + classifyId);
        }

        LookUpItemEntity existingItem = lookUpItemMapper.selectByClassifyIdAndCode(
                classifyId, createDTO.getItemCode());
        if (existingItem != null) {
            return ApiResponse.error(ResponseCodeEnum.ITEM_CODE_EXISTS,
                    "项编码已存在: " + createDTO.getItemCode(),
                    "Item code already exists: " + createDTO.getItemCode());
        }

        LookUpItemEntity entity = new LookUpItemEntity();
        entity.setClassifyId(classifyId);
        entity.setItemCode(createDTO.getItemCode());
        entity.setItemName(createDTO.getItemName());
        entity.setItemValue(createDTO.getItemValue());
        entity.setItemIndex(createDTO.getItemIndex());
        entity.setItemDesc(createDTO.getItemDesc());
        entity.setItemAttr1(createDTO.getItemAttr1());
        entity.setItemAttr2(createDTO.getItemAttr2());
        entity.setItemAttr3(createDTO.getItemAttr3());
        entity.setItemAttr4(createDTO.getItemAttr4());
        entity.setItemAttr5(createDTO.getItemAttr5());
        entity.setItemAttr6(createDTO.getItemAttr6());
        entity.setStatus(StatusEnum.ACTIVE.getCode());
        entity.setCreateBy(UserContextHolder.getUserId());
        entity.setCreateTime(new Date());
        entity.setLastUpdateBy(UserContextHolder.getUserId());
        entity.setLastUpdateTime(new Date());
        entity.setItemId(idGenerator.nextId());

        lookUpItemMapper.insert(entity);

        log.info("Item created successfully, itemId={}", entity.getItemId());
        ClassifyEntity classifyForCache = classifyMapper.selectById(classifyId);
        if (classifyForCache != null) {
            cacheService.clearLookUpItemCache(classifyForCache.getPath(), classifyForCache.getClassifyCode());
        }

        return ApiResponse.success();
    }

    /**
     * 更新LookUp项
     *
     * @param itemId     项ID
     * @param updateDTO  更新DTO
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> updateItem(Long itemId, ItemUpdateDTO updateDTO) {
        log.info("Update item, itemId={}, itemName={}", itemId, updateDTO.getItemName());

        LookUpItemEntity entity = lookUpItemMapper.selectById(itemId);
        if (entity == null) {
            return ApiResponse.error(ResponseCodeEnum.ITEM_NOT_FOUND,
                    "LookUp项不存在: " + itemId,
                    "LookUp item not found: " + itemId);
        }

        entity.setItemName(updateDTO.getItemName());
        entity.setItemValue(updateDTO.getItemValue());
        entity.setItemIndex(updateDTO.getItemIndex());
        entity.setItemDesc(updateDTO.getItemDesc());
        entity.setItemAttr1(updateDTO.getItemAttr1());
        entity.setItemAttr2(updateDTO.getItemAttr2());
        entity.setItemAttr3(updateDTO.getItemAttr3());
        entity.setItemAttr4(updateDTO.getItemAttr4());
        entity.setItemAttr5(updateDTO.getItemAttr5());
        entity.setItemAttr6(updateDTO.getItemAttr6());
        if (updateDTO.getStatus() != null) {
            entity.setStatus(updateDTO.getStatus());
        }
        entity.setLastUpdateBy(UserContextHolder.getUserId());
        entity.setLastUpdateTime(new Date());

        lookUpItemMapper.update(entity);

        log.info("Item updated successfully, itemId={}", itemId);
        ClassifyEntity classifyForCache = classifyMapper.selectById(entity.getClassifyId());
        if (classifyForCache != null) {
            cacheService.clearLookUpItemCache(classifyForCache.getPath(), classifyForCache.getClassifyCode());
        }

        return ApiResponse.success();
    }

    /**
     * 删除LookUp项
     *
     * @param itemId 项ID
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> deleteItem(Long itemId) {
        log.info("Delete item, itemId={}", itemId);

        LookUpItemEntity entity = lookUpItemMapper.selectById(itemId);
        if (entity == null) {
            return ApiResponse.error(ResponseCodeEnum.ITEM_NOT_FOUND,
                    "LookUp项不存在: " + itemId,
                    "LookUp item not found: " + itemId);
        }

        if (entity.getStatus() != null && entity.getStatus() == 1) {
            return ApiResponse.error(ResponseCodeEnum.ITEM_STATUS_CANNOT_DELETE,
                    "LookUp项状态为有效，无法删除。请先将项设置为失效状态",
                    "LookUp item is effective, cannot delete. Please set status to ineffective first");
        }

        lookUpItemMapper.deleteById(itemId);

        log.info("Item deleted successfully, itemId={}", itemId);
        ClassifyEntity classifyForCache = classifyMapper.selectById(entity.getClassifyId());
        if (classifyForCache != null) {
            cacheService.clearLookUpItemCache(classifyForCache.getPath(), classifyForCache.getClassifyCode());
        }

        return ApiResponse.success();
    }

    /**
     * 获取LookUp项详情
     *
     * @param itemId 项ID
     * @return 项详情
     */
    @Override
    public ApiResponse<ItemDetailVO> getItemDetail(Long itemId) {
        log.info("Get item detail, itemId={}", itemId);

        LookUpItemEntity entity = lookUpItemMapper.selectById(itemId);
        if (entity == null) {
            return ApiResponse.error(ResponseCodeEnum.ITEM_NOT_FOUND,
                    "LookUp项不存在: " + itemId,
                    "LookUp item not found: " + itemId);
        }

        ItemDetailVO vo = convertToDetailVO(entity);

        return ApiResponse.success(vo);
    }

    private ItemListVO convertToListVO(LookUpItemEntity entity) {
        ItemListVO vo = new ItemListVO();
        vo.setItemId(entity.getItemId());
        vo.setClassifyId(entity.getClassifyId());
        vo.setItemCode(entity.getItemCode());
        vo.setItemName(entity.getItemName());
        vo.setItemValue(entity.getItemValue());
        vo.setItemIndex(entity.getItemIndex());
        vo.setItemDesc(entity.getItemDesc());
        vo.setItemAttr1(entity.getItemAttr1());
        vo.setItemAttr2(entity.getItemAttr2());
        vo.setItemAttr3(entity.getItemAttr3());
        vo.setItemAttr4(entity.getItemAttr4());
        vo.setItemAttr5(entity.getItemAttr5());
        vo.setItemAttr6(entity.getItemAttr6());
        vo.setStatus(entity.getStatus());
        vo.setStatusName(StatusEnum.getNameByCode(entity.getStatus()));
        vo.setCreateBy(entity.getCreateBy());
        vo.setCreateTime(entity.getCreateTime());
        vo.setLastUpdateBy(entity.getLastUpdateBy());
        vo.setLastUpdateTime(entity.getLastUpdateTime());
        return vo;
    }

    private ItemDetailVO convertToDetailVO(LookUpItemEntity entity) {
        ItemDetailVO vo = new ItemDetailVO();
        vo.setItemId(entity.getItemId());
        vo.setClassifyId(entity.getClassifyId());
        vo.setClassifyName(entity.getClassifyName());
        vo.setItemCode(entity.getItemCode());
        vo.setItemName(entity.getItemName());
        vo.setItemValue(entity.getItemValue());
        vo.setItemIndex(entity.getItemIndex());
        vo.setItemDesc(entity.getItemDesc());
        vo.setItemAttr1(entity.getItemAttr1());
        vo.setItemAttr2(entity.getItemAttr2());
        vo.setItemAttr3(entity.getItemAttr3());
        vo.setItemAttr4(entity.getItemAttr4());
        vo.setItemAttr5(entity.getItemAttr5());
        vo.setItemAttr6(entity.getItemAttr6());
        vo.setStatus(entity.getStatus());
        vo.setStatusName(StatusEnum.getNameByCode(entity.getStatus()));
        vo.setCreateBy(entity.getCreateBy());
        vo.setCreateTime(entity.getCreateTime());
        vo.setLastUpdateBy(entity.getLastUpdateBy());
        vo.setLastUpdateTime(entity.getLastUpdateTime());
        return vo;
    }
}