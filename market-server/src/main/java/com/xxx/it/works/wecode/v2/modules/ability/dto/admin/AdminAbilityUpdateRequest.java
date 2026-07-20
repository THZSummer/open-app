package com.xxx.it.works.wecode.v2.modules.ability.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 编辑能力请求 DTO
 *
 * <p>所有字段均为可选，仅更新传入的非空字段。
 * abilityType 不可修改（请求中即使传入也会被忽略）。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Schema(description = "编辑能力请求，所有字段可选")
public class AdminAbilityUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Size(min = 2, max = 30, message = "中文名长度需在2-30字符之间")
    @Schema(description = "中文名")
    private String nameCn;

    @Size(min = 2, max = 30, message = "英文名长度需在2-30字符之间")
    @Schema(description = "英文名")
    private String nameEn;

    @Size(min = 5, max = 200, message = "中文描述长度需在5-200字符之间")
    @Schema(description = "中文描述")
    private String descCn;

    @Size(min = 5, max = 200, message = "英文描述长度需在5-200字符之间")
    @Schema(description = "英文描述")
    private String descEn;

    @Min(value = 1, message = "排序号不能小于1")
    @Schema(description = "排序号，不传则不修改")
    private Integer orderNum;

    @Schema(description = "访问地址（loadType=2时必填，HTTP/HTTPS协议，最长1000字符）")
    private String entryUrl;

    @Schema(description = "路由路径（子应用激活路由）")
    private String routePath;

    @Schema(description = "别名（子应用唯一标识）")
    private String aliasName;

    @Schema(description = "隐藏：0=展示, 1=隐藏")
    private Integer hidden;

    @Schema(description = "需版本发布：0=即时生效, 1=需版本发布")
    private Integer requireRelease;

    @Schema(description = "加载类型：1=路由加载, 2=微前端加载")
    private Integer loadType;

    @Schema(description = "图标batchId（由上传接口返回，不传则不修改）")
    private String iconBatchId;

    @Schema(description = "示意图batchId（由上传接口返回，不传则不修改）")
    private String diagramBatchId;

    /**
     * 乐观锁：记录的最后更新时间，由客户端在更新时传入。
     * 若与数据库不一致，说明数据已被修改，返回冲突提示。
     */
    @Schema(description = "乐观锁字段：记录的最后更新时间，用于冲突检测")
    private Date lastUpdateTime;
}
