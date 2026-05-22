package com.xxx.it.works.wecode.v2.modules.lookup.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 文件实体类
 *
 * <p>对应表 openplatform_lookup_file_t</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class LookUpFileEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文件ID，主键
     */
    private Long fileId;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型（如：application/vnd.openxmlformats-officedocument.spreadsheetml.sheet）
     */
    private String fileType;

    /**
     * 业务类型：1-LookUp，2-数据字典(DATA_DICTIONARY)
     */
    private Integer bizType;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 最后更新时间
     */
    private Date lastUpdateTime;
}