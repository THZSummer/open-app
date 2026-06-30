-- ============================================================================
-- 通道地址白名单初始化数据
-- 关联: ADR-004 事件/回调通道地址白名单控制
-- 日期: 2026-06-29
-- 说明: 平台管理员通过数据字典管理(openplatform_property_t)维护白名单规则
--       path = channel_address_whitelist
--       code = callback_url_regex_{seq} 或 event_url_regex_{seq}
--       value = 正则表达式（全串匹配 Pattern.matches）
--
-- ⚠️ 重要: 以下规则为示例，请根据实际业务需求修改后执行
--          空白名单 = 不限制，初期可暂不插入数据
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 回调通道地址白名单 (callback_url_regex_*)
-- 适用场景: FR-022 消费方配置回调接收地址时校验
-- ----------------------------------------------------------------------------
INSERT INTO openplatform_property_t
    (path, code, name, value, description, status, create_by, create_time, last_update_by, last_update_time)
VALUES
    ('channel_address_whitelist', 'callback_url_regex_001',
     '回调白名单-企业域名',
     '^https://.*\\.corp\\.example\\.com/.*$',
     '允许回调到企业域名下的任意 HTTPS 地址',
     1, 'system', NOW(3), 'system', NOW(3)),

    ('channel_address_whitelist', 'callback_url_regex_002',
     '回调白名单-合作伙伴',
     '^https://webhook\\.partner\\.com/.*$',
     '允许回调到指定合作伙伴 WebHook 地址',
     1, 'system', NOW(3), 'system', NOW(3));

-- ----------------------------------------------------------------------------
-- 事件通道地址白名单 (event_url_regex_*)
-- 适用场景: FR-019 消费方配置事件接收地址时校验
-- ----------------------------------------------------------------------------
INSERT INTO openplatform_property_t
    (path, code, name, value, description, status, create_by, create_time, last_update_by, last_update_time)
VALUES
    ('channel_address_whitelist', 'event_url_regex_001',
     '事件白名单-企业域名',
     '^https://.*\\.corp\\.example\\.com/.*$',
     '允许事件推送到企业域名下的任意 HTTPS 地址',
     1, 'system', NOW(3), 'system', NOW(3));

-- ----------------------------------------------------------------------------
-- 验证查询
-- ----------------------------------------------------------------------------
-- 查看已配置的白名单规则:
-- SELECT path, code, name, value, status
-- FROM openplatform_property_t
-- WHERE path = 'channel_address_whitelist'
-- ORDER BY code;
--
-- 按类型统计:
-- SELECT
--   CASE
--     WHEN code LIKE 'callback_url_regex%' THEN '回调白名单'
--     WHEN code LIKE 'event_url_regex%'    THEN '事件白名单'
--   END AS type,
--   COUNT(*) AS rule_count
-- FROM openplatform_property_t
-- WHERE path = 'channel_address_whitelist' AND status = 1
-- GROUP BY type;
