package com.xxx.it.works.wecode.v2.modules.commonfile;

import com.xxx.it.works.wecode.v2.modules.commonfile.dto.UploadResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Component
@ConditionalOnProperty(name = "ability.file.storage-mode", havingValue = "dev")
public class DevFileStorageStrategy implements FileStorageStrategy {

    @Value("${ability.file.local-dir:${java.io.tmpdir}/ability-upload}")
    private String localDir;

    @Value("${ability.file.local-url-prefix:/ability-files/}")
    private String localUrlPrefix;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Override
    public UploadResult store(MultipartFile file, Integer bizType, String batchId, String extension) {
        try {
            Path uploadDir = Paths.get(localDir);
            Files.createDirectories(uploadDir);

            String originalFilename = file.getOriginalFilename();
            String storedFileName = batchId + "." + extension;
            Path targetPath = uploadDir.resolve(storedFileName);

            try (InputStream is = file.getInputStream()) {
                Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("File saved to local path: {}", targetPath);

            saveFileRecord(batchId, originalFilename, targetPath.toString(),
                    bizType, file.getSize(), file.getContentType());

            String showUrl = localUrlPrefix + storedFileName;
            log.info("Dev mode upload success: batchId={}, showUrl={}", batchId, showUrl);

            return new UploadResult(batchId, showUrl);

        } catch (IOException e) {
            log.error("Failed to save file in dev mode", e);
            throw com.xxx.it.works.wecode.v2.common.exception.BusinessException.internalError(
                    "文件保存失败，请稍后重试",
                    "File save failed, please retry later");
        }
    }

    @Override
    public String getShowUrl(String batchId) {
        if (jdbcTemplate != null) {
            try {
                java.util.List<String> filePaths = jdbcTemplate.queryForList(
                        "SELECT file_path FROM openplatform_common_file_t WHERE batch_id = ?",
                        String.class, batchId);
                if (!filePaths.isEmpty()) {
                    String filePath = filePaths.get(0);
                    String fileName = Paths.get(filePath).getFileName().toString();
                    return localUrlPrefix + fileName;
                }
            } catch (Exception e) {
                log.warn("Failed to query file path for batchId={}: {}", batchId, e.getMessage());
            }
        }
        Path localPath = Paths.get(localDir);
        if (Files.exists(localPath)) {
            try (java.nio.file.DirectoryStream<Path> ds = Files.newDirectoryStream(localPath, batchId + ".*")) {
                for (Path p : ds) {
                    return localUrlPrefix + p.getFileName().toString();
                }
            } catch (IOException ignored) {
                log.warn("Failed to scan directory for batchId={}: {}", batchId, ignored.getMessage());
            }
        }

        log.warn("getShowUrl: file not found for batchId={}", batchId);
        return localUrlPrefix + batchId + ".png";
    }

    private void saveFileRecord(String batchId, String originalFilename,
                                 String filePath, Integer bizType,
                                 long fileSize, String contentType) {
        if (jdbcTemplate == null) {
            log.warn("JdbcTemplate not available, skipping DB record for batchId={}", batchId);
            return;
        }
        try {
            jdbcTemplate.update(
                    "INSERT INTO openplatform_common_file_t " +
                    "(batch_id, file_name, file_path, biz_type, file_size, content_type, create_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, NOW())",
                    batchId, originalFilename, filePath, bizType, fileSize, contentType);
            log.debug("File record saved to openplatform_common_file_t, batchId={}", batchId);
        } catch (Exception e) {
            log.warn("Failed to save file record to DB (non-fatal): {}", e.getMessage());
        }
    }
}
