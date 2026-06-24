package com.xxx.it.works.wecode.v2.modules.lookup.service;

import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.common.enums.ResponseCodeEnum;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.common.service.CacheServiceV2;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.classify.ClassifyCreateDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.classify.ClassifyQueryDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.classify.ClassifyUpdateDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.entity.ClassifyEntity;
import com.xxx.it.works.wecode.v2.modules.lookup.enums.StatusEnum;
import com.xxx.it.works.wecode.v2.modules.lookup.mapper.ClassifyMapper;
import com.xxx.it.works.wecode.v2.modules.lookup.mapper.LookUpItemMapper;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.classify.ClassifyListVO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.classify.ClassifyVO;
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
 * 分类服务实现
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Service
public class ClassifyServiceImpl implements ClassifyService {

    @Autowired
    private ClassifyMapper classifyMapper;
    @Autowired
    private LookUpItemMapper lookUpItemMapper;
    @Autowired
    private IdGeneratorStrategy idGenerator;
    @Autowired
    private CacheServiceV2 cacheService;

    /**
     * 获取分类列表（分页）
     *
     * @param queryDTO 查询条件
     * @return 分页分类列表
     */
    @Override
    public ApiResponse<PageVO<ClassifyListVO>> getClassifyList(ClassifyQueryDTO queryDTO) {
        log.debug("Get classify list, queryDTO={}", queryDTO);

        // 计算分页参数
        int offset = (queryDTO.getPageNum() - 1) * queryDTO.getPageSize();
        queryDTO.setOffset(offset);

        // 拼接模糊查询的 %
        if (StringUtils.hasText(queryDTO.getClassifyCode())) {
            queryDTO.setClassifyCode("%" + queryDTO.getClassifyCode() + "%");
        }
        if (StringUtils.hasText(queryDTO.getClassifyName())) {
            queryDTO.setClassifyName("%" + queryDTO.getClassifyName() + "%");
        }
        if (StringUtils.hasText(queryDTO.getClassifyDesc())) {
            queryDTO.setClassifyDesc("%" + queryDTO.getClassifyDesc() + "%");
        }

        // 查询列表
        List<ClassifyEntity> entityList = classifyMapper.selectList(queryDTO);

        // 统计总数
        long total = classifyMapper.countList(queryDTO);

        // 转换为VO并统计项数量
        List<ClassifyListVO> voList = entityList.stream().map(this::convertToListVO).collect(Collectors.toList());

        // 构建分页结果
        PageVO<ClassifyListVO> pageVO = PageVO.of(voList, total, queryDTO.getPageNum(), queryDTO.getPageSize());

        return ApiResponse.success(pageVO);
    }

    /**
     * 创建分类
     *
     * @param createDTO 创建参数
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> createClassify(ClassifyCreateDTO createDTO) {
        log.info("Create classify, classifyCode={}, classifyName={}", createDTO.getClassifyCode(), createDTO.getClassifyName());

        // 校验分类编码唯一性（同path下）
        String path = createDTO.getPath();
        ClassifyEntity existingClassify = classifyMapper.selectByCodeAndPath(createDTO.getClassifyCode(), path);
        if (existingClassify != null) {
            return ApiResponse.error(ResponseCodeEnum.ALREADY_EXISTS, "分类编码已存在: " + createDTO.getClassifyCode(), "Classify code already exists: " + createDTO.getClassifyCode());
        }

        // 创建实体
        ClassifyEntity entity = new ClassifyEntity();
        entity.setClassifyCode(createDTO.getClassifyCode());
        entity.setClassifyName(createDTO.getClassifyName());
        entity.setPath(path);
        entity.setClassifyDesc(createDTO.getClassifyDesc());
        entity.setStatus(StatusEnum.ACTIVE.getCode());
        entity.setCreateBy(UserContextHolder.getUserId());
        entity.setCreateTime(new Date());
        entity.setLastUpdateBy(UserContextHolder.getUserId());
        entity.setLastUpdateTime(new Date());
        entity.setClassifyId(idGenerator.nextId());

        // 插入数据
        classifyMapper.insert(entity);

        log.info("Classify created successfully, classifyId={}", entity.getClassifyId());

        cacheService.clearLookUpItemCache(entity.getPath(), entity.getClassifyCode());

        return ApiResponse.success();
    }

    /**
     * 更新分类
     *
     * @param classifyId 分类ID
     * @param updateDTO  更新参数
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> updateClassify(Long classifyId, ClassifyUpdateDTO updateDTO) {
        log.info("Update classify, classifyId={}, classifyName={}", classifyId, updateDTO.getClassifyName());

        // 校验分类是否存在
        ClassifyEntity entity = classifyMapper.selectById(classifyId);
        if (entity == null) {
            return ApiResponse.error(ResponseCodeEnum.NOT_FOUND, "分类不存在: " + classifyId, "Classify not found: " + classifyId);
        }

        // 更新字段
        entity.setClassifyName(updateDTO.getClassifyName());
        entity.setClassifyDesc(updateDTO.getClassifyDesc());
        if (updateDTO.getStatus() != null) {
            entity.setStatus(updateDTO.getStatus());
        }
        entity.setLastUpdateBy(UserContextHolder.getUserId());
        entity.setLastUpdateTime(new Date());

        // 更新数据
        classifyMapper.update(entity);

        log.info("Classify updated successfully, classifyId={}", classifyId);

        cacheService.clearLookUpItemCache(entity.getPath(), entity.getClassifyCode());

        return ApiResponse.success();
    }

    /**
     * 删除分类
     *
     * @param classifyId 分类ID
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> deleteClassify(Long classifyId) {
        log.info("Delete classify, classifyId={}", classifyId);

        // 校验分类是否存在
        ClassifyEntity entity = classifyMapper.selectById(classifyId);
        if (entity == null) {
            return ApiResponse.error(ResponseCodeEnum.NOT_FOUND, "分类不存在: " + classifyId, "Classify not found: " + classifyId);
        }

        // 校验分类状态必须为失效(0)才能删除
        if (entity.getStatus() != null && entity.getStatus() == 1) {
            return ApiResponse.error(ResponseCodeEnum.STATUS_CANNOT_DELETE);
        }

        // 删除分类下的所有LookUp项（级联删除）
        lookUpItemMapper.deleteByClassifyId(classifyId);

        // 删除分类
        classifyMapper.deleteById(classifyId);

        cacheService.clearLookUpItemCache(entity.getPath(), entity.getClassifyCode());

        log.info("Classify deleted successfully, classifyId={}", classifyId);

        return ApiResponse.success();
    }

    /**
     * 获取分类详情
     *
     * @param classifyId 分类ID
     * @return 分类详情
     */
    @Override
    public ApiResponse<ClassifyVO> getClassifyDetail(Long classifyId) {
        log.debug("Get classify detail, classifyId={}", classifyId);

        // 查询分类
        ClassifyEntity entity = classifyMapper.selectById(classifyId);
        if (entity == null) {
            return ApiResponse.error(ResponseCodeEnum.NOT_FOUND, "分类不存在: " + classifyId, "Classify not found: " + classifyId);
        }

        // 转换为VO
        ClassifyVO vo = convertToVO(entity);

        return ApiResponse.success(vo);
    }

    /**
     * 转换为列表VO
     */
    private ClassifyListVO convertToListVO(ClassifyEntity entity) {
        ClassifyListVO vo = new ClassifyListVO();
        vo.setClassifyId(String.valueOf(entity.getClassifyId()));
        vo.setClassifyCode(entity.getClassifyCode());
        vo.setClassifyName(entity.getClassifyName());
        vo.setPath(entity.getPath());
        vo.setClassifyDesc(entity.getClassifyDesc());
        vo.setStatus(entity.getStatus());
        vo.setStatusName(StatusEnum.getNameByCode(entity.getStatus()));
        vo.setCreateBy(entity.getCreateBy());
        vo.setCreateTime(entity.getCreateTime());
        vo.setLastUpdateBy(entity.getLastUpdateBy());
        vo.setLastUpdateTime(entity.getLastUpdateTime());

        return vo;
    }

    /**
     * 转换为详情VO
     */
    private ClassifyVO convertToVO(ClassifyEntity entity) {
        ClassifyVO vo = new ClassifyVO();
        vo.setClassifyId(String.valueOf(entity.getClassifyId()));
        vo.setClassifyCode(entity.getClassifyCode());
        vo.setClassifyName(entity.getClassifyName());
        vo.setPath(entity.getPath());
        vo.setClassifyDesc(entity.getClassifyDesc());
        vo.setStatus(entity.getStatus());
        vo.setStatusName(StatusEnum.getNameByCode(entity.getStatus()));
        vo.setCreateBy(entity.getCreateBy());
        vo.setCreateTime(entity.getCreateTime());
        vo.setLastUpdateBy(entity.getLastUpdateBy());
        vo.setLastUpdateTime(entity.getLastUpdateTime());
        return vo;
    }
}
