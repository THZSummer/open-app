package com.xxx.it.works.wecode.v2.modules.file.storage;

import com.xxx.it.works.wecode.v2.common.annotation.StandardTodo;
import com.xxx.it.works.wecode.v2.modules.file.dto.UploadResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@ConditionalOnProperty(name = "ability.file.storage-mode", havingValue = "standard", matchIfMissing = true)
@StandardTodo("标准环境需对接 OSS/CDN：实现 store() 上传到 OSS，getShowUrl() 返回 CDN 地址")
public class StandardFileStorageStrategy implements FileStorageStrategy {

    @Override
    @StandardTodo("对接 OSS/CDN 上传，替换占位实现")
    public UploadResult store(MultipartFile file, Integer bizType, String batchId, String extension) {
        String showUrl = "/ability-files/" + batchId + "/" + extension;
        log.info("Standard mode upload (placeholder): batchId={}, showUrl={}", batchId, showUrl);
        return new UploadResult(batchId, showUrl);
    }

    @Override
    @StandardTodo("对接 CDN 返回展示地址，替换占位实现")
    public String getShowUrl(String batchId) {
        return "/ability-files/" + batchId + ".png";
    }
}
