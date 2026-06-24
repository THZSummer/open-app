package com.xxx.it.works.wecode.v2.modules.app.vo;

import com.xxx.it.works.wecode.v2.common.file.vo.FileV2VO;
import lombok.Data;

import java.io.Serializable;

/**
 * 应用列表项 VO
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Data
public class AppListItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 中文名
     */
    private String nameCn;

    /**
     * 英文名
     */
    private String nameEn;

    /**
     * 应用图标
     */
    private FileV2VO icon;

    /**
     * 应用类型
     */
    private Integer appType;

    /**
     * 应用子类型
     */
    private Integer appSubType;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 是否绑定 EAMAP
     */
    private Boolean eamapBound;

    /**
     * 应用拥有者
     */
    private EmployeeInfoVO owner;

    /**
     * 当前用户角色
     */
    private Integer currentUserRole;

    /**
     * 最后更新时间
     */
    private String lastUpdateTime;

    public AppListItemVO() {
    }

    public AppListItemVO(String appId, String nameCn, String nameEn, FileV2VO icon, Integer appType, Integer appSubType, Integer status, Boolean eamapBound, EmployeeInfoVO owner, Integer currentUserRole, String lastUpdateTime) {
        this.appId = appId;
        this.nameCn = nameCn;
        this.nameEn = nameEn;
        this.icon = icon;
        this.appType = appType;
        this.appSubType = appSubType;
        this.status = status;
        this.eamapBound = eamapBound;
        this.owner = owner;
        this.currentUserRole = currentUserRole;
        this.lastUpdateTime = lastUpdateTime;
    }
}
