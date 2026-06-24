package com.xxx.it.works.wecode.v2.modules.auditlog.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 操作日志 VO（前端展示用）
 *
 * @author SDDU Build Agent
 * @date 2026-06-07
 */
@Data
public class OperateLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    /** 操作账号 */
    private String account;
    /** 操作类型 */
    private String operationType;
    /** 操作对象 */
    private String operationObject;
    /** 操作描述 */
    private String description;
    /** 操作IP */
    private String ip;
    /** 操作时间 */
    private String time;
    /** 操作结果：成功/失败 */
    private String result;
}
