package com.xxx.it.works.wecode.v2.modules.file.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 上传结果 DTO
 *
 * <p>包含文件批次 ID（batchId）和展示地址（showUrl）。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@AllArgsConstructor
public class UploadResult {

    /**
     * 文件批次 ID（用于后续创建/编辑接口提交）
     */
    private String batchId;

    /**
     * 文件展示地址
     */
    private String showUrl;
}
