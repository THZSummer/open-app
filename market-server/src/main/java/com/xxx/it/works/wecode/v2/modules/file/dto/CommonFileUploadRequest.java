package com.xxx.it.works.wecode.v2.modules.file.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 通用文件上传请求 DTO
 *
 * <p>用于接收 multipart/form-data 格式的文件上传请求，
 * 包含上传文件对象和业务类型标识。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class CommonFileUploadRequest {

    /**
     * 上传的文件
     */
    @NotNull(message = "上传文件不能为空")
    private MultipartFile file;

    /**
     * 业务类型
     * <ul>
     *   <li>1 = 能力图标</li>
     *   <li>2 = 能力示意图</li>
     * </ul>
     */
    @NotNull(message = "业务类型不能为空")
    private Integer bizType;
}
