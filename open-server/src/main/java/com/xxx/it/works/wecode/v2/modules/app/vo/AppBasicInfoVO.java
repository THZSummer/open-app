package com.xxx.it.works.wecode.v2.modules.app.vo;

import com.xxx.it.works.wecode.v2.common.file.vo.FileV2VO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 应用基本信息 VO
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppBasicInfoVO implements Serializable {

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
     * 中文描述
     */
    private String descCn;

    /**
     * 英文描述
     */
    private String descEn;

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
     * EAMAP 应用编码
     */
    private String eamapAppCode;

    /**
     * EAMAP 应用名称
     */
    private String eamapAppName;

    /**
     * 架构图列表
     */
    private List<FileV2VO> diagramIdList;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 最后更新人
     */
    private String lastUpdateBy;

    /**
     * 最后更新时间
     */
    private String lastUpdateTime;
}
