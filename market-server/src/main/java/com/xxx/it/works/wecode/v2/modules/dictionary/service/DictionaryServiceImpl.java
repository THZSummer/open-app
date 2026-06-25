package com.xxx.it.works.wecode.v2.modules.dictionary.service;

import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.common.service.CacheServiceV2;
import com.xxx.it.works.wecode.v2.modules.dictionary.dto.DictionaryCreateDTO;
import com.xxx.it.works.wecode.v2.modules.dictionary.dto.DictionaryQueryDTO;
import com.xxx.it.works.wecode.v2.modules.dictionary.dto.DictionaryUpdateDTO;
import com.xxx.it.works.wecode.v2.modules.dictionary.vo.DictionaryListVO;
import com.xxx.it.works.wecode.v2.modules.dictionary.vo.DictionaryVO;
import com.xxx.it.works.wecode.v2.modules.dictionary.entity.DictionaryEntity;
import com.xxx.it.works.wecode.v2.modules.dictionary.mapper.DictionaryMapper;
import com.xxx.it.works.wecode.v2.modules.lookup.enums.StatusEnum;
import com.xxx.it.works.wecode.v2.common.enums.ResponseCodeEnum;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.common.PageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据字典服务实现
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Service
public class DictionaryServiceImpl implements DictionaryService {

    @Autowired
    private DictionaryMapper dictionaryMapper;
    @Autowired
    private IdGeneratorStrategy idGenerator;
    @Autowired
    private CacheServiceV2 cacheService;

    @Override
    public ApiResponse<PageVO<DictionaryListVO>> getDictionaryList(DictionaryQueryDTO queryDTO) {
        log.debug("Get dictionary list, queryDTO={}", queryDTO);

        int offset = (queryDTO.getPageNum() - 1) * queryDTO.getPageSize();
        queryDTO.setOffset(offset);

        if (StringUtils.hasText(queryDTO.getCode())) {
            queryDTO.setCode("%" + queryDTO.getCode() + "%");
        }
        if (StringUtils.hasText(queryDTO.getName())) {
            queryDTO.setName("%" + queryDTO.getName() + "%");
        }
        if (StringUtils.hasText(queryDTO.getPath())) {
            queryDTO.setPath("%" + queryDTO.getPath() + "%");
        }

        List<DictionaryEntity> entityList = dictionaryMapper.selectList(queryDTO);

        long total = dictionaryMapper.countList(queryDTO);

        List<DictionaryListVO> voList = entityList.stream()
                .map(this::convertToListVO)
                .collect(Collectors.toList());

        PageVO<DictionaryListVO> pageVO = PageVO.of(voList, total, queryDTO.getPageNum(), queryDTO.getPageSize());

        return ApiResponse.success(pageVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> createDictionary(DictionaryCreateDTO createDTO) {
        log.info("Create dictionary, code={}", createDTO.getCode());

        DictionaryEntity existingEntity = dictionaryMapper.selectByPathAndCode(
                createDTO.getPath(), createDTO.getCode());
        if (existingEntity != null) {
            return ApiResponse.error(ResponseCodeEnum.ALREADY_EXISTS,
                    "编码 [" + createDTO.getCode() + "] 在路径 [" + createDTO.getPath() + "] 下已存在",
                    "Code [" + createDTO.getCode() + "] already exists in path [" + createDTO.getPath() + "]");
        }

        DictionaryEntity entity = new DictionaryEntity();
        entity.setId(idGenerator.nextId());
        entity.setCode(createDTO.getCode());
        entity.setName(createDTO.getName());
        entity.setValue(createDTO.getValue());
        entity.setPath(createDTO.getPath());
        entity.setDescription(createDTO.getDescription());
        entity.setStatus(StatusEnum.ACTIVE.getCode());
        entity.setCreateBy(UserContextHolder.getUserId());
        entity.setCreateTime(new Date());
        entity.setLastUpdateBy(UserContextHolder.getUserId());
        entity.setLastUpdateTime(new Date());
        entity.setLanguage(1);

        dictionaryMapper.insert(entity);

        log.info("Dictionary created successfully, id={}", entity.getId());

        cacheService.clearDictionaryCache(entity.getPath(), entity.getCode());

        return ApiResponse.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> updateDictionary(Long id, DictionaryUpdateDTO updateDTO) {
        log.info("Update dictionary, id={}, name={}", id, updateDTO.getName());

        DictionaryEntity entity = dictionaryMapper.selectById(id);
        if (entity == null) {
            return ApiResponse.error(ResponseCodeEnum.NOT_FOUND,
                    "数据字典不存在: " + id,
                    "Dictionary not found: " + id);
        }

        entity.setName(updateDTO.getName());
        entity.setValue(updateDTO.getValue());
        entity.setDescription(updateDTO.getDescription());
        if (updateDTO.getStatus() != null) {
            entity.setStatus(updateDTO.getStatus());
        }
        entity.setLastUpdateBy(UserContextHolder.getUserId());
        entity.setLastUpdateTime(new Date());

        dictionaryMapper.update(entity);

        log.info("Dictionary updated successfully, id={}", id);

        cacheService.clearDictionaryCache(entity.getPath(), entity.getCode());

        return ApiResponse.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> deleteDictionary(Long id) {
        log.info("Delete dictionary, id={}", id);

        DictionaryEntity entity = dictionaryMapper.selectById(id);
        if (entity == null) {
            return ApiResponse.error(ResponseCodeEnum.NOT_FOUND,
                    "数据字典不存在: " + id,
                    "Dictionary not found: " + id);
        }

        if (entity.getStatus() != null && entity.getStatus() == 1) {
            return ApiResponse.error(ResponseCodeEnum.STATUS_CANNOT_DELETE,
                    "数据字典状态为有效，无法删除。请先将数据字典设置为失效状态",
                    "Dictionary is effective, cannot delete. Please set status to ineffective first");
        }

        dictionaryMapper.deleteById(id);

        log.info("Dictionary deleted successfully, id={}", id);

        cacheService.clearDictionaryCache(entity.getPath(), entity.getCode());

        return ApiResponse.success();
    }

    @Override
    public ApiResponse<DictionaryVO> getDictionaryDetail(Long id) {
        log.info("Get dictionary detail, id={}", id);

        DictionaryEntity entity = dictionaryMapper.selectById(id);
        if (entity == null) {
            return ApiResponse.error(ResponseCodeEnum.NOT_FOUND,
                    "数据字典不存在: " + id,
                    "Dictionary not found: " + id);
        }

        DictionaryVO vo = convertToVO(entity);

        return ApiResponse.success(vo);
    }

    private DictionaryListVO convertToListVO(DictionaryEntity entity) {
        DictionaryListVO vo = new DictionaryListVO();
        vo.setId(String.valueOf(entity.getId()));
        vo.setCode(entity.getCode());
        vo.setName(entity.getName());
        vo.setValue(entity.getValue());
        vo.setDescription(entity.getDescription());
        vo.setPath(entity.getPath());
        vo.setStatus(entity.getStatus());
        vo.setStatusName(StatusEnum.getNameByCode(entity.getStatus()));
        vo.setCreateBy(entity.getCreateBy());
        vo.setCreateTime(entity.getCreateTime());
        vo.setLastUpdateBy(entity.getLastUpdateBy());
        vo.setLastUpdateTime(entity.getLastUpdateTime());
        return vo;
    }

    private DictionaryVO convertToVO(DictionaryEntity entity) {
        DictionaryVO vo = new DictionaryVO();
        vo.setId(String.valueOf(entity.getId()));
        vo.setCode(entity.getCode());
        vo.setName(entity.getName());
        vo.setValue(entity.getValue());
        vo.setDescription(entity.getDescription());
        vo.setPath(entity.getPath());
        vo.setStatus(entity.getStatus());
        vo.setStatusName(StatusEnum.getNameByCode(entity.getStatus()));
        vo.setCreateBy(entity.getCreateBy());
        vo.setCreateTime(entity.getCreateTime());
        vo.setLastUpdateBy(entity.getLastUpdateBy());
        vo.setLastUpdateTime(entity.getLastUpdateTime());
        return vo;
    }
}