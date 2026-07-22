package com.xxx.it.works.wecode.v2.modules.commonfile.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CommonFileUploadRequest {

    @NotNull(message = "上传文件不能为空")
    private MultipartFile file;

    @NotNull(message = "业务类型不能为空")
    private Integer bizType;
}
