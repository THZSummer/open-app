package com.xxx.it.works.wecode.v2.modules.commonfile.service.impl;

import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.modules.commonfile.dto.UploadResult;
import com.xxx.it.works.wecode.v2.modules.commonfile.service.CommonFileService;
import com.xxx.it.works.wecode.v2.modules.commonfile.validator.FileValidator;
import com.xxx.it.works.wecode.v2.modules.commonfile.validator.ImageValidationUtils;
import com.xxx.it.works.wecode.v2.modules.commonfile.storage.FileStorageStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class CommonFileServiceImpl implements CommonFileService {

    @Autowired
    private List<FileValidator> validators;

    @Autowired
    private FileStorageStrategy storageStrategy;

    @Override
    public UploadResult upload(MultipartFile file, Integer bizType) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("上传文件不能为空", "Upload file must not be empty");
        }
        if (bizType == null) {
            throw BusinessException.badRequest("业务类型不能为空", "BizType must not be null");
        }

        String batchId = UUID.randomUUID().toString().replace("-", "");

        FileValidator validator = validators.stream()
                .filter(v -> v.supportedBizType() == bizType)
                .findFirst()
                .orElseThrow(() -> BusinessException.badRequest(
                        "不支持的业务类型：" + bizType, "Unsupported bizType: " + bizType));
        validator.validate(file);

        String extension = ImageValidationUtils.getFileExtension(file.getOriginalFilename());
        return storageStrategy.store(file, bizType, batchId, extension);
    }

    @Override
    public String getShowUrl(String batchId) {
        return storageStrategy.getShowUrl(batchId);
    }
}
