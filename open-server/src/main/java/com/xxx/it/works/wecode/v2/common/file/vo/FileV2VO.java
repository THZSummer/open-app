package com.xxx.it.works.wecode.v2.common.file.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 文件 VO（V2）
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileV2VO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文件ID
     */
    private String fileId;
    /**
     * 文件访问URL
     */
    private String url;
}
