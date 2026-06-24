package com.xxx.it.works.wecode.v2.modules.app.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 创建应用响应 VO
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAppVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用ID
     */
    private String appId;
}
