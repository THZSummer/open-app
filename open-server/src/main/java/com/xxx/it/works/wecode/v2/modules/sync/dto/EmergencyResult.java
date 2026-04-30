package com.xxx.it.works.wecode.v2.modules.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 应急接口响应结果
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 成功数量
     */
    private int success;

    /**
     * 失败数量
     */
    private int failed;

    /**
     * 新增数量
     */
    private int inserted;

    /**
     * 更新数量
     */
    private int updated;

    /**
     * 详细结果列表
     */
    private List<EmergencyDetail> details;
}
