package com.xxx.it.works.wecode.v2.modules.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SYSTOKEN 白名单验证器
 *
 * <p>验证 SYSTOKEN 是否在允许的白名单令牌列表中</p>
 * <p>安全策略：空白名单（null 或空列表）视为全部禁止（与 URL 白名单策略相反）</p>
 * <p>采用严格匹配策略，仅当 SYSTOKEN 精确存在于白名单中才放行</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 * @see spec.md §5.2 安全白名单校验
 */
@Slf4j
@Component
public class SystokenWhitelistValidator {

    /**
     * 验证 SYSTOKEN 是否在白名单中
     *
     * @param systoken        待验证的 SYSTOKEN
     * @param whitelistTokens 白名单令牌列表
     * @return true-允许访问, false-拒绝访问
     */
    public boolean validate(String systoken, List<String> whitelistTokens) {
        // 空白名单 = 全部禁止（与 URL 白名单策略相反，确保默认安全）
        if (whitelistTokens == null || whitelistTokens.isEmpty()) {
            log.warn("SYSTOKEN whitelist validation rejected: whitelist is empty");
            return false;
        }

        // SYSTOKEN 为空则拒绝
        if (systoken == null || systoken.isEmpty()) {
            log.warn("SYSTOKEN whitelist validation rejected: systoken is empty");
            return false;
        }

        // 检查 SYSTOKEN 是否在白名单中
        if (whitelistTokens.contains(systoken)) {
            return true;
        }

        // 不在白名单中
        log.warn("SYSTOKEN not in whitelist: token={}", systoken);
        return false;
    }
}
