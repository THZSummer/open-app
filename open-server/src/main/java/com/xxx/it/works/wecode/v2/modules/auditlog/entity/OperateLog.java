package com.xxx.it.works.wecode.v2.modules.auditlog.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 操作记录实体
 *
 * <p>对应表 openplatform_operate_log_t</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class OperateLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 应用ID */
    private String appId;

    /** 操作类型 */
    private String operateType;

    /** 操作对象 */
    private String operateObject;

    /** 中文描述 */
    private String operateDescCn;

    /** 英文描述 */
    private String operateDescEn;

    /** 操作人 */
    private String operateUser;

    /** 操作人地址 */
    private String ipAddress;

    /** 操作前数据（JSON） */
    private String beforeData;

    /** 操作后数据（JSON） */
    private String afterData;

    /** 操作状态：0-失败；1-成功 */
    private Integer status;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private Date createTime;

    /** 最后更新人 */
    private String lastUpdateBy;

    /** 最后更新时间 */
    private Date lastUpdateTime;
}
