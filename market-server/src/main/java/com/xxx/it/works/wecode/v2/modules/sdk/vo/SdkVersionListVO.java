package com.xxx.it.works.wecode.v2.modules.sdk.vo;

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
 * SDK版本列表项 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "SDK版本列表项")
public class SdkVersionListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "版本ID")
    private Long id;

    @Schema(description = "版本号")
    private String versionName;

    @Schema(description = "更新说明")
    private String updateNotes;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "关联权限ID")
    private Long permissionId;

    @Schema(description = "关联权限名称")
    private String permissionName;

    @Schema(description = "关联权限scope")
    private String permissionScope;

    @Schema(description = "状态：1-已发布 2-已废弃")
    private Integer status;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @Schema(description = "最后更新人")
    private String lastUpdateBy;

    @Schema(description = "最后更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastUpdateTime;
}
