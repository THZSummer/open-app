package com.xxx.it.works.wecode.v2.modules.sdk.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * SDK权限 VO（从 open 库联查）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "SDK权限")
public class SdkPermissionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "权限ID")
    private Long id;

    @Schema(description = "权限中文名")
    private String nameCn;

    @Schema(description = "权限英文名")
    private String nameEn;

    @Schema(description = "权限scope")
    private String scope;
}
