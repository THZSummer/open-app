package com.xxx.it.works.wecode.v2.modules.file.storage;

import com.xxx.it.works.wecode.v2.modules.file.dto.UploadResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "common.file.storage-mode", havingValue = "standard", matchIfMissing = true)
public class StandardFileStorageStrategy implements FileStorageStrategy {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Override
    public UploadResult store(MultipartFile file, Integer bizType, String batchId, String extension) {
        String showUrl = "/common/files/" + batchId + "." + extension;
        log.info("Standard mode upload (placeholder): batchId={}, showUrl={}", batchId, showUrl);
        return new UploadResult(batchId, showUrl);
    }

    @Override
    public String getShowUrl(String batchId) {
        if (jdbcTemplate != null) {
            try {
                List<String> filePaths = jdbcTemplate.queryForList(
                        "SELECT file_path FROM openplatform_common_file_t WHERE batch_id = ?",
                        String.class, batchId);
                if (!filePaths.isEmpty()) {
                    String fileName = Paths.get(filePaths.get(0)).getFileName().toString();
                    return "/common/files/" + fileName;
                }
            } catch (Exception e) {
                log.warn("Failed to query file path for batchId={}: {}", batchId, e.getMessage());
            }
        }

        Path localPath = Paths.get(System.getProperty("java.io.tmpdir"), "ability-upload");
        if (Files.exists(localPath)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(localPath, batchId + ".*")) {
                for (Path p : ds) {
                    return "/common/files/" + p.getFileName().toString();
                }
            } catch (IOException ignored) {
            }
        }

        log.warn("getShowUrl: file not found for batchId={}", batchId);
        return "/common/files/" + batchId + ".png";
    }
}
