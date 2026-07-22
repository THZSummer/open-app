package com.xxx.it.works.wecode.v2.modules.commonfile.service;

import com.xxx.it.works.wecode.v2.modules.commonfile.dto.UploadResult;
import org.springframework.web.multipart.MultipartFile;

public interface CommonFileService {

    UploadResult upload(MultipartFile file, Integer bizType);

    String getShowUrl(String batchId);
}
