package com.xxx.it.works.wecode.v2.modules.ability.vo.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 能力列表响应 VO
 *
 * <p>管理面能力列表接口的输出字段，共 14 个业务字段 + 时间字段。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "能力列表响应 VO")
public class AdminAbilityVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "能力ID")
    private Long id;

    @Schema(description = "能力类型")
    private Integer abilityType;

    @Schema(description = "中文名")
    private String nameCn;

    @Schema(description = "英文名")
    private String nameEn;

    @Schema(description = "中文描述")
    private String descCn;

    @Schema(description = "英文描述")
    private String descEn;

    @Schema(description = "图标原始值（如 ability_icon_1）")
    private String icon;

    @Schema(description = "图标访问 URL")
    private String iconUrl;

    @Schema(description = "示意图原始值（如 ability_diagram_1）")
    private String exampleDiagram;

    @Schema(description = "示意图访问 URL")
    private String exampleDiagramUrl;

    @Schema(description = "排序号")
    private Integer orderNum;

    @Schema(description = "进入地址")
    private String entryUrl;

    @Schema(description = "是否在开放面展示：0=展示, 1=隐藏")
    private Integer hidden;

    @Schema(description = "路由路径")
    private String routePath;

    @Schema(description = "别名")
    private String aliasName;

    @Schema(description = "是否需要版本发布才生效：0=即时生效, 1=需版本发布")
    private Integer requireRelease;

    @Schema(description = "加载类型：1=路由加载, 2=微前端加载")
    private Integer loadType;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @Schema(description = "更新人")
    private String updateBy;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
