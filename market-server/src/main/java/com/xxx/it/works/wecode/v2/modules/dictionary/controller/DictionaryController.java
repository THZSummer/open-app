package com.xxx.it.works.wecode.v2.modules.dictionary.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.common.security.AuthRole;
import com.xxx.it.works.wecode.v2.modules.dictionary.dto.DictionaryCreateDTO;
import com.xxx.it.works.wecode.v2.modules.dictionary.dto.DictionaryQueryDTO;
import com.xxx.it.works.wecode.v2.modules.dictionary.dto.DictionaryUpdateDTO;
import com.xxx.it.works.wecode.v2.modules.dictionary.vo.DictionaryListVO;
import com.xxx.it.works.wecode.v2.modules.dictionary.vo.DictionaryVO;
import com.xxx.it.works.wecode.v2.modules.dictionary.service.DictionaryService;
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
 * 数据字典管理 Controller
 *
 * <p>提供数据字典的CRUD接口</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/service/open/v2/dictionary")
@Tag(name = "数据字典管理", description = "数据字典管理接口")
public class DictionaryController {

    @Autowired
    private DictionaryService dictionaryService;

    /**
     * 查询数据字典列表
     *
     * @param code 编码，模糊匹配
     * @param name 名称，模糊匹配
     * @param path 路径，模糊匹配
     * @param status 状态：0-失效，1-有效
     * @param pageNum 页码，默认1
     * @param pageSize 每页条数，默认10
     * @return 分页列表
     */
    @GetMapping("/list")
    @AuthRole
    @Operation(summary = "查询数据字典列表",
               description = "查询数据字典列表，支持分页和条件筛选")
    public ApiResponse<PageVO<DictionaryListVO>> getDictionaryList(
            @Parameter(description = "编码，模糊匹配")
            @RequestParam(required = false) String code,
            @Parameter(description = "名称，模糊匹配")
            @RequestParam(required = false) String name,
            @Parameter(description = "路径，模糊匹配")
            @RequestParam(required = false) String path,
            @Parameter(description = "状态：0-失效，1-有效")
            @RequestParam(required = false) Integer status,
            @Parameter(description = "页码，默认1")
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数，默认10")
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {

        log.info("Get dictionary list, code={}, name={}, path={}, status={}, pageNum={}, pageSize={}",
                code, name, path, status, pageNum, pageSize);

        DictionaryQueryDTO queryDTO = new DictionaryQueryDTO();
        queryDTO.setCode(code);
        queryDTO.setName(name);
        queryDTO.setPath(path);
        queryDTO.setStatus(status);
        queryDTO.setPageNum(pageNum);
        queryDTO.setPageSize(pageSize);

        return dictionaryService.getDictionaryList(queryDTO);
    }

    /**
     * 新增数据字典
     *
     * @param createDTO 新增数据
     * @return 操作结果
     */
    @PostMapping
    @AuthRole
    @Operation(summary = "新增数据字典",
               description = "新增数据字典，编码在同一路径下唯一")
    public ApiResponse<Void> createDictionary(
            @Valid @RequestBody DictionaryCreateDTO createDTO) {

        log.info("Create dictionary, code={}", createDTO.getCode());

        return dictionaryService.createDictionary(createDTO);
    }

    /**
     * 获取数据字典详情
     *
     * @param id 数据字典ID
     * @return 数据字典详情
     */
    @GetMapping("/{id}")
    @AuthRole
    @Operation(summary = "获取数据字典详情",
               description = "获取数据字典完整详情")
    public ApiResponse<DictionaryVO> getDictionaryDetail(
            @Parameter(description = "数据字典ID", required = true)
            @PathVariable Long id) {

        log.info("Get dictionary detail, id={}", id);

        return dictionaryService.getDictionaryDetail(id);
    }

    /**
     * 编辑数据字典
     *
     * @param id 数据字典ID
     * @param updateDTO 更新数据
     * @return 操作结果
     */
    @PutMapping("/{id}")
    @AuthRole
    @Operation(summary = "编辑数据字典",
               description = "编辑数据字典信息，编码不可修改")
    public ApiResponse<Void> updateDictionary(
            @Parameter(description = "数据字典ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody DictionaryUpdateDTO updateDTO) {

        log.info("Update dictionary, id={}", id);

        return dictionaryService.updateDictionary(id, updateDTO);
    }

    /**
     * 删除数据字典
     *
     * @param id 数据字典ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @AuthRole
    @Operation(summary = "删除数据字典",
               description = "删除指定的数据字典，仅状态为失效时可删除")
    public ApiResponse<Void> deleteDictionary(
            @Parameter(description = "数据字典ID", required = true)
            @PathVariable Long id) {

        log.info("Delete dictionary, id={}", id);

        return dictionaryService.deleteDictionary(id);
    }
}