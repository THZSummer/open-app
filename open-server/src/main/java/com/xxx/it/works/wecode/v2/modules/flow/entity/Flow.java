package com.xxx.it.works.wecode.v2.modules.flow.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 连接流基本信息实体
 * <p>
 * 对应表 openplatform_v2_cp_flow_t
 * V3 扩展: lifecycle_status 4 状态（已停止⇄运行中→已失效→物理删除）
 * V3 新增: deployed_version_id, deployed_version_number, app_id
 * 部署不改变状态，仅切换版本绑定
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

    /** 生命周期状态: 1=已停止, 2=运行中, 3=已失效, 4=物理删除（V3 启用 4 状态） */
    private Integer lifecycleStatus;

    /** 当前部署的版本ID（运行时按此指针读取编排快照，V3 新增） */
    private Long deployedVersionId;

    /** 当前部署的版本号（冗余，避免列表查询 JOIN flow_version_t，V3 新增） */
    private Integer deployedVersionNumber;

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