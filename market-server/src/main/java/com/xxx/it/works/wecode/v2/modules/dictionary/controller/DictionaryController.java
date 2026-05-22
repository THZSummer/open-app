package com.xxx.it.works.wecode.v2.modules.dictionary.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.common.security.PlatformAdminPermission;
import com.xxx.it.works.wecode.v2.modules.dictionary.api.dto.DictionaryCreateDTO;
import com.xxx.it.works.wecode.v2.modules.dictionary.api.dto.DictionaryQueryDTO;
import com.xxx.it.works.wecode.v2.modules.dictionary.api.dto.DictionaryUpdateDTO;
import com.xxx.it.works.wecode.v2.modules.dictionary.api.vo.DictionaryListVO;
import com.xxx.it.works.wecode.v2.modules.dictionary.api.vo.DictionaryVO;
import com.xxx.it.works.wecode.v2.modules.dictionary.service.DictionaryService;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.common.PageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 数据字典管理 Controller
 *
 * <p>提供数据字典的CRUD接口和批量导入导出功能</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/dictionary")
@RequiredArgsConstructor
@Tag(name = "数据字典管理", description = "数据字典管理接口")
public class DictionaryController {

    private final DictionaryService dictionaryService;

    /**
     * 查询数据字典列表
     *
     * @param code 编码，模糊匹配
     * @param name 名称，模糊匹配
     * @param path 路径，模糊匹配
     * @param language 语言：1-中文，2-英文
     * @param status 状态：0-失效，1-有效
     * @param pageNum 页码，默认1
     * @param pageSize 每页条数，默认10
     * @return 分页列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询数据字典列表",
               description = "查询数据字典列表，支持分页和条件筛选")
    public ApiResponse<PageVO<DictionaryListVO>> getDictionaryList(
            @Parameter(description = "编码，模糊匹配")
            @RequestParam(required = false) String code,
            @Parameter(description = "名称，模糊匹配")
            @RequestParam(required = false) String name,
            @Parameter(description = "路径，模糊匹配")
            @RequestParam(required = false) String path,
            @Parameter(description = "语言：1-中文，2-英文")
            @RequestParam(required = false) Integer language,
            @Parameter(description = "状态：0-失效，1-有效")
            @RequestParam(required = false) Integer status,
            @Parameter(description = "页码，默认1")
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数，默认10")
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {

        log.info("Get dictionary list, code={}, name={}, path={}, language={}, status={}, pageNum={}, pageSize={}",
                code, name, path, language, status, pageNum, pageSize);

        DictionaryQueryDTO queryDTO = new DictionaryQueryDTO();
        queryDTO.setCode(code);
        queryDTO.setName(name);
        queryDTO.setPath(path);
        queryDTO.setLanguage(language);
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
    @PlatformAdminPermission
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
    @PlatformAdminPermission
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
    @PlatformAdminPermission
    @Operation(summary = "删除数据字典",
               description = "删除指定的数据字典，仅状态为失效时可删除")
    public ApiResponse<Void> deleteDictionary(
            @Parameter(description = "数据字典ID", required = true)
            @PathVariable Long id) {

        log.info("Delete dictionary, id={}", id);

        return dictionaryService.deleteDictionary(id);
    }

    /**
     * 下载导入模板
     *
     * @param response HTTP响应
     */
    @GetMapping("/import/template")
    @PlatformAdminPermission
    @Operation(summary = "下载导入模板",
               description = "下载数据字典批量导入的Excel模板文件")
    public void downloadTemplate(HttpServletResponse response) {
        log.info("Download import template");

        dictionaryService.downloadTemplate(response);
    }

    /**
     * 异步导入数据字典
     *
     * @param file Excel文件
     * @return 任务ID
     */
    @PostMapping("/import/async")
    @PlatformAdminPermission
    @Operation(summary = "异步导入数据字典",
               description = "从Excel文件异步批量导入数据字典，单次最多1000条")
    public ApiResponse<Long> importDictionaryAsync(
            @Parameter(description = "Excel文件", required = true)
            @RequestParam MultipartFile file) {

        log.info("Import dictionary async, fileName={}", file.getOriginalFilename());

        return dictionaryService.importDictionaryAsync(file);
    }

    /**
     * 异步导出数据字典
     *
     * @param selectedIds 选中的ID列表
     * @param code 编码筛选
     * @param name 名称筛选
     * @param path 路径筛选
     * @param language 语言筛选
     * @param status 状态筛选
     * @return 任务ID
     */
    @PostMapping("/export/async")
    @PlatformAdminPermission
    @Operation(summary = "异步导出数据字典",
               description = "异步导出数据字典到Excel文件，单次最多1000条")
    public ApiResponse<Long> exportDictionaryAsync(
            @Parameter(description = "选中的ID列表")
            @RequestParam(required = false) List<Long> selectedIds,
            @Parameter(description = "编码，模糊匹配")
            @RequestParam(required = false) String code,
            @Parameter(description = "名称，模糊匹配")
            @RequestParam(required = false) String name,
            @Parameter(description = "路径，模糊匹配")
            @RequestParam(required = false) String path,
            @Parameter(description = "语言：1-中文，2-英文")
            @RequestParam(required = false) Integer language,
            @Parameter(description = "状态：0-失效，1-有效")
            @RequestParam(required = false) Integer status) {

        log.info("Export dictionary async, selectedIds={}, code={}, name={}, path={}, language={}, status={}",
                selectedIds, code, name, path, language, status);

        return dictionaryService.exportDictionaryAsync(selectedIds, code, name, path, language, status);
    }
}
