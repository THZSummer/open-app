package com.xxx.it.works.wecode.v2.modules.lookup.service.impl;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.lookup.entity.LookUpFileEntity;
import com.xxx.it.works.wecode.v2.modules.lookup.mapper.LookUpFileMapper;
import com.xxx.it.works.wecode.v2.modules.lookup.service.LookUpFileService;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.file.LookUpFileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.Date;

/**
 * 文件服务实现
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LookUpFileServiceImpl implements LookUpFileService {

    private final LookUpFileMapper lookUpFileMapper;

    @Override
    @Transactional
    public ApiResponse<Long> saveFile(File file, String fileName, Integer bizType, String createBy) {
        try {
            LookUpFileEntity fileEntity = new LookUpFileEntity();
            fileEntity.setFileName(fileName);
            fileEntity.setFilePath(file.getAbsolutePath());
            fileEntity.setFileSize(file.length());
            fileEntity.setFileType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            fileEntity.setBizType(bizType);
            fileEntity.setCreateBy(createBy);
            fileEntity.setCreateTime(new Date());
            fileEntity.setLastUpdateTime(new Date());

            lookUpFileMapper.insert(fileEntity);
            log.info("File saved, fileId={}, fileName={}, filePath={}", 
                    fileEntity.getFileId(), fileName, file.getAbsolutePath());

            return ApiResponse.success(fileEntity.getFileId());
        } catch (Exception e) {
            log.error("Failed to save file, fileName={}", fileName, e);
            return ApiResponse.error("500", "Failed to save file: " + e.getMessage(), null);
        }
    }

    @Override
    public ApiResponse<LookUpFileVO> getFileById(Long fileId) {
        LookUpFileEntity entity = lookUpFileMapper.selectById(fileId);
        if (entity == null) {
            return ApiResponse.error("404", "File not found", null);
        }

        LookUpFileVO vo = new LookUpFileVO();
        vo.setFileId(entity.getFileId());
        vo.setFileName(entity.getFileName());
        vo.setFilePath(entity.getFilePath());
        vo.setFileSize(entity.getFileSize());
        vo.setFileType(entity.getFileType());
        vo.setBizType(entity.getBizType());

        return ApiResponse.success(vo);
    }

    @Override
    public String getFilePath(Long fileId) {
        LookUpFileEntity entity = lookUpFileMapper.selectById(fileId);
        if (entity == null) {
            return null;
        }
        return entity.getFilePath();
    }
}