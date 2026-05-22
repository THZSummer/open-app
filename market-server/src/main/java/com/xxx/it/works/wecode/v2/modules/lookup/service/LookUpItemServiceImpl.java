package com.xxx.it.works.wecode.v2.modules.lookup.service;

import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.item.ItemCreateDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.item.ItemQueryDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.item.ItemUpdateDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.entity.ClassifyEntity;
import com.xxx.it.works.wecode.v2.modules.lookup.entity.LookUpItemEntity;
import com.xxx.it.works.wecode.v2.modules.lookup.dto.task.TaskUpdateStatusDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.enums.BizTypeEnum;
import com.xxx.it.works.wecode.v2.modules.lookup.enums.StatusEnum;
import com.xxx.it.works.wecode.v2.modules.lookup.enums.TaskStatusEnum;
import com.xxx.it.works.wecode.v2.modules.lookup.enums.TaskTypeEnum;
import com.xxx.it.works.wecode.v2.modules.lookup.mapper.ClassifyMapper;
import com.xxx.it.works.wecode.v2.modules.lookup.mapper.LookUpItemMapper;
import com.xxx.it.works.wecode.v2.modules.lookup.service.LookUpFileService;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.common.ImportResultVO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.common.PageVO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.item.ItemDetailVO;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.item.ItemListVO;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * LookUp项服务实现
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LookUpItemServiceImpl implements LookUpItemService {

    private final ClassifyMapper classifyMapper;
    private final LookUpItemMapper lookUpItemMapper;
    private final TaskService taskService;
    private final LookUpFileService lookUpFileService;

    // Sheet1: 分类信息
    private static final String[] CLASSIFY_SHEET_HEADERS = {
            "分类编码", "分类名称", "路径", "分类描述", "状态"
    };

    // Sheet2: 项信息
    private static final String[] ITEM_SHEET_HEADERS = {
            "分类编码", "路径", "项编码", "项名称", "项值", "排序", "项描述",
            "扩展属性1", "扩展属性2", "扩展属性3", "扩展属性4", "扩展属性5", "扩展属性6", "状态"
    };

    // Excel列定义（导入导出用）
    private static final String[] EXCEL_HEADERS = {
            "项编码", "项名称", "项值", "排序", "描述", 
            "扩展属性1", "扩展属性2", "扩展属性3", "扩展属性4", "扩展属性5", "扩展属性6"
    };

    private static final int BATCH_SIZE = 100;
    private static final int MAX_IMPORT_ROWS = 1000;
    private static final int MAX_EXPORT_ROWS = 1000;

    /**
     * 获取LookUp项列表
     *
     * @param classifyId 分类ID
     * @param queryDTO   查询条件DTO
     * @return 分页结果，包含LookUp项列表和分页信息
     */
    @Override
    public ApiResponse<PageVO<ItemListVO>> getItemList(Long classifyId, ItemQueryDTO queryDTO) {
        log.debug("Get item list, classifyId={}, queryDTO={}", classifyId, queryDTO);

        // 校验分类是否存在
        ClassifyEntity classify = classifyMapper.selectById(classifyId);
        if (classify == null) {
            return ApiResponse.error("40401", 
                    "分类不存在: " + classifyId, 
                    "Classify not found: " + classifyId);
        }

        // 计算分页参数
        int offset = (queryDTO.getPageNum() - 1) * queryDTO.getPageSize();
        int limit = queryDTO.getPageSize();

        // 查询列表
        List<LookUpItemEntity> entityList = lookUpItemMapper.selectList(
                classifyId,
                queryDTO.getItemCode(),
                queryDTO.getItemName(),
                queryDTO.getStatus(),
                offset,
                limit
        );

        // 统计总数
        long total = lookUpItemMapper.countList(
                classifyId,
                queryDTO.getItemCode(),
                queryDTO.getItemName(),
                queryDTO.getStatus()
        );

        // 转换为VO
        List<ItemListVO> voList = entityList.stream()
                .map(this::convertToListVO)
                .collect(Collectors.toList());

        // 构建分页结果
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

        // 校验分类是否存在
        ClassifyEntity classify = classifyMapper.selectById(classifyId);
        if (classify == null) {
            return ApiResponse.error("40401", 
                    "分类不存在: " + classifyId, 
                    "Classify not found: " + classifyId);
        }

        // 校验项编码唯一性（同一分类下）
        LookUpItemEntity existingItem = lookUpItemMapper.selectByClassifyIdAndCode(
                classifyId, createDTO.getItemCode());
        if (existingItem != null) {
            return ApiResponse.error("40902", 
                    "项编码已存在: " + createDTO.getItemCode(), 
                    "Item code already exists: " + createDTO.getItemCode());
        }

        // 创建实体
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

        // 插入数据
        lookUpItemMapper.insert(entity);

        log.info("Item created successfully, itemId={}", entity.getItemId());

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

        // 校验项是否存在
        LookUpItemEntity entity = lookUpItemMapper.selectById(itemId);
        if (entity == null) {
            return ApiResponse.error("40402", 
                    "LookUp项不存在: " + itemId, 
                    "LookUp item not found: " + itemId);
        }

        // 更新字段
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

        // 更新数据
        lookUpItemMapper.update(entity);

        log.info("Item updated successfully, itemId={}", itemId);

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

        // 校验项是否存在
        LookUpItemEntity entity = lookUpItemMapper.selectById(itemId);
        if (entity == null) {
            return ApiResponse.error("40402", 
                    "LookUp项不存在: " + itemId, 
                    "LookUp item not found: " + itemId);
        }

        // 校验项状态必须为失效(0)才能删除
        if (entity.getStatus() != null && entity.getStatus() == 1) {
            return ApiResponse.error("40004",
                    "LookUp项状态为有效，无法删除。请先将项设置为失效状态",
                    "LookUp item is effective, cannot delete. Please set status to ineffective first");
        }

        // 删除项
        lookUpItemMapper.deleteById(itemId);

        log.info("Item deleted successfully, itemId={}", itemId);

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

        // 查询项
        LookUpItemEntity entity = lookUpItemMapper.selectById(itemId);
        if (entity == null) {
            return ApiResponse.error("40402", 
                    "LookUp项不存在: " + itemId, 
                    "LookUp item not found: " + itemId);
        }

        // 转换为VO
        ItemDetailVO vo = convertToDetailVO(entity);

        return ApiResponse.success(vo);
    }

    /**
     * 导入LookUp项
     *
     * @param classifyId 分类ID
     * @param file       上传的文件
     * @return 导入结果，包含成功数量、失败数量和失败记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<ImportResultVO> importItems(Long classifyId, MultipartFile file) {
        log.info("Import items, classifyId={}, fileName={}", classifyId, file.getOriginalFilename());

        // 校验分类是否存在
        ClassifyEntity classify = classifyMapper.selectById(classifyId);
        if (classify == null) {
            return ApiResponse.error("40401", 
                    "分类不存在: " + classifyId, 
                    "Classify not found: " + classifyId);
        }

        // 校验文件格式
        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
            return ApiResponse.error("400", 
                    "文件格式不支持，仅支持.xlsx或.xls格式", 
                    "File format not supported, only .xlsx or .xls allowed");
        }

        // 解析Excel
        List<ImportResultVO.ImportFailRecordVO> failList = new ArrayList<>();
        int totalCount = 0;
        int successCount = 0;
        List<LookUpItemEntity> itemsToInsert = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = fileName.endsWith(".xlsx") 
                     ? new XSSFWorkbook(is) 
                     : new HSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();

            // 检查行数限制
            if (lastRowNum > MAX_IMPORT_ROWS) {
                return ApiResponse.error("400", 
                        "导入数据超过限制，单次最多导入" + MAX_IMPORT_ROWS + "条", 
                        "Import data exceeds limit, max " + MAX_IMPORT_ROWS + " rows per batch");
            }

            // 跳过表头，从第2行开始读取
            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                totalCount++;

                // 读取单元格数据
                String itemCode = getCellValue(row.getCell(0));
                String itemName = getCellValue(row.getCell(1));
                String itemValue = getCellValue(row.getCell(2));
                String itemIndexStr = getCellValue(row.getCell(3));
                String itemDesc = getCellValue(row.getCell(4));
                String itemAttr1 = getCellValue(row.getCell(5));
                String itemAttr2 = getCellValue(row.getCell(6));
                String itemAttr3 = getCellValue(row.getCell(7));
                String itemAttr4 = getCellValue(row.getCell(8));
                String itemAttr5 = getCellValue(row.getCell(9));
                String itemAttr6 = getCellValue(row.getCell(10));

                // 数据校验
                if (itemCode == null || itemCode.trim().isEmpty()) {
                    failList.add(new ImportResultVO.ImportFailRecordVO(
                            i + 1, "", "项编码不能为空"));
                    continue;
                }

                // 检查项编码是否已存在
                if (lookUpItemMapper.checkItemCodeExists(classifyId, itemCode) > 0) {
                    failList.add(new ImportResultVO.ImportFailRecordVO(
                            i + 1, itemCode, "项编码已存在，自动跳过"));
                    continue;
                }

                // 创建实体
                LookUpItemEntity entity = new LookUpItemEntity();
                entity.setClassifyId(classifyId);
                entity.setItemCode(itemCode.trim());
                entity.setItemName(itemName != null ? itemName.trim() : "");
                entity.setItemValue(itemValue);
                entity.setItemIndex(parseItemIndex(itemIndexStr));
                entity.setItemDesc(itemDesc);
                entity.setItemAttr1(itemAttr1);
                entity.setItemAttr2(itemAttr2);
                entity.setItemAttr3(itemAttr3);
                entity.setItemAttr4(itemAttr4);
                entity.setItemAttr5(itemAttr5);
                entity.setItemAttr6(itemAttr6);
                entity.setStatus(StatusEnum.ACTIVE.getCode());
                entity.setCreateBy(UserContextHolder.getUserId());
                entity.setCreateTime(new Date());
                entity.setLastUpdateBy(UserContextHolder.getUserId());
                entity.setLastUpdateTime(new Date());

                itemsToInsert.add(entity);
                successCount++;

                // 批量插入
                if (itemsToInsert.size() >= BATCH_SIZE) {
                    lookUpItemMapper.batchInsert(itemsToInsert);
                    itemsToInsert.clear();
                }
            }

            // 插入剩余数据
            if (!itemsToInsert.isEmpty()) {
                lookUpItemMapper.batchInsert(itemsToInsert);
            }

        } catch (IOException e) {
            log.error("Failed to import items", e);
            return ApiResponse.error("500", 
                    "文件解析失败: " + e.getMessage(), 
                    "File parsing failed: " + e.getMessage());
        }

        // 构建返回结果
        ImportResultVO resultVO = ImportResultVO.builder()
                .totalCount(totalCount)
                .successCount(successCount)
                .failCount(failList.size())
                .failList(failList)
                .build();

        log.info("Import completed, total={}, success={}, fail={}", 
                totalCount, successCount, failList.size());

        return ApiResponse.success(resultVO);
    }

    /**
     * 异步导入LookUp项
     *
     * @param classifyId 分类ID
     * @param file       上传的文件
     * @return 任务ID
     */
    @Override
    public ApiResponse<Long> importItemsAsync(Long classifyId, MultipartFile file) {
        log.info("Import items async, classifyId={}, fileName={}", classifyId, file.getOriginalFilename());

        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
            return ApiResponse.error("400",
                    "文件格式不支持，仅支持.xlsx或.xls格式",
                    "File format not supported, only .xlsx or .xls allowed");
        }

        Long taskId = createImportTask(classifyId, file);

        processImportAsync(classifyId, file, taskId);

        return ApiResponse.success(taskId);
    }

    private Long createImportTask(Long classifyId, MultipartFile file) {
        ApiResponse<Long> taskResponse = taskService.createTask(
                TaskTypeEnum.IMPORT.getCode(),
                BizTypeEnum.LOOKUP.getCode(),
                file.getOriginalFilename()
        );
        if (taskResponse.getCode() == null || !taskResponse.getCode().equals("200") || taskResponse.getData() == null) {
            throw new RuntimeException("Failed to create import task");
        }
        return taskResponse.getData();
    }

    private void processImportAsync(Long classifyId, MultipartFile file, Long taskId) {
        File tempFile = null;
        try {
            TaskUpdateStatusDTO processingStatus = new TaskUpdateStatusDTO();
            processingStatus.setStatus(TaskStatusEnum.PROCESSING.getCode());
            taskService.updateTaskStatus(taskId, processingStatus);

            tempFile = saveToTempFile(file);

            doActualImport(classifyId, tempFile, taskId);

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

    private void doActualImport(Long classifyId, File tempFile, Long taskId) {
        try (InputStream is = Files.newInputStream(tempFile.toPath());
             Workbook workbook = tempFile.getName().endsWith(".xlsx")
                     ? new XSSFWorkbook(is)
                     : new HSSFWorkbook(is)) {

            ClassifyEntity classify = classifyMapper.selectById(classifyId);
            if (classify == null) {
                throw new RuntimeException("Classify not found: " + classifyId);
            }

            List<ImportResultVO.ImportFailRecordVO> failList = new ArrayList<>();
            int totalCount = 0;
            int successCount = 0;
            List<LookUpItemEntity> itemsToInsert = new ArrayList<>();

            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();

            if (lastRowNum > MAX_IMPORT_ROWS) {
                throw new RuntimeException("Import data exceeds limit, max " + MAX_IMPORT_ROWS + " rows per batch");
            }

            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                totalCount++;

                String itemCode = getCellValue(row.getCell(0));
                String itemName = getCellValue(row.getCell(1));
                String itemValue = getCellValue(row.getCell(2));
                String itemIndexStr = getCellValue(row.getCell(3));
                String itemDesc = getCellValue(row.getCell(4));
                String itemAttr1 = getCellValue(row.getCell(5));
                String itemAttr2 = getCellValue(row.getCell(6));
                String itemAttr3 = getCellValue(row.getCell(7));
                String itemAttr4 = getCellValue(row.getCell(8));
                String itemAttr5 = getCellValue(row.getCell(9));
                String itemAttr6 = getCellValue(row.getCell(10));

                if (itemCode == null || itemCode.trim().isEmpty()) {
                    failList.add(new ImportResultVO.ImportFailRecordVO(
                            i + 1, "", "Item code cannot be empty"));
                    continue;
                }

                if (lookUpItemMapper.checkItemCodeExists(classifyId, itemCode) > 0) {
                    failList.add(new ImportResultVO.ImportFailRecordVO(
                            i + 1, itemCode, "Item code already exists, skipped"));
                    continue;
                }

                LookUpItemEntity entity = new LookUpItemEntity();
                entity.setClassifyId(classifyId);
                entity.setItemCode(itemCode.trim());
                entity.setItemName(itemName != null ? itemName.trim() : "");
                entity.setItemValue(itemValue);
                entity.setItemIndex(parseItemIndex(itemIndexStr));
                entity.setItemDesc(itemDesc);
                entity.setItemAttr1(itemAttr1);
                entity.setItemAttr2(itemAttr2);
                entity.setItemAttr3(itemAttr3);
                entity.setItemAttr4(itemAttr4);
                entity.setItemAttr5(itemAttr5);
                entity.setItemAttr6(itemAttr6);
                entity.setStatus(StatusEnum.ACTIVE.getCode());
                entity.setCreateBy(UserContextHolder.getUserId());
                entity.setCreateTime(new Date());
                entity.setLastUpdateBy(UserContextHolder.getUserId());
                entity.setLastUpdateTime(new Date());

                itemsToInsert.add(entity);
                successCount++;

                if (itemsToInsert.size() >= BATCH_SIZE) {
                    lookUpItemMapper.batchInsert(itemsToInsert);
                    itemsToInsert.clear();
                }
            }

            if (!itemsToInsert.isEmpty()) {
                lookUpItemMapper.batchInsert(itemsToInsert);
            }

            log.info("Async import completed, taskId={}, total={}, success={}, fail={}",
                    taskId, totalCount, successCount, failList.size());

        } catch (IOException e) {
            throw new RuntimeException("File parsing failed: " + e.getMessage(), e);
        }
    }

    /**
     * 导出LookUp项
     *
     * @param classifyId 分类ID
     * @param status     状态过滤
     * @param itemCode   项编码过滤
     * @param itemName   项名称过滤
     * @param response   HTTP响应对象
     */
    @Override
    public void exportItems(Long classifyId, Integer status, String itemCode, String itemName, 
                           HttpServletResponse response) {
        log.info("Export items, classifyId={}, status={}", classifyId, status);

        // 查询数据
        List<LookUpItemEntity> items = lookUpItemMapper.selectForExport(
                classifyId, status, itemCode, itemName);

        if (items.size() > MAX_EXPORT_ROWS) {
            items = items.subList(0, MAX_EXPORT_ROWS);
        }

        // 获取分类信息（用于文件名）
        String classifyCode = "ALL";
        if (classifyId != null) {
            ClassifyEntity classify = classifyMapper.selectById(classifyId);
            if (classify != null) {
                classifyCode = classify.getClassifyCode();
            }
        }

        // 生成文件名
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String fileName = "LookUp_" + classifyCode + "_" + timestamp + ".xlsx";

        try {
            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", 
                    "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

            // 创建工作簿
            Workbook workbook;
            if (items.size() > 5000) {
                workbook = new SXSSFWorkbook(100); // 使用流式写入
            } else {
                workbook = new XSSFWorkbook();
            }

            Sheet sheet = workbook.createSheet("LookUp Items");

            // 创建表头
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < EXCEL_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(EXCEL_HEADERS[i]);
                // 设置表头样式
                CellStyle headerStyle = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                headerStyle.setFont(font);
                cell.setCellStyle(headerStyle);
            }

            // 填充数据
            for (int i = 0; i < items.size(); i++) {
                LookUpItemEntity item = items.get(i);
                Row row = sheet.createRow(i + 1);
                
                row.createCell(0).setCellValue(item.getItemCode());
                row.createCell(1).setCellValue(item.getItemName());
                row.createCell(2).setCellValue(item.getItemValue());
                row.createCell(3).setCellValue(item.getItemIndex() != null ? item.getItemIndex() : 0);
                row.createCell(4).setCellValue(item.getItemDesc());
                row.createCell(5).setCellValue(item.getItemAttr1());
                row.createCell(6).setCellValue(item.getItemAttr2());
                row.createCell(7).setCellValue(item.getItemAttr3());
                row.createCell(8).setCellValue(item.getItemAttr4());
                row.createCell(9).setCellValue(item.getItemAttr5());
                row.createCell(10).setCellValue(item.getItemAttr6());
            }

            // 设置列宽
            for (int i = 0; i < EXCEL_HEADERS.length; i++) {
                sheet.setColumnWidth(i, 15);
            }

            // 写入响应
            try (ServletOutputStream os = response.getOutputStream()) {
                workbook.write(os);
                os.flush();
            }

            // 关闭工作簿
            workbook.close();

            log.info("Export completed, fileName={}, count={}", fileName, items.size());

        } catch (IOException e) {
            log.error("Failed to export items", e);
            throw new RuntimeException("Export failed: " + e.getMessage(), e);
        }
    }

    /**
     * 异步导出LookUp项
     *
     * @param classifyId 分类ID
     * @param status     状态过滤
     * @param itemCode   项编码过滤
     * @param itemName   项名称过滤
     * @return 任务ID
     */
    @Override
    public ApiResponse<Long> exportItemsAsync(Long classifyId, Integer status, String itemCode, String itemName) {
        log.info("Export items async, classifyId={}, status={}", classifyId, status);

        Long taskId = createExportTask();

        processExportAsync(classifyId, status, itemCode, itemName, taskId);

        return ApiResponse.success(taskId);
    }

    private Long createExportTask() {
        String fileName = "LookUp_Export_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".xlsx";
        ApiResponse<Long> taskResponse = taskService.createTask(
                TaskTypeEnum.EXPORT.getCode(),
                BizTypeEnum.LOOKUP.getCode(),
                fileName
        );
        if (taskResponse.getCode() == null || !taskResponse.getCode().equals("200") || taskResponse.getData() == null) {
            throw new RuntimeException("Failed to create export task");
        }
        return taskResponse.getData();
    }

    private void processExportAsync(Long classifyId, Integer status, String itemCode, String itemName, Long taskId) {
        File tempFile = null;
        try {
            TaskUpdateStatusDTO processingStatus = new TaskUpdateStatusDTO();
            processingStatus.setStatus(TaskStatusEnum.PROCESSING.getCode());
            taskService.updateTaskStatus(taskId, processingStatus);

            List<LookUpItemEntity> items = lookUpItemMapper.selectForExport(
                    classifyId, status, itemCode, itemName);

            String classifyCode = "ALL";
            if (classifyId != null) {
                ClassifyEntity classify = classifyMapper.selectById(classifyId);
                if (classify != null) {
                    classifyCode = classify.getClassifyCode();
                }
            }

            String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String fileName = "LookUp_" + classifyCode + "_" + timestamp + ".xlsx";

            ClassifyEntity classifyForExport = null;
            if (classifyId != null) {
                classifyForExport = classifyMapper.selectById(classifyId);
            }
            tempFile = createExportFile(classifyForExport, items, fileName);

            ApiResponse<Long> fileIdResponse = lookUpFileService.saveFile(
                    tempFile, fileName, BizTypeEnum.LOOKUP.getCode(), UserContextHolder.getUserId());
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

    private static final int MAX_CLASSIFY_EXPORT_ROWS = 1000;
    private static final int MAX_ITEM_EXPORT_ROWS_PER_CLASSIFY = 1000;

    private File createExportFile(ClassifyEntity classify, List<LookUpItemEntity> items, String fileName) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        File tempFile = new File(tempDir, fileName);

        Workbook workbook;
        if (items.size() > 5000) {
            workbook = new SXSSFWorkbook(100);
        } else {
            workbook = new XSSFWorkbook();
        }

        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        headerStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Map<Long, ClassifyEntity> classifyMap = new HashMap<>();
        Map<Long, List<LookUpItemEntity>> itemsByClassify = new HashMap<>();
        
        if (classify != null) {
            classifyMap.put(classify.getClassifyId(), classify);
        }
        
        for (LookUpItemEntity item : items) {
            if (item.getClassifyId() != null) {
                itemsByClassify.computeIfAbsent(item.getClassifyId(), k -> new ArrayList<>()).add(item);
            }
        }

        for (LookUpItemEntity item : items) {
            if (item.getClassifyId() != null && !classifyMap.containsKey(item.getClassifyId())) {
                ClassifyEntity c = classifyMapper.selectById(item.getClassifyId());
                if (c != null) {
                    classifyMap.put(c.getClassifyId(), c);
                }
            }
        }

        List<ClassifyEntity> classifiesToExport = new ArrayList<>(classifyMap.values());
        if (classifiesToExport.size() > MAX_CLASSIFY_EXPORT_ROWS) {
            classifiesToExport = classifiesToExport.subList(0, MAX_CLASSIFY_EXPORT_ROWS);
        }
        final Set<Long> validClassifyIds = new HashSet<>();
        for (ClassifyEntity c : classifiesToExport) {
            validClassifyIds.add(c.getClassifyId());
        }

        // ===== Sheet1: 分类信息 =====
        Sheet classifySheet = workbook.createSheet("分类信息");
        Row classifyHeaderRow = classifySheet.createRow(0);
        for (int i = 0; i < CLASSIFY_SHEET_HEADERS.length; i++) {
            Cell cell = classifyHeaderRow.createCell(i);
            cell.setCellValue(CLASSIFY_SHEET_HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }
        
        int classifyDataRowNum = 1;
        for (ClassifyEntity c : classifiesToExport) {
            Row classifyDataRow = classifySheet.createRow(classifyDataRowNum++);
            classifyDataRow.createCell(0).setCellValue(c.getClassifyCode() != null ? c.getClassifyCode() : "");
            classifyDataRow.createCell(1).setCellValue(c.getClassifyName() != null ? c.getClassifyName() : "");
            classifyDataRow.createCell(2).setCellValue(c.getPath() != null ? c.getPath() : "");
            classifyDataRow.createCell(3).setCellValue(c.getClassifyDesc() != null ? c.getClassifyDesc() : "");
            classifyDataRow.createCell(4).setCellValue(c.getStatus() != null && c.getStatus() == 1 ? "有效" : "失效");
        }
        for (int i = 0; i < CLASSIFY_SHEET_HEADERS.length; i++) {
            classifySheet.setColumnWidth(i, 20 * 256);
        }

        // ===== Sheet2: 项信息 =====
        Sheet itemSheet = workbook.createSheet("项信息");
        Row itemHeaderRow = itemSheet.createRow(0);
        for (int i = 0; i < ITEM_SHEET_HEADERS.length; i++) {
            Cell cell = itemHeaderRow.createCell(i);
            cell.setCellValue(ITEM_SHEET_HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        int itemRowNum = 1;
        for (ClassifyEntity c : classifiesToExport) {
            List<LookUpItemEntity> classifyItems = itemsByClassify.getOrDefault(c.getClassifyId(), new ArrayList<>());
            if (classifyItems.size() > MAX_ITEM_EXPORT_ROWS_PER_CLASSIFY) {
                classifyItems = classifyItems.subList(0, MAX_ITEM_EXPORT_ROWS_PER_CLASSIFY);
            }
            
            for (LookUpItemEntity item : classifyItems) {
                Row row = itemSheet.createRow(itemRowNum++);
                
                row.createCell(0).setCellValue(c.getClassifyCode() != null ? c.getClassifyCode() : "");
                row.createCell(1).setCellValue(c.getPath() != null ? c.getPath() : "");
                row.createCell(2).setCellValue(item.getItemCode() != null ? item.getItemCode() : "");
                row.createCell(3).setCellValue(item.getItemName() != null ? item.getItemName() : "");
                row.createCell(4).setCellValue(item.getItemValue() != null ? item.getItemValue() : "");
                row.createCell(5).setCellValue(item.getItemIndex() != null ? item.getItemIndex() : 0);
                row.createCell(6).setCellValue(item.getItemDesc() != null ? item.getItemDesc() : "");
                row.createCell(7).setCellValue(item.getItemAttr1() != null ? item.getItemAttr1() : "");
                row.createCell(8).setCellValue(item.getItemAttr2() != null ? item.getItemAttr2() : "");
                row.createCell(9).setCellValue(item.getItemAttr3() != null ? item.getItemAttr3() : "");
                row.createCell(10).setCellValue(item.getItemAttr4() != null ? item.getItemAttr4() : "");
                row.createCell(11).setCellValue(item.getItemAttr5() != null ? item.getItemAttr5() : "");
                row.createCell(12).setCellValue(item.getItemAttr6() != null ? item.getItemAttr6() : "");
                row.createCell(13).setCellValue(item.getStatus() != null && item.getStatus() == 1 ? "有效" : "失效");
            }
        }

        for (int i = 0; i < ITEM_SHEET_HEADERS.length; i++) {
            itemSheet.setColumnWidth(i, 18 * 256);
        }

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            workbook.write(fos);
        }

        workbook.close();

        return tempFile;
    }

    /**
     * 下载导入模板
     *
     * @param response HTTP响应对象
     */
    @Override
    public void downloadTemplate(HttpServletResponse response) {
        log.info("Download import template");

        String fileName = "LookUp_Import_Template.xlsx";

        try {
            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", 
                    "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

            // 创建工作簿
            Workbook workbook = new XSSFWorkbook();

            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // ===== Sheet1: 分类信息 =====
            Sheet classifySheet = workbook.createSheet("分类信息");
            Row classifyHeaderRow = classifySheet.createRow(0);
            for (int i = 0; i < CLASSIFY_SHEET_HEADERS.length; i++) {
                Cell cell = classifyHeaderRow.createCell(i);
                cell.setCellValue(CLASSIFY_SHEET_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }
            // 示例数据
            Row classifyExampleRow = classifySheet.createRow(1);
            classifyExampleRow.createCell(0).setCellValue("USER_TYPE");
            classifyExampleRow.createCell(1).setCellValue("用户类型");
            classifyExampleRow.createCell(2).setCellValue("/system");
            classifyExampleRow.createCell(3).setCellValue("用户类型分类");
            classifyExampleRow.createCell(4).setCellValue("有效");
            // 设置列宽
            for (int i = 0; i < CLASSIFY_SHEET_HEADERS.length; i++) {
                classifySheet.setColumnWidth(i, 20 * 256);
            }

            // ===== Sheet2: 项信息 =====
            Sheet itemSheet = workbook.createSheet("项信息");
            Row itemHeaderRow = itemSheet.createRow(0);
            for (int i = 0; i < ITEM_SHEET_HEADERS.length; i++) {
                Cell cell = itemHeaderRow.createCell(i);
                cell.setCellValue(ITEM_SHEET_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }
            // 示例数据
            Row itemExampleRow = itemSheet.createRow(1);
            itemExampleRow.createCell(0).setCellValue("USER_TYPE");
            itemExampleRow.createCell(1).setCellValue("/system");
            itemExampleRow.createCell(2).setCellValue("ADMIN");
            itemExampleRow.createCell(3).setCellValue("管理员");
            itemExampleRow.createCell(4).setCellValue("1");
            itemExampleRow.createCell(5).setCellValue(1);
            itemExampleRow.createCell(6).setCellValue("系统管理员");
            itemExampleRow.createCell(7).setCellValue("");
            itemExampleRow.createCell(8).setCellValue("");
            itemExampleRow.createCell(9).setCellValue("");
            itemExampleRow.createCell(10).setCellValue("");
            itemExampleRow.createCell(11).setCellValue("");
            itemExampleRow.createCell(12).setCellValue("");
            itemExampleRow.createCell(13).setCellValue("有效");
            // 设置列宽
            for (int i = 0; i < ITEM_SHEET_HEADERS.length; i++) {
                itemSheet.setColumnWidth(i, 18 * 256);
            }

            // 写入响应
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

    /**
     * 获取单元格值
     */
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

    /**
     * 解析排序序号
     */
    private Integer parseItemIndex(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 转换为列表VO
     */
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

    /**
     * 转换为详情VO
     */
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
