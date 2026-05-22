package com.xxx.it.works.wecode.v2.modules.dictionary.service;

import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.dictionary.api.dto.DictionaryCreateDTO;
import com.xxx.it.works.wecode.v2.modules.dictionary.api.dto.DictionaryQueryDTO;
import com.xxx.it.works.wecode.v2.modules.dictionary.api.dto.DictionaryUpdateDTO;
import com.xxx.it.works.wecode.v2.modules.dictionary.api.vo.DictionaryListVO;
import com.xxx.it.works.wecode.v2.modules.dictionary.api.vo.DictionaryVO;
import com.xxx.it.works.wecode.v2.modules.dictionary.domain.entity.DictionaryEntity;
import com.xxx.it.works.wecode.v2.modules.dictionary.mapper.DictionaryMapper;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.task.TaskUpdateStatusDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.enums.BizTypeEnum;
import com.xxx.it.works.wecode.v2.modules.lookup.enums.StatusEnum;
import com.xxx.it.works.wecode.v2.modules.lookup.enums.TaskStatusEnum;
import com.xxx.it.works.wecode.v2.modules.lookup.enums.TaskTypeEnum;
import com.xxx.it.works.wecode.v2.modules.lookup.service.LookUpFileService;
import com.xxx.it.works.wecode.v2.modules.lookup.service.TaskService;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.common.PageVO;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 数据字典服务实现
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictionaryServiceImpl implements DictionaryService {

    private final DictionaryMapper dictionaryMapper;
    private final TaskService taskService;
    private final LookUpFileService lookUpFileService;

    private static final String[] EXCEL_HEADERS = {
            "编码", "名称", "值", "路径", "描述", "语言", "状态"
    };

    private static final int BATCH_SIZE = 100;
    private static final int MAX_IMPORT_ROWS = 1000;
    private static final int MAX_EXPORT_ROWS = 1000;

    @Override
    public ApiResponse<PageVO<DictionaryListVO>> getDictionaryList(DictionaryQueryDTO queryDTO) {
        log.debug("Get dictionary list, queryDTO={}", queryDTO);

        int offset = (queryDTO.getPageNum() - 1) * queryDTO.getPageSize();
        int limit = queryDTO.getPageSize();

        List<DictionaryEntity> entityList = dictionaryMapper.selectList(
                queryDTO.getCode(),
                queryDTO.getName(),
                queryDTO.getPath(),
                queryDTO.getLanguage(),
                queryDTO.getStatus(),
                offset,
                limit
        );

        long total = dictionaryMapper.countList(
                queryDTO.getCode(),
                queryDTO.getName(),
                queryDTO.getPath(),
                queryDTO.getLanguage(),
                queryDTO.getStatus()
        );

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
            return ApiResponse.error("40901",
                    "编码 [" + createDTO.getCode() + "] 在路径 [" + createDTO.getPath() + "] 下已存在",
                    "Code [" + createDTO.getCode() + "] already exists in path [" + createDTO.getPath() + "]");
        }

        DictionaryEntity entity = new DictionaryEntity();
        entity.setCode(createDTO.getCode());
        entity.setName(createDTO.getName());
        entity.setValue(createDTO.getValue());
        entity.setPath(createDTO.getPath());
        entity.setDescription(createDTO.getDescription());
        entity.setLanguage(createDTO.getLanguage());
        entity.setStatus(StatusEnum.ACTIVE.getCode());
        entity.setCreateBy(UserContextHolder.getUserId());
        entity.setCreateTime(new Date());
        entity.setLastUpdateBy(UserContextHolder.getUserId());
        entity.setLastUpdateTime(new Date());

        dictionaryMapper.insert(entity);

        log.info("Dictionary created successfully, id={}", entity.getId());

        return ApiResponse.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> updateDictionary(Long id, DictionaryUpdateDTO updateDTO) {
        log.info("Update dictionary, id={}, name={}", id, updateDTO.getName());

        DictionaryEntity entity = dictionaryMapper.selectById(id);
        if (entity == null) {
            return ApiResponse.error("40401",
                    "数据字典不存在: " + id,
                    "Dictionary not found: " + id);
        }

        entity.setName(updateDTO.getName());
        entity.setValue(updateDTO.getValue());
        entity.setPath(updateDTO.getPath());
        entity.setDescription(updateDTO.getDescription());
        if (updateDTO.getStatus() != null) {
            entity.setStatus(updateDTO.getStatus());
        }
        entity.setLastUpdateBy(UserContextHolder.getUserId());
        entity.setLastUpdateTime(new Date());

        dictionaryMapper.update(entity);

        log.info("Dictionary updated successfully, id={}", id);

        return ApiResponse.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> deleteDictionary(Long id) {
        log.info("Delete dictionary, id={}", id);

        DictionaryEntity entity = dictionaryMapper.selectById(id);
        if (entity == null) {
            return ApiResponse.error("40401",
                    "数据字典不存在: " + id,
                    "Dictionary not found: " + id);
        }

        if (entity.getStatus() != null && entity.getStatus() == 1) {
            return ApiResponse.error("40003",
                    "数据字典状态为有效，无法删除。请先将数据字典设置为失效状态",
                    "Dictionary is effective, cannot delete. Please set status to ineffective first");
        }

        dictionaryMapper.deleteById(id);

        log.info("Dictionary deleted successfully, id={}", id);

        return ApiResponse.success();
    }

    @Override
    public ApiResponse<DictionaryVO> getDictionaryDetail(Long id) {
        log.info("Get dictionary detail, id={}", id);

        DictionaryEntity entity = dictionaryMapper.selectById(id);
        if (entity == null) {
            return ApiResponse.error("40401",
                    "数据字典不存在: " + id,
                    "Dictionary not found: " + id);
        }

        DictionaryVO vo = convertToVO(entity);

        return ApiResponse.success(vo);
    }

    @Override
    public void downloadTemplate(HttpServletResponse response) {
        log.info("Download import template");

        String fileName = "Dictionary_Import_Template.xlsx";

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

            Workbook workbook = new XSSFWorkbook();

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Sheet sheet = workbook.createSheet("数据字典");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < EXCEL_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(EXCEL_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            Row exampleRow = sheet.createRow(1);
            exampleRow.createCell(0).setCellValue("USER_STATUS");
            exampleRow.createCell(1).setCellValue("用户状态");
            exampleRow.createCell(2).setCellValue("active");
            exampleRow.createCell(3).setCellValue("system/user");
            exampleRow.createCell(4).setCellValue("用户账户状态字典");
            exampleRow.createCell(5).setCellValue(1);
            exampleRow.createCell(6).setCellValue(1);

            for (int i = 0; i < EXCEL_HEADERS.length; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }

            try (ServletOutputStream os = response.getOutputStream()) {
                workbook.write(os);
                os.flush();
            }

            workbook.close();

            log.info("Template download completed");

        } catch (IOException e) {
            log.error("Failed to download template", e);
            throw new RuntimeException("Download template failed: " + e.getMessage(), e);
        }
    }

    @Override
    public ApiResponse<Long> importDictionaryAsync(MultipartFile file) {
        log.info("Import dictionary async, fileName={}", file.getOriginalFilename());

        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
            return ApiResponse.error("400",
                    "文件格式不支持，仅支持.xlsx或.xls格式",
                    "File format not supported, only .xlsx or .xls allowed");
        }

        try {
            int rowCount = countExcelRows(file);
            if (rowCount > MAX_IMPORT_ROWS) {
                return ApiResponse.error("40002",
                        "单次最多处理" + MAX_IMPORT_ROWS + "条数据，请分批操作",
                        "Max " + MAX_IMPORT_ROWS + " rows per batch, please process in batches");
            }
        } catch (IOException e) {
            return ApiResponse.error("400",
                    "文件解析失败: " + e.getMessage(),
                    "File parsing failed: " + e.getMessage());
        }

        Long taskId = createImportTask(file);

        processImportAsync(file, taskId);

        return ApiResponse.success(taskId);
    }

    private int countExcelRows(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream();
             Workbook workbook = file.getOriginalFilename().endsWith(".xlsx")
                     ? new XSSFWorkbook(is)
                     : new HSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            return sheet.getLastRowNum();
        }
    }

    private Long createImportTask(MultipartFile file) {
        ApiResponse<Long> taskResponse = taskService.createTask(
                TaskTypeEnum.IMPORT.getCode(),
                BizTypeEnum.DATA_DICTIONARY.getCode(),
                file.getOriginalFilename()
        );
        if (taskResponse.getCode() == null || !taskResponse.getCode().equals("200") || taskResponse.getData() == null) {
            throw new RuntimeException("Failed to create import task");
        }
        return taskResponse.getData();
    }

    private void processImportAsync(MultipartFile file, Long taskId) {
        File tempFile = null;
        try {
            TaskUpdateStatusDTO processingStatus = new TaskUpdateStatusDTO();
            processingStatus.setStatus(TaskStatusEnum.PROCESSING.getCode());
            taskService.updateTaskStatus(taskId, processingStatus);

            tempFile = saveToTempFile(file);

            doActualImport(tempFile, taskId);

            TaskUpdateStatusDTO completedStatus = new TaskUpdateStatusDTO();
            completedStatus.setStatus(TaskStatusEnum.COMPLETED.getCode());
            completedStatus.setResult("Import completed successfully");
            taskService.updateTaskStatus(taskId, completedStatus);

        } catch (Exception e) {
            log.error("Async import failed, taskId={}", taskId, e);
            TaskUpdateStatusDTO failedStatus = new TaskUpdateStatusDTO();
            failedStatus.setStatus(TaskStatusEnum.FAILED.getCode());
            failedStatus.setResult("Import failed: " + e.getMessage());
            taskService.updateTaskStatus(taskId, failedStatus);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private File saveToTempFile(MultipartFile file) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        String tempFileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        File tempFile = new File(tempDir, tempFileName);
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(file.getBytes());
        }
        return tempFile;
    }

    private void doActualImport(File tempFile, Long taskId) {
        try (InputStream is = Files.newInputStream(tempFile.toPath());
             Workbook workbook = tempFile.getName().endsWith(".xlsx")
                     ? new XSSFWorkbook(is)
                     : new HSSFWorkbook(is)) {

            List<DictionaryEntity> entitiesToInsert = new ArrayList<>();
            int totalCount = 0;
            int successCount = 0;

            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();

            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                totalCount++;

                String code = getCellValue(row.getCell(0));
                String name = getCellValue(row.getCell(1));
                String value = getCellValue(row.getCell(2));
                String path = getCellValue(row.getCell(3));
                String description = getCellValue(row.getCell(4));
                String languageStr = getCellValue(row.getCell(5));
                String statusStr = getCellValue(row.getCell(6));

                if (code == null || code.trim().isEmpty()) {
                    continue;
                }

                if (dictionaryMapper.checkCodeExists(path, code, null) > 0) {
                    continue;
                }

                DictionaryEntity entity = new DictionaryEntity();
                entity.setCode(code.trim());
                entity.setName(name != null ? name.trim() : "");
                entity.setValue(value);
                entity.setPath(path);
                entity.setDescription(description);
                entity.setLanguage(parseLanguage(languageStr));
                entity.setStatus(parseStatus(statusStr));
                entity.setCreateBy(UserContextHolder.getUserId());
                entity.setCreateTime(new Date());
                entity.setLastUpdateBy(UserContextHolder.getUserId());
                entity.setLastUpdateTime(new Date());

                entitiesToInsert.add(entity);
                successCount++;

                if (entitiesToInsert.size() >= BATCH_SIZE) {
                    dictionaryMapper.batchInsert(entitiesToInsert);
                    entitiesToInsert.clear();
                }
            }

            if (!entitiesToInsert.isEmpty()) {
                dictionaryMapper.batchInsert(entitiesToInsert);
            }

            log.info("Async import completed, taskId={}, total={}, success={}",
                    taskId, totalCount, successCount);

        } catch (IOException e) {
            throw new RuntimeException("File parsing failed: " + e.getMessage(), e);
        }
    }

    @Override
    public ApiResponse<Long> exportDictionaryAsync(List<Long> selectedIds,
                                                     String code,
                                                     String name,
                                                     String path,
                                                     Integer language,
                                                     Integer status) {
        log.info("Export dictionary async, selectedIds={}, code={}, name={}, path={}, language={}, status={}",
                selectedIds, code, name, path, language, status);

        Long taskId = createExportTask();

        processExportAsync(selectedIds, code, name, path, language, status, taskId);

        return ApiResponse.success(taskId);
    }

    private Long createExportTask() {
        String fileName = "Dictionary_Export_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".xlsx";
        ApiResponse<Long> taskResponse = taskService.createTask(
                TaskTypeEnum.EXPORT.getCode(),
                BizTypeEnum.DATA_DICTIONARY.getCode(),
                fileName
        );
        if (taskResponse.getCode() == null || !taskResponse.getCode().equals("200") || taskResponse.getData() == null) {
            throw new RuntimeException("Failed to create export task");
        }
        return taskResponse.getData();
    }

    private void processExportAsync(List<Long> selectedIds,
                                     String code,
                                     String name,
                                     String path,
                                     Integer language,
                                     Integer status,
                                     Long taskId) {
        File tempFile = null;
        try {
            TaskUpdateStatusDTO processingStatus = new TaskUpdateStatusDTO();
            processingStatus.setStatus(TaskStatusEnum.PROCESSING.getCode());
            taskService.updateTaskStatus(taskId, processingStatus);

            List<DictionaryEntity> items;
            if (selectedIds != null && !selectedIds.isEmpty()) {
                items = dictionaryMapper.selectByIds(selectedIds);
            } else {
                items = dictionaryMapper.selectForExport(code, name, path, language, status);
            }

            if (items.size() > MAX_EXPORT_ROWS) {
                items = items.subList(0, MAX_EXPORT_ROWS);
            }

            String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String fileName = "Dictionary_" + timestamp + ".xlsx";

            tempFile = createExportFile(items, fileName);

            ApiResponse<Long> fileIdResponse = lookUpFileService.saveFile(
                    tempFile, fileName, BizTypeEnum.DATA_DICTIONARY.getCode(), UserContextHolder.getUserId());
            Long fileId = null;
            if (fileIdResponse.getCode() != null && fileIdResponse.getCode().equals("200")) {
                fileId = fileIdResponse.getData();
            }

            TaskUpdateStatusDTO completedStatus = new TaskUpdateStatusDTO();
            completedStatus.setStatus(TaskStatusEnum.COMPLETED.getCode());
            completedStatus.setResult(tempFile.getAbsolutePath());
            completedStatus.setFileId(fileId);
            taskService.updateTaskStatus(taskId, completedStatus);

            log.info("Async export completed, taskId={}, filePath={}, count={}", taskId, tempFile.getAbsolutePath(), items.size());

        } catch (Exception e) {
            log.error("Async export failed, taskId={}", taskId, e);
            TaskUpdateStatusDTO failedStatus = new TaskUpdateStatusDTO();
            failedStatus.setStatus(TaskStatusEnum.FAILED.getCode());
            failedStatus.setResult("Export failed: " + e.getMessage());
            taskService.updateTaskStatus(taskId, failedStatus);
        }
    }

    private File createExportFile(List<DictionaryEntity> items, String fileName) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        File tempFile = new File(tempDir, fileName);

        Workbook workbook = new XSSFWorkbook();

        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        headerStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Sheet sheet = workbook.createSheet("数据字典");
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < EXCEL_HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(EXCEL_HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        for (int i = 0; i < items.size(); i++) {
            DictionaryEntity item = items.get(i);
            Row row = sheet.createRow(i + 1);

            row.createCell(0).setCellValue(item.getCode() != null ? item.getCode() : "");
            row.createCell(1).setCellValue(item.getName() != null ? item.getName() : "");
            row.createCell(2).setCellValue(item.getValue() != null ? item.getValue() : "");
            row.createCell(3).setCellValue(item.getPath() != null ? item.getPath() : "");
            row.createCell(4).setCellValue(item.getDescription() != null ? item.getDescription() : "");
            row.createCell(5).setCellValue(item.getLanguage() != null && item.getLanguage() == 1 ? "中文" : "英文");
            row.createCell(6).setCellValue(item.getStatus() != null && item.getStatus() == 1 ? "有效" : "失效");
        }

        for (int i = 0; i < EXCEL_HEADERS.length; i++) {
            sheet.setColumnWidth(i, 20 * 256);
        }

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            workbook.write(fos);
        }

        workbook.close();

        return tempFile;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cell.getDateCellValue());
                } else {
                    return String.valueOf((int) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    private Integer parseLanguage(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 1;
        }
        if (value.contains("中文") || value.equals("1")) {
            return 1;
        }
        if (value.contains("英文") || value.equals("2")) {
            return 2;
        }
        return 1;
    }

    private Integer parseStatus(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 1;
        }
        if (value.contains("有效") || value.equals("1")) {
            return 1;
        }
        if (value.contains("失效") || value.equals("0")) {
            return 0;
        }
        return 1;
    }

    private DictionaryListVO convertToListVO(DictionaryEntity entity) {
        DictionaryListVO vo = new DictionaryListVO();
        vo.setId(entity.getId());
        vo.setCode(entity.getCode());
        vo.setName(entity.getName());
        vo.setValue(entity.getValue());
        vo.setDescription(entity.getDescription());
        vo.setPath(entity.getPath());
        vo.setLanguage(entity.getLanguage());
        vo.setLanguageName(getLanguageName(entity.getLanguage()));
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
        vo.setId(entity.getId());
        vo.setCode(entity.getCode());
        vo.setName(entity.getName());
        vo.setValue(entity.getValue());
        vo.setDescription(entity.getDescription());
        vo.setPath(entity.getPath());
        vo.setLanguage(entity.getLanguage());
        vo.setLanguageName(getLanguageName(entity.getLanguage()));
        vo.setStatus(entity.getStatus());
        vo.setStatusName(StatusEnum.getNameByCode(entity.getStatus()));
        vo.setCreateBy(entity.getCreateBy());
        vo.setCreateTime(entity.getCreateTime());
        vo.setLastUpdateBy(entity.getLastUpdateBy());
        vo.setLastUpdateTime(entity.getLastUpdateTime());
        return vo;
    }

    private String getLanguageName(Integer language) {
        if (language == null) {
            return "";
        }
        return language == 1 ? "中文" : "英文";
    }
}
