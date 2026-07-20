package com.xxx.it.works.wecode.v2.modules.file.service;

import com.xxx.it.works.wecode.v2.modules.file.dto.UploadResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * 通用文件上传服务接口
 *
 * <p>提供文件上传和展示地址获取能力。存储模式（storage-mode）通过配置切换：
 * <ul>
 *   <li>{@code standard}（默认）：标准环境，文件上传与地址拼接走 OSS/CDN</li>
 *   <li>{@code dev}：开发环境，文件存本地临时目录，showUrl 通过本地静态资源映射拼接</li>
 * </ul>
 * </p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
public interface CommonFileService {

    /**
     * 上传文件
     *
     * @param file   上传的文件（MultipartFile）
     * @param bizType 业务类型：1=能力图标，2=能力示意图
     * @return 上传结果，包含文件批次ID（batchId）和展示地址（showUrl）
     * @throws com.xxx.it.works.wecode.v2.common.exception.BusinessException
     *         当文件格式/尺寸/大小不符合 bizType 校验规则时抛出
     */
    UploadResult upload(MultipartFile file, Integer bizType);

    /**
     * 根据文件批次 ID 获取展示地址
     *
     * @param batchId 文件批次 ID
     * @return 文件展示地址（showUrl）
     */
    String getShowUrl(String batchId);

}
