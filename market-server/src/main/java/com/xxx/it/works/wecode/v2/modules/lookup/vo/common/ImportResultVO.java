package com.xxx.it.works.wecode.v2.modules.lookup.vo.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 导入结果VO
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "导入结果")
public class ImportResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 总记录数
     */
    @Schema(description = "总记录数",
            example = "100")
    private Integer totalCount;

    /**
     * 成功导入数
     */
    @Schema(description = "成功导入数",
            example = "95")
    private Integer successCount;

    /**
     * 失败数
     */
    @Schema(description = "失败数",
            example = "5")
    private Integer failCount;

    /**
     * 失败明细列表
     */
    @Schema(description = "失败明细列表")
    private List<ImportFailRecordVO> failList;

    /**
     * 导入失败记录VO
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "导入失败记录")
    public static class ImportFailRecordVO implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 行号
         */
        @Schema(description = "行号",
                example = "10")
        private Integer rowNum;

        /**
         * 项编码
         */
        @Schema(description = "项编码",
                example = "ADMIN")
        private String itemCode;

        /**
         * 错误信息
         */
        @Schema(description = "错误信息",
                example = "项编码已存在，自动跳过")
        private String errorMsg;
    }
}
