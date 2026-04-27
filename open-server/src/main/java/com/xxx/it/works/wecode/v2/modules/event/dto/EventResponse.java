package com.xxx.it.works.wecode.v2.modules.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 事件响应
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Schema(description = "事件详情响应")
public class EventResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件ID
     */
    @Schema(description = "事件ID")
    private String id;

    /**
     * 中文名称
     */
    @Schema(description = "中文名称")
    private String nameCn;

    /**
     * 英文名称
     */
    @Schema(description = "英文名称")
    private String nameEn;

    /**
     * 事件 Topic
     */
    @Schema(description = "事件 Topic")
    private String topic;

    /**
     * 所属分类ID
     */
    @Schema(description = "所属分类ID")
    private String categoryId;

    /**
     * 分类名称
     */
    @Schema(description = "分类名称")
    private String categoryName;

    /**
     * 状态：0=草稿, 1=待审, 2=已发布, 3=已下线
     */
    @Schema(description = "状态：0=草稿, 1=待审, 2=已发布, 3=已下线")
    private Integer status;

    /**
     * 权限信息
     */
    @Schema(description = "权限信息")
    private PermissionDto permission;

    /**
     * 扩展属性列表
     */
    @Schema(description = "扩展属性列表")
    private List<EventPropertyDto> properties;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 创建人
     */
    @Schema(description = "创建人")
    private String createBy;
}
