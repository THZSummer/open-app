package com.xxx.it.works.wecode.v2.modules.file.validator;

import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class DiagramFileValidator implements FileValidator {

    @Override
    public int supportedBizType() {
        return 2;
    }

    @Override
    public void validate(MultipartFile file) {
        String extension = ImageValidationUtils.getFileExtension(file.getOriginalFilename());
        long fileSize = file.getSize();

        // 校验格式
        if (!"png".equalsIgnoreCase(extension) && !"jpg".equalsIgnoreCase(extension)
                && !"jpeg".equalsIgnoreCase(extension)) {
            throw BusinessException.badRequest(
                    "示意图仅支持 PNG/JPG 格式，当前文件格式：" + extension,
                    "Diagram only supports PNG/JPG format, current: " + extension);
        }

        // 校验文件大小（≤500KB）
        long MAX_DIAGRAM_SIZE = 500 * 1024;
        if (fileSize > MAX_DIAGRAM_SIZE) {
            throw BusinessException.badRequest(
                    "示意图文件大小不能超过 500KB，当前：" + (fileSize / 1024) + "KB",
                    "Diagram file size must not exceed 500KB, current: " + (fileSize / 1024) + "KB");
        }

        // 校验图片尺寸
        ImageValidationUtils.validateImageDimensions(file, 520, 288, "示意图", "520×288PX");
    }
}
