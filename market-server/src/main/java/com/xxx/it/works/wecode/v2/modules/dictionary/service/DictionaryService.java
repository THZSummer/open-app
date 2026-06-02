package com.xxx.it.works.wecode.v2.modules.dictionary.service;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.dictionary.dto.DictionaryCreateDTO;
import com.xxx.it.works.wecode.v2.modules.dictionary.dto.DictionaryQueryDTO;
import com.xxx.it.works.wecode.v2.modules.dictionary.dto.DictionaryUpdateDTO;
import com.xxx.it.works.wecode.v2.modules.dictionary.vo.DictionaryListVO;
import com.xxx.it.works.wecode.v2.modules.dictionary.vo.DictionaryVO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.common.PageVO;

/**
 * 数据字典服务接口
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
public interface DictionaryService {

    /**
     * 查询数据字典列表
     *
     * @param queryDTO 查询条件
     * @return 分页列表
     */
    ApiResponse<PageVO<DictionaryListVO>> getDictionaryList(DictionaryQueryDTO queryDTO);

    /**
     * 新增数据字典
     *
     * @param createDTO 新增数据
     * @return 操作结果
     */
    ApiResponse<Void> createDictionary(DictionaryCreateDTO createDTO);

    /**
     * 编辑数据字典
     *
     * @param id 数据字典ID
     * @param updateDTO 更新数据
     * @return 操作结果
     */
    ApiResponse<Void> updateDictionary(Long id, DictionaryUpdateDTO updateDTO);

    /**
     * 删除数据字典
     *
     * @param id 数据字典ID
     * @return 操作结果
     */
    ApiResponse<Void> deleteDictionary(Long id);

    /**
     * 获取数据字典详情
     *
     * @param id 数据字典ID
     * @return 数据字典详情
     */
    ApiResponse<DictionaryVO> getDictionaryDetail(Long id);
}