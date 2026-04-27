package com.xxx.api.common.service;

/**
 * 应用服务接口
 * 
 * <p>提供应用相关的查询能力</p>
 * <p>注意：此接口为预留接口，实际实现需要对接现有的应用管理系统或 AKSK 管理系统</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
public interface ApplicationService {

    /**
     * 通过 AK 获取应用ID
     * 
     * <p>对接现有 AKSK 管理系统，根据 Access Key 查询对应的应用ID</p>
     * 
     * @param ak Access Key
     * @return 应用ID，未找到返回 null
     */
    Long getAppIdByAk(String ak);

    /**
     * 验证应用身份
     * 
     * <p>验证 AKSK 签名或 Bearer Token</p>
     * 
     * @param appId 应用ID
     * @param authType 认证类型
     * @param authCredential 认证凭证
     * @return 是否验证通过
     */
    boolean verifyApplication(String appId, Integer authType, String authCredential);
}