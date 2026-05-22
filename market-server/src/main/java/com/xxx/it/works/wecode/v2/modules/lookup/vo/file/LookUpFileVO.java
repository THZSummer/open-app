package com.xxx.it.works.wecode.v2.modules.lookup.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 文件信息VO
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Schema(description = "文件信息")
public class LookUpFileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文件ID", example = "1")
    private Long fileId;

    @Schema(description = "文件名称", example = "LookUp_TEST_20240521.xlsx")
    private String fileName;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "文件大小（字节）", example = "1024")
    private Long fileSize;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "业务类型：1-LookUp，2-数据字典", example = "1")
    private Integer bizType;
}