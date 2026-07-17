package com.xxx.it.works.wecode.v2.modules.file.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.common.security.AuthRole;
import com.xxx.it.works.wecode.v2.modules.file.dto.UploadResult;
import com.xxx.it.works.wecode.v2.modules.file.service.CommonFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/service/open/v2/file")
@Tag(name = "通用文件管理", description = "通用文件上传接口")
public class CommonFileController {

    @Autowired
    private CommonFileService commonFileService;

    @PostMapping("/upload")
    @AuthRole
    @Operation(summary = "通用文件上传", description = "上传能力图标或示意图文件，返回 batchId 和 showUrl")
    public ApiResponse<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("bizType") Integer bizType) {
        if (file == null || file.isEmpty()) {
            return ApiResponse.error("400", "上传文件不能为空", "Upload file must not be empty");
        }
        if (bizType == null) {
            return ApiResponse.error("400", "业务类型不能为空", "BizType must not be null");
        }
        log.info("File upload request: bizType={}, fileName={}", bizType, file.getOriginalFilename());

        UploadResult result = commonFileService.upload(file, bizType);

        Map<String, String> data = new HashMap<>();
        data.put("batchId", result.getBatchId());
        data.put("showUrl", result.getShowUrl());

        log.info("File upload success: batchId={}, showUrl={}", result.getBatchId(), result.getShowUrl());
        return ApiResponse.success(data);
    }
}
