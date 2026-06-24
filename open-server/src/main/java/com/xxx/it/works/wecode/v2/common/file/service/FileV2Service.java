package com.xxx.it.works.wecode.v2.common.file.service;

import com.xxx.it.works.wecode.v2.common.constants.CommonConstants;
import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.enums.ResponseCodeEnum;
import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.common.file.entity.FileEntity;
import com.xxx.it.works.wecode.v2.common.file.mapper.FileMapper;
import com.xxx.it.works.wecode.v2.common.file.vo.FileV2VO;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.common.enums.StatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Random;

/**
 * 文件公共服务
 *
 * <p>统一处理文件上传、文件ID→URL的转换逻辑</p>
 *
 * @author SDDU Build Agent
 * @version 1.1.0
 */
@Slf4j
@Service
public class FileV2Service {

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private IdGeneratorStrategy idGenerator;

    @Value("${platform.file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${platform.file.url-prefix:/uploads}")
    private String urlPrefix;

    /**
     * 根据文件ID查询文件 URL
     *
     * @param fileId 文件ID
     * @return 文件 URL，fileId 为空或查不到时返回空字符串
     */
    public String queryFileUrl(String fileId) {
        if (!StringUtils.hasText(fileId)) {
            return "";
        }
        FileEntity file = fileMapper.selectByFileId(fileId);
        return Objects.nonNull(file) ? file.getUrl() : "";
    }

    /**
     * 根据文件ID构建 FileV2VO（含 fileId + url）
     *
     * @param fileId 文件ID
     * @return FileV2VO，fileId 为入参值，url 从文件表查询（查不到则为空字符串）
     */
    public FileV2VO buildFileVO(String fileId) {
        FileV2VO vo = new FileV2VO();
        vo.setFileId(fileId);
        vo.setUrl(queryFileUrl(fileId));
        return vo;
    }

    /**
     * 上传文件到本地磁盘并入库
     *
     * <p>按日期分目录存储，生成唯一 fileId，返回 fileId + url</p>
     *
     * @param bizType 业务类型
     * @param file    上传的文件
     * @return FileV2VO 含 fileId 和 url
     */
    public FileV2VO saveFile(Integer bizType, MultipartFile file) {
        if (Objects.isNull(file) || file.isEmpty()) {
            throw new BusinessException(
                    ResponseCodeEnum.FILE_EMPTY.getCode(),
                    ResponseCodeEnum.FILE_EMPTY.getMessageZh(),
                    ResponseCodeEnum.FILE_EMPTY.getMessageEn()
            );
        }

        // 1. 按日期分目录：uploads/2026/06/07/
        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path dirPath = Paths.get(uploadDir, dateDir);
        try {
            Files.createDirectories(dirPath);
        } catch (IOException e) {
            log.error("Failed to create upload directory: {}", dirPath, e);
            throw new BusinessException(
                    ResponseCodeEnum.FILE_DIR_CREATE_FAILED.getCode(),
                    ResponseCodeEnum.FILE_DIR_CREATE_FAILED.getMessageZh(),
                    ResponseCodeEnum.FILE_DIR_CREATE_FAILED.getMessageEn()
            );
        }

        // 2. 生成 fileId 和文件名
        String fileId = CommonConstants.FILE_ID_PREFIX + System.currentTimeMillis() + "_" + new Random().nextInt(10000);
        String originalFileName = file.getOriginalFilename();
        String ext = "";
        if (StringUtils.hasText(originalFileName) && originalFileName.contains(".")) {
            ext = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String savedFileName = fileId + ext;

        // 3. 保存文件到磁盘
        // NOTE: 必须用绝对路径。MultipartFile.transferTo(File) 对相对路径会按
        // Tomcat 临时目录解析（而非工作目录），导致与 createDirectories 的根目录不一致，
        // 抛 FileNotFoundException(FILE_SAVE_FAILED)。转绝对路径后两者一致。
        Path filePath = dirPath.resolve(savedFileName).toAbsolutePath().normalize();
        try {
            file.transferTo(filePath.toFile());
        } catch (IOException e) {
            log.error("Failed to save file: {}", filePath, e);
            throw new BusinessException(
                    ResponseCodeEnum.FILE_SAVE_FAILED.getCode(),
                    ResponseCodeEnum.FILE_SAVE_FAILED.getMessageZh(),
                    ResponseCodeEnum.FILE_SAVE_FAILED.getMessageEn()
            );
        }

        // 4. 构建 URL: /uploads/2026/06/07/file_xxx.png
        String relativePath = dateDir + "/" + savedFileName;
        String url = urlPrefix + "/" + relativePath;

        // 5. 入库
        FileEntity fileEntity = new FileEntity();
        fileEntity.setId(idGenerator.nextId());
        fileEntity.setFileId(fileId);
        fileEntity.setFileName(Objects.nonNull(originalFileName) ? originalFileName : savedFileName);
        fileEntity.setFilePath(relativePath);
        fileEntity.setUrl(url);
        fileEntity.setBizType(bizType);
        fileEntity.setFileSize(file.getSize());
        fileEntity.setContentType(file.getContentType());
        fileEntity.setTenantId(CommonConstants.DEFAULT_TENANT_ID);
        fileEntity.setStatus(StatusEnum.ENABLED.getCode());
        fileEntity.setCreateBy(UserContextHolder.getUserId());
        fileEntity.setCreateTime(LocalDateTime.now());
        fileEntity.setLastUpdateBy(UserContextHolder.getUserId());
        fileEntity.setLastUpdateTime(LocalDateTime.now());
        fileMapper.insert(fileEntity);

        log.info("File uploaded successfully: fileId={}, url={}, bizType={}", fileId, url, bizType);
        return new FileV2VO(fileId, url);
    }
}
