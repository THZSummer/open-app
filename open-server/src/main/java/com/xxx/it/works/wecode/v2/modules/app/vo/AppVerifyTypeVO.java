package com.xxx.it.works.wecode.v2.modules.app.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 认证方式 VO
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Data
public class AppVerifyTypeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 认证方式列表
     */
    private List<Integer> verifyType;

    /**
     * API 密钥
     */
    private String apiSecret;
}
