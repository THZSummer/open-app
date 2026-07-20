package com.xxx.it.works.wecode.v2.modules.ability.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * 创建能力请求 DTO
 *
 * <p>包含能力目录创建所需的所有字段及 JSR-303 校验注解。
 * 图标/示意图仅接收前端上传后返回的 batchId，不在此接口上传文件。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Schema(description = "创建能力请求")
public class AdminAbilityCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "能力类型编码不能为空")
    @Schema(description = "能力类型编码", requiredMode = REQUIRED)
    private Integer abilityType;

    @NotBlank(message = "中文名不能为空")
    @Size(min = 2, max = 30, message = "中文名长度需在2-30字符之间")
    @Schema(description = "中文名", requiredMode = REQUIRED)
    private String nameCn;

    @NotBlank(message = "英文名不能为空")
    @Size(min = 2, max = 30, message = "英文名长度需在2-30字符之间")
    @Schema(description = "英文名", requiredMode = REQUIRED)
    private String nameEn;

    @NotBlank(message = "中文描述不能为空")
    @Size(min = 5, max = 200, message = "中文描述长度需在5-200字符之间")
    @Schema(description = "中文描述", requiredMode = REQUIRED)
    private String descCn;

    @NotBlank(message = "英文描述不能为空")
    @Size(min = 5, max = 200, message = "英文描述长度需在5-200字符之间")
    @Schema(description = "英文描述", requiredMode = REQUIRED)
    private String descEn;

    @Min(value = 1, message = "排序号不能小于1")
    @Schema(description = "排序号，不传则自动取当前最大值+1")
    private Integer orderNum;

    @Schema(description = "访问地址（loadType=2时必填，HTTP/HTTPS协议，最长1000字符）")
    private String entryUrl;

    @Schema(description = "路由路径（子应用激活路由）")
    private String routePath;

    @Schema(description = "别名（子应用唯一标识）")
    private String aliasName;

    @Schema(description = "隐藏：0=展示, 1=隐藏", defaultValue = "1")
    private Integer hidden = 1;

    @Schema(description = "需版本发布：0=即时生效, 1=需版本发布", defaultValue = "0")
    private Integer requireRelease = 0;

    @Schema(description = "加载类型：1=路由加载, 2=微前端加载", defaultValue = "1")
    private Integer loadType = 1;

    @NotBlank(message = "图标为必填项")
    @Schema(description = "图标batchId（由上传接口返回）", requiredMode = REQUIRED)
    private String iconBatchId;

    @Schema(description = "示意图batchId（由上传接口返回，非必填）")
    private String diagramBatchId;
}
