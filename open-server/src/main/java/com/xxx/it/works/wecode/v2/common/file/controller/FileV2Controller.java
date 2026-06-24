package com.xxx.it.works.wecode.v2.common.file.controller;

import com.xxx.it.works.wecode.v2.common.file.service.FileV2Service;
import com.xxx.it.works.wecode.v2.common.file.vo.FileV2VO;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片上传公共控制器
 *
 * <p>提供通用图片上传接口，不依赖具体业务模块</p>
 *
 * @author SDDU Build Agent
 * @version 1.1.0
 */
@RestController
@RequestMapping("/service/open/v2/file")
public class FileV2Controller {

    @Autowired
    private FileV2Service fileV2Service;

    /**
     * 上传图片
     *
     * <p>接收 multipart/form-data，保存到本地磁盘，入库，返回 fileId + url</p>
     *
     * @param bizType 业务类型
     * @param file    上传的图片文件
     * @return FileV2VO 含 fileId 和 url
     */
    @PostMapping("/upload-image")
    public ApiResponse<FileV2VO> uploadImage(
            @RequestParam Integer bizType,
            @RequestParam("file") MultipartFile file) {
        FileV2VO vo = fileV2Service.saveFile(bizType, file);
        return ApiResponse.success(vo);
    }
}
