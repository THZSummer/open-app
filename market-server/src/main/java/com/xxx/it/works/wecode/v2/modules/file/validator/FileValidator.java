package com.xxx.it.works.wecode.v2.modules.file.validator;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件校验器接口 — 每个 bizType 对应一个实现类。
 * 新增业务场景只需新增实现，无需修改 Service。
 */
public interface FileValidator {
    /** 支持的 bizType */
    int supportedBizType();
    /** 校验文件格式/尺寸/大小，不通过抛 BusinessException */
    void validate(MultipartFile file);
}
