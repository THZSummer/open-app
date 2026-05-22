package com.xxx.it.works.wecode.v2.modules.lookup.service;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.file.LookUpFileVO;

import java.io.File;

/**
 * 文件服务接口
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
public interface LookUpFileService {

    /**
     * 保存文件并返回fileId
     *
     * @param file     文件
     * @param fileName 文件名称
     * @param bizType  业务类型
     * @param createBy 创建人
     * @return fileId
     */
    ApiResponse<Long> saveFile(File file, String fileName, Integer bizType, String createBy);

    /**
     * 根据fileId获取文件信息
     *
     * @param fileId 文件ID
     * @return 文件信息
     */
    ApiResponse<LookUpFileVO> getFileById(Long fileId);

    /**
     * 根据fileId获取文件路径
     *
     * @param fileId 文件ID
     * @return 文件路径
     */
    String getFilePath(Long fileId);
}