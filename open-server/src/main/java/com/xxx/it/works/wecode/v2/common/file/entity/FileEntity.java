package com.xxx.it.works.wecode.v2.common.file.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件实体
 *
 * <p>对应表 openplatform_file_t</p>
 *
 * @author SDDU Build Agent
 * @date 2026-06-07
 */
@Data
public class FileEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;
    /**
     * 文件ID（业务ID）
     */
    private String fileId;
    /**
     * 文件名
     */
    private String fileName;
    /**
     * 文件存储路径
     */
    private String filePath;
    /**
     * 文件访问URL
     */
    private String url;
    /**
     * 业务类型
     */
    private Integer bizType;
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    /**
     * 内容类型（MIME）
     */
    private String contentType;
    /**
     * 租户ID
     */
    private String tenantId;
    /**
     * 状态：0=禁用, 1=启用
     */
    private Integer status;
    /**
     * 创建人
     */
    private String createBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    /**
     * 最后更新人
     */
    private String lastUpdateBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUpdateTime;
}
