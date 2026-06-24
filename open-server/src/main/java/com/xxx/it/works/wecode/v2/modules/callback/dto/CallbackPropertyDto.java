package com.xxx.it.works.wecode.v2.modules.callback.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 回调属性 DTO
 *
 * <p>用于回调响应中展示属性信息</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
public class CallbackPropertyDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 属性名称
     */
    private String propertyName;

    /**
     * 属性值
     */
    private String propertyValue;

    public CallbackPropertyDto(String propertyName, String propertyValue) {
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }
}
