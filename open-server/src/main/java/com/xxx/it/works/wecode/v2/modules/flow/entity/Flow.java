package com.xxx.it.works.wecode.v2.modules.flow.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 连接流基本信息实体
 * <p>
 * 对应表 openplatform_v2_cp_flow_t
 * lifecycle_status: 1=running(运行中), 2=stopped(已停止)
 * MVP 创建后默认 lifecycle_status=1 (running)
 * </p>
 */
@Data
public class Flow implements Serializable {

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

    /** 生命周期状态: 1=running(运行中), 2=stopped(已停止) */
    private Integer lifecycleStatus;

    /** 创建时间 */
    private Date createTime;

    /** 最后更新时间 */
    private Date lastUpdateTime;

    /** 创建人 */
    private String createBy;

    /** 最后更新人 */
    private String lastUpdateBy;
}