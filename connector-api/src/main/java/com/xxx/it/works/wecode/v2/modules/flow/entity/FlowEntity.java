package com.xxx.it.works.wecode.v2.modules.flow.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * 连接流基本信息 R2DBC Entity
 * <p>
 * 对应表: openplatform_v2_cp_flow_t
 * lifecycle_status: 1=running(运行中), 2=stopped(已停止)
 * MVP 创建后默认 lifecycle_status=1 (running)
 * </p>
 */
@Table("openplatform_v2_cp_flow_t")
public class FlowEntity {

    @Id
    @Column("id")
    private Long id;

    @Column("name_cn")
    private String nameCn;

    @Column("name_en")
    private String nameEn;

    @Column("description_cn")
    private String descriptionCn;

    @Column("description_en")
    private String descriptionEn;

    @Column("icon_file_id")
    private String iconFileId;

    @Column("lifecycle_status")
    private Integer lifecycleStatus;

    /** V3: 已部署版本ID */
    @Column("deployed_version_id")
    private Long deployedVersionId;

    /** V3: 已部署版本号 */
    @Column("deployed_version_number")
    private Integer deployedVersionNumber;

    /** V3: 所属应用ID */
    @Column("app_id")
    private Long appId;

    @Column("create_time")
    private LocalDateTime createTime;

    @Column("last_update_time")
    private LocalDateTime lastUpdateTime;

    @Column("create_by")
    private String createBy;

    @Column("last_update_by")
    private String lastUpdateBy;

    public FlowEntity() {}

    public FlowEntity(Long id, String nameCn, String nameEn) {
        this.id = id;
        this.nameCn = nameCn;
        this.nameEn = nameEn;
    }

    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNameCn() { return nameCn; }
    public void setNameCn(String nameCn) { this.nameCn = nameCn; }

    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }

    public String getDescriptionCn() { return descriptionCn; }
    public void setDescriptionCn(String descriptionCn) { this.descriptionCn = descriptionCn; }

    public String getDescriptionEn() { return descriptionEn; }
    public void setDescriptionEn(String descriptionEn) { this.descriptionEn = descriptionEn; }

    public String getIconFileId() { return iconFileId; }
    public void setIconFileId(String iconFileId) { this.iconFileId = iconFileId; }

    public Integer getLifecycleStatus() { return lifecycleStatus; }
    public void setLifecycleStatus(Integer lifecycleStatus) { this.lifecycleStatus = lifecycleStatus; }

    public Long getDeployedVersionId() { return deployedVersionId; }
    public void setDeployedVersionId(Long deployedVersionId) { this.deployedVersionId = deployedVersionId; }

    public Integer getDeployedVersionNumber() { return deployedVersionNumber; }
    public void setDeployedVersionNumber(Integer deployedVersionNumber) { this.deployedVersionNumber = deployedVersionNumber; }

    public Long getAppId() { return appId; }
    public void setAppId(Long appId) { this.appId = appId; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getLastUpdateTime() { return lastUpdateTime; }
    public void setLastUpdateTime(LocalDateTime lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }

    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }

    public String getLastUpdateBy() { return lastUpdateBy; }
    public void setLastUpdateBy(String lastUpdateBy) { this.lastUpdateBy = lastUpdateBy; }
}