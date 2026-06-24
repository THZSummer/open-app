package com.xxx.it.works.wecode.v2.modules.app.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用凭证 VO
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Data
public class AppIdentityVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Access Key
     */
    private String ak;

    /**
     * Secret Key
     */
    private String sk;

    public AppIdentityVO() {
    }

    public AppIdentityVO(String ak, String sk) {
        this.ak = ak;
        this.sk = sk;
    }
}
