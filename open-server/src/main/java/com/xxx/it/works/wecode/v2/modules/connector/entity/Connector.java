package com.xxx.it.works.wecode.v2.modules.connector.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 连接器基本信息实体
 * <p>
 * 对应表 openplatform_v2_cp_connector_t
 * 连接器 = 纯出站端点定义, 类比 import 的模块/库
 * V3 新增: app_id 应用归属; status 启用 4 状态流转
 * </p>
 */
@Data
public class Connector implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 雪花ID (应用层生成) */
    private Long id;

    /** 中文名称 */
    private String nameCn;

    /** 英文名称 */
    private String nameEn;

    /** 中文描述 */
    private String descriptionCn;

    /** 英文描述 */
    private String descriptionEn;

    /** 图标文件ID */
    private String iconFileId;

    /** 连接器类型: 1=HTTP */
    private Integer connectorType;

    /** 状态: 1=有效不可用, 2=有效可用, 3=已失效, 4=物理删除（V3 启用） */
    private Integer status;

    /** 归属应用ID（V3 新增，实现 G13 应用数据隔离） */
    private Long appId;

    /** 创建时间 */
    private Date createTime;

    /** 最后更新时间 */
    private Date lastUpdateTime;

    /** 创建人 */
    private String createBy;

    /** 最后更新人 */
    private String lastUpdateBy;
}