package com.xxx.it.works.wecode.v2.modules.commonfile.validator;

import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

@Slf4j
public final class ImageValidationUtils {

    private ImageValidationUtils() {
    }

    public static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot >= filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1).toLowerCase(Locale.ROOT);
    }

    public static void validateImageDimensions(MultipartFile file,
                                                int expectedWidth, int expectedHeight,
                                                String fileType, String expectedSize) {
        try (InputStream is = file.getInputStream()) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                throw BusinessException.badRequest(
                        "无法读取" + fileType + "图片信息，请确认文件格式正确",
                        "Cannot read " + fileType + " image info, please verify file format");
            }
            int actualWidth = image.getWidth();
            int actualHeight = image.getHeight();
            if (actualWidth != expectedWidth || actualHeight != expectedHeight) {
                throw BusinessException.badRequest(
                        fileType + "尺寸必须为 " + expectedSize + "，当前：" + actualWidth + "×" + actualHeight + "PX",
                        fileType + " dimensions must be " + expectedSize
                                + ", current: " + actualWidth + "×" + actualHeight + "PX");
            }
        } catch (IOException e) {
            log.error("Failed to read image dimensions", e);
            throw BusinessException.badRequest(
                    "读取" + fileType + "图片信息失败",
                    "Failed to read " + fileType + " image info");
        }
    }
}
