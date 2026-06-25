package com.xxx.it.works.wecode.v2.modules.chatbotbindtab.service;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.chatbotbindtab.vo.ChatbotAccountVO;

import java.util.List;

/**
 * 机器人绑定服务接口
 *
 * @author SDDU Build Agent
 * @version 2.0.0
 */
public interface ChatbotBindService {

    /**
     * 查询应用已绑定的机器人账号列表
     *
     * @param appId 应用业务 ID
     * @return 绑定列表
     */
    ApiResponse<List<ChatbotAccountVO>> getBoundAccounts(String appId);

    /**
     * 绑定机器人账号
     *
     * @param appId     应用业务 ID
     * @param accountId 机器人账号 ID
     * @param token     Authorization token（人工获取）
     * @return 绑定后的账号信息
     */
    ApiResponse<ChatbotAccountVO> bindAccount(String appId, String accountId, String token);

    /**
     * 解绑机器人账号
     *
     * @param appId     应用业务 ID
     * @param accountId 机器人账号 ID
     * @return 操作结果
     */
    ApiResponse<Void> unbindAccount(String appId, String accountId);
}
