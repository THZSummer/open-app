package com.xxx.it.works.wecode.v2.modules.commonfile.storage;

import com.xxx.it.works.wecode.v2.modules.commonfile.dto.UploadResult;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageStrategy {
    UploadResult store(MultipartFile file, Integer bizType, String batchId, String extension);
    String getShowUrl(String batchId);
}
