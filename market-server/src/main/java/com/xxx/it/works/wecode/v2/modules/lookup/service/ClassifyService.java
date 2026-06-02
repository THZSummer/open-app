package com.xxx.it.works.wecode.v2.modules.lookup.service;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.classify.ClassifyCreateDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.classify.ClassifyQueryDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.classify.ClassifyUpdateDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.classify.ClassifyListVO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.classify.ClassifyVO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.common.PageVO;

/**
 * 分类服务接口
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
public interface ClassifyService {

    /**
     * 查询分类列表
     *
     * @param queryDTO 查询条件
     * @return 分页列表
     */
    ApiResponse<PageVO<ClassifyListVO>> getClassifyList(ClassifyQueryDTO queryDTO);

    /**
     * 新增分类
     *
     * @param createDTO 新增数据
     * @return 操作结果
     */
    ApiResponse<Void> createClassify(ClassifyCreateDTO createDTO);

    /**
     * 编辑分类
     *
     * @param classifyId 分类ID
     * @param updateDTO 更新数据
     * @return 操作结果
     */
    ApiResponse<Void> updateClassify(Long classifyId, ClassifyUpdateDTO updateDTO);

    /**
     * 删除分类
     *
     * @param classifyId 分类ID
     * @return 操作结果
     */
    ApiResponse<Void> deleteClassify(Long classifyId);

    /**
     * 获取分类详情
     *
     * @param classifyId 分类ID
     * @return 分类详情
     */
    ApiResponse<ClassifyVO> getClassifyDetail(Long classifyId);
}
