package com.xxx.it.works.wecode.v2.modules.chatbotbindtab.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.common.security.AuthRole;
import com.xxx.it.works.wecode.v2.modules.chatbotbindtab.dto.ChatbotBindRequest;
import com.xxx.it.works.wecode.v2.modules.chatbotbindtab.service.ChatbotBindService;
import com.xxx.it.works.wecode.v2.modules.chatbotbindtab.vo.ChatbotAccountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 机器人绑定控制器
 *
 * <p>三个接口统一路径，通过 HTTP Method 区分：
 * <ul>
 *   <li>GET - 查询已绑定列表</li>
 *   <li>POST - 绑定</li>
 *   <li>DELETE - 解绑</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @version 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/service/open/v2/apps/single-chatbot-accounts")
@Tag(name = "机器人绑定管理", description = "应用绑定单聊机器人账号接口")
public class ChatbotBindController {

    @Autowired
    private ChatbotBindService chatbotBindService;

    /**
     * Authorization token（由人工通过工具类获取，配置在 application.yml）
     * <p>实际生产中应通过工具类动态获取，此处简化为配置项</p>
     */
    @Value("${wecontact.token:}")
    private String weContactToken;

    /**
     * 查询已绑定账号列表
     */
    @GetMapping
    @AuthRole
    @Operation(summary = "查询已绑定机器人账号列表")
    public ApiResponse<List<ChatbotAccountVO>> getBoundAccounts(
            @RequestParam("appId") String appId) {
        log.info("Get bound accounts, appId={}", appId);
        return chatbotBindService.getBoundAccounts(appId);
    }

    /**
     * 绑定机器人账号
     */
    @PostMapping
    @AuthRole
    @Operation(summary = "绑定机器人账号")
    public ApiResponse<ChatbotAccountVO> bindAccount(
            @Valid @RequestBody ChatbotBindRequest request) {
        log.info("Bind account, appId={}, accountId={}", request.getAppId(), request.getAccountId());
        return chatbotBindService.bindAccount(request.getAppId(), request.getAccountId(), weContactToken);
    }

    /**
     * 解绑机器人账号
     */
    @DeleteMapping
    @AuthRole
    @Operation(summary = "解绑机器人账号")
    public ApiResponse<Void> unbindAccount(
            @Valid @RequestBody ChatbotBindRequest request) {
        log.info("Unbind account, appId={}, accountId={}", request.getAppId(), request.getAccountId());
        return chatbotBindService.unbindAccount(request.getAppId(), request.getAccountId());
    }
}
