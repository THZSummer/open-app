package com.xxx.it.works.wecode.v2.modules.app.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * EAMAP VO
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Data
@NoArgsConstructor
public class EamapVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * EAMAP 应用编码
     */
    private String eamapAppCode;

    /**
     * EAMAP 应用名称
     */
    private String eamapAppName;

    public EamapVO(String eamapAppCode, String eamapAppName) {
        this.eamapAppCode = eamapAppCode;
        this.eamapAppName = eamapAppName;
    }
}
