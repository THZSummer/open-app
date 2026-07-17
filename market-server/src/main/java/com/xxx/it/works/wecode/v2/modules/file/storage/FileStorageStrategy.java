package com.xxx.it.works.wecode.v2.modules.file.storage;

import com.xxx.it.works.wecode.v2.modules.file.dto.UploadResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储策略接口。
 * 开发环境和标准环境各自实现，通过 Spring @ConditionalOnProperty 切换。
 */
public interface FileStorageStrategy {
    UploadResult store(MultipartFile file, Integer bizType, String batchId, String extension);
    String getShowUrl(String batchId);
}
