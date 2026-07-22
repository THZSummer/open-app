package com.xxx.it.works.wecode.v2.modules.commonfile;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@ConditionalOnProperty(name = "ability.file.storage-mode", havingValue = "standard", matchIfMissing = true)
public class StandardFileStorageStrategy implements FileStorageStrategy {

    @Override
    public UploadResult store(MultipartFile file, Integer bizType, String batchId, String extension) {
        String showUrl = "/ability-files/" + batchId + "/" + extension;
        log.info("Standard mode upload (placeholder): batchId={}, showUrl={}", batchId, showUrl);
        return new UploadResult(batchId, showUrl);
    }

    @Override
    public String getShowUrl(String batchId) {
        return "/ability-files/" + batchId + ".png";
    }
}
