package com.xxx.it.works.wecode.v2.modules.commonfile.validator;

import org.springframework.web.multipart.MultipartFile;

public interface FileValidator {
    int supportedBizType();
    void validate(MultipartFile file);
}
