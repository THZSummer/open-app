package com.xxx.it.works.wecode.v2.modules.permission.validator;

import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.modules.app.mapper.AppMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 通道地址白名单校验器
 *
 * <p>在消费方配置事件/回调通道地址时，校验地址是否命中平台管理员维护的正则白名单。</p>
 *
 * <h3>设计要点</h3>
 * <ul>
 *   <li>写入时校验（非运行时），频次低，不做缓存</li>
 *   <li>空白名单 = 不限制（兼容初期上线）</li>
 *   <li>使用 Pattern.matches() 全串匹配（非 find）</li>
 * </ul>
 *
 * <h3>存储约定</h3>
 * <p>复用 openplatform_property_t 字典表：</p>
 * <ul>
 *   <li>path = channel_address_whitelist</li>
 *   <li>code = callback_url_regex_{seq} 或 event_url_regex_{seq}</li>
 *   <li>value = 正则表达式（一条规则一行记录）</li>
 * </ul>
 *
 * @author Summer
 * @see <a href="ADR-004">ADR-004: 事件/回调通道地址白名单控制</a>
 * @since 2026-06-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelAddressWhitelistValidator {

    private static final String PATH = "channel_address_whitelist";

    /**
     * 回调通道地址白名单 code 前缀
     */
    public static final String CODE_PREFIX_CALLBACK = "callback_url_regex";

    /**
     * 事件通道地址白名单 code 前缀
     */
    public static final String CODE_PREFIX_EVENT = "event_url_regex";

    private final AppMapper appMapper;

    /**
     * 校验通道地址是否在白名单内
     *
     * <p>规则：</p>
     * <ol>
     *   <li>channelAddress 为空时不校验（由调用方决定是否必填）</li>
     *   <li>白名单为空时放行（空白名单 = 不限制）</li>
     *   <li>命中任一有效正则规则即放行</li>
     *   <li>全部不命中则拒绝，抛出 BusinessException</li>
     * </ol>
     *
     * @param channelAddress 消费方配置的通道地址
     * @param codePrefix     规则前缀：{@link #CODE_PREFIX_CALLBACK} 或 {@link #CODE_PREFIX_EVENT}
     * @throws BusinessException 地址不在白名单内（code=400）
     */
    public void validate(String channelAddress, String codePrefix) {
        // 地址为空时不校验
        if (!StringUtils.hasText(channelAddress)) {
            return;
        }

        // 查询白名单规则
        List<String> patterns;
        try {
            patterns = appMapper.selectDictionaryValuesByPathAndCodePrefix(PATH, codePrefix);
        } catch (Exception e) {
            log.warn("Failed to query channel address whitelist (path={}, codePrefix={}), allowing all",
                    PATH, codePrefix, e);
            return;
        }

        // 空白名单 = 不限制
        if (patterns == null || patterns.isEmpty()) {
            log.debug("Channel address whitelist is empty (codePrefix={}), allowing all", codePrefix);
            return;
        }

        // 逐条匹配，命中任一即放行
        for (String regex : patterns) {
            try {
                if (Pattern.compile(regex).matcher(channelAddress).matches()) {
                    log.debug("Channel address [{}] matched whitelist rule [{}]", channelAddress, regex);
                    return;
                }
            } catch (PatternSyntaxException e) {
                // 跳过非法正则（不应出现，字典写入时应已校验）
                log.warn("Invalid regex in whitelist: [{}], skipping", regex, e);
            }
        }

        // 全部不命中 → 拒绝
        log.info("Channel address [{}] not in whitelist (codePrefix={}), rejected", channelAddress, codePrefix);
        throw new BusinessException(
                "400",
                "通道地址不在允许范围内，请联系平台管理员配置白名单",
                "Channel address is not in the allowed whitelist"
        );
    }
}
