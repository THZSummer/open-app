package com.xxx.it.works.wecode.v2.modules.version.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 版本 VO
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Data
public class VersionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 版本ID
     */
    private String id;

    /**
     * 版本号
     */
    private String versionCode;

    /**
     * 中文描述
     */
    private String versionDescCn;

    /**
     * 英文描述
     */
    private String versionDescEn;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 审批通过时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvedTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
