package com.xxx.it.works.wecode.v2.modules.lookup.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.common.security.AuthRole;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.classify.ClassifyCreateDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.classify.ClassifyQueryDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.classify.ClassifyUpdateDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.service.ClassifyService;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.classify.ClassifyListVO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.classify.ClassifyVO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.common.PageVO;
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
 * 分类管理 Controller
 *
 * <p>提供分类的CRUD接口</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/service/open/v2/lookup/classify")
@Tag(name = "分类管理", description = "LookUp分类管理接口")
public class ClassifyController {

    @Autowired
    private ClassifyService classifyService;

    /**
     * 查询分类列表
     *
     * @param classifyCode 分类编码，模糊匹配
     * @param classifyName 分类名称，模糊匹配
     * @param classifyDesc 分类描述，模糊匹配
     * @param status 状态：0-失效，1-有效，对应 StatusEnum
     * @param pageNum 页码，默认1
     * @param pageSize 每页条数，默认10
     * @return 分页列表
     */
    @GetMapping("/list")
    @AuthRole
    @Operation(summary = "查询分类列表",
               description = "支持分页、模糊查询和状态筛选，返回包含项数量的列表")
    public ApiResponse<PageVO<ClassifyListVO>> getClassifyList(
            @Parameter(description = "分类编码，模糊匹配")
            @RequestParam(required = false) String classifyCode,
            @Parameter(description = "分类名称，模糊匹配")
            @RequestParam(required = false) String classifyName,
            @Parameter(description = "分类描述，模糊匹配")
            @RequestParam(required = false) String classifyDesc,
            @Parameter(description = "状态：0-失效(INACTIVE)，1-有效(ACTIVE)，对应 StatusEnum", required = false)
            @RequestParam(required = false) Integer status,
            @Parameter(description = "页码，默认1")
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数，默认10")
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {

        log.info("Get classify list, classifyCode={}, classifyName={}, status={}, pageNum={}, pageSize={}",
                classifyCode, classifyName, status, pageNum, pageSize);

        ClassifyQueryDTO queryDTO = new ClassifyQueryDTO();
        queryDTO.setClassifyCode(classifyCode);
        queryDTO.setClassifyName(classifyName);
        queryDTO.setClassifyDesc(classifyDesc);
        queryDTO.setStatus(status);
        queryDTO.setPageNum(pageNum);
        queryDTO.setPageSize(pageSize);

        return classifyService.getClassifyList(queryDTO);
    }

    /**
     * 新增分类
     *
     * @param createDTO 新增数据
     * @return 操作结果
     */
    @PostMapping
    @AuthRole
    @Operation(summary = "新增分类",
               description = "新增LookUp分类，分类编码在同级路径下唯一")
    public ApiResponse<Void> createClassify(
            @Valid @RequestBody ClassifyCreateDTO createDTO) {

        log.info("Create classify, classifyCode={}, classifyName={}",
                createDTO.getClassifyCode(), createDTO.getClassifyName());

        return classifyService.createClassify(createDTO);
    }

    /**
     * 编辑分类
     *
     * @param classifyId 分类ID
     * @param updateDTO 更新数据
     * @return 操作结果
     */
    @PutMapping("/{classifyId}")
    @AuthRole
    @Operation(summary = "编辑分类",
               description = "编辑分类信息，分类编码不可修改")
    public ApiResponse<Void> updateClassify(
            @Parameter(description = "分类ID", required = true)
            @PathVariable Long classifyId,
            @Valid @RequestBody ClassifyUpdateDTO updateDTO) {

        log.info("Update classify, classifyId={}", classifyId);

        updateDTO.setClassifyId(classifyId);

        return classifyService.updateClassify(classifyId, updateDTO);
    }

    /**
     * 删除分类
     *
     * @param classifyId 分类ID
     * @return 操作结果
     */
    @DeleteMapping("/{classifyId}")
    @AuthRole
    @Operation(summary = "删除分类",
               description = "删除分类，同时级联删除其下所有LookUp项")
    public ApiResponse<Void> deleteClassify(
            @Parameter(description = "分类ID", required = true)
            @PathVariable Long classifyId) {

        log.info("Delete classify, classifyId={}", classifyId);

        return classifyService.deleteClassify(classifyId);
    }

    /**
     * 获取分类详情
     *
     * @param classifyId 分类ID
     * @return 分类详情
     */
    @GetMapping("/{classifyId}")
    @AuthRole
    @Operation(summary = "获取分类详情",
               description = "获取分类详情，包含项数量统计")
    public ApiResponse<ClassifyVO> getClassifyDetail(
            @Parameter(description = "分类ID", required = true)
            @PathVariable Long classifyId) {

        log.info("Get classify detail, classifyId={}", classifyId);

        return classifyService.getClassifyDetail(classifyId);
    }
}
