package com.xxx.it.works.wecode.v2.modules.file.validator;

import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class IconFileValidator implements FileValidator {

    @Override
    public int supportedBizType() {
        return 1;
    }

    @Override
    public void validate(MultipartFile file) {
        String extension = ImageValidationUtils.getFileExtension(file.getOriginalFilename());
        long fileSize = file.getSize();

        // 校验格式
        if (!"png".equalsIgnoreCase(extension) && !"svg".equalsIgnoreCase(extension)) {
            throw BusinessException.badRequest(
                    "图标仅支持 PNG/SVG 格式，当前文件格式：" + extension,
                    "Icon only supports PNG/SVG format, current: " + extension);
        }

        // 校验文件大小（≤200KB）
        long MAX_ICON_SIZE = 200 * 1024;
        if (fileSize > MAX_ICON_SIZE) {
            throw BusinessException.badRequest(
                    "图标文件大小不能超过 200KB，当前：" + (fileSize / 1024) + "KB",
                    "Icon file size must not exceed 200KB, current: " + (fileSize / 1024) + "KB");
        }

        // 校验图片尺寸（仅对 PNG 进行像素校验，SVG 不校验像素尺寸）
        if (!"svg".equalsIgnoreCase(extension)) {
            ImageValidationUtils.validateImageDimensions(file, 40, 40, "图标", "40×40PX");
        }
    }
}
