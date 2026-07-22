package com.xxx.it.works.wecode.v2.modules.commonfile.storage;

import com.xxx.it.works.wecode.v2.modules.commonfile.dto.UploadResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@ConditionalOnProperty(name = "common.file.storage-mode", havingValue = "standard", matchIfMissing = true)
public class StandardFileStorageStrategy implements FileStorageStrategy {

    @Override
    public UploadResult store(MultipartFile file, Integer bizType, String batchId, String extension) {
        String showUrl = "/common/files/" + batchId + "/" + extension;
        log.info("Standard mode upload (placeholder): batchId={}, showUrl={}", batchId, showUrl);
        return new UploadResult(batchId, showUrl);
    }

    @Override
    public String getShowUrl(String batchId) {
        return "/common/files/" + batchId + ".png";
    }
}
