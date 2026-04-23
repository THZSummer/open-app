-- ============================================================================
-- 能力开放平台测试数据初始化脚本
-- 版本: v1.0
-- 创建日期: 2026-04-23
-- 说明: 测试环境数据准备，解决测试数据唯一性冲突
-- 执行顺序: 03（测试数据，仅测试环境使用）
-- 注意: 本脚本依赖02脚本中的默认分类数据
-- ============================================================================

-- 禁用外键检查（避免删除顺序问题）
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 1. 清理旧的测试数据
-- =====================================================

-- 清理订阅数据
DELETE FROM openplatform_v2_subscription_t WHERE id >= 400 AND id < 500;

-- 清理审批日志和记录
DELETE FROM openplatform_v2_approval_log_t WHERE record_id >= 500 AND record_id < 600;
DELETE FROM openplatform_v2_approval_record_t WHERE id >= 500 AND id < 600;

-- 清理权限数据
DELETE FROM openplatform_v2_permission_p_t WHERE parent_id >= 1000 AND parent_id < 4000;
DELETE FROM openplatform_v2_permission_t WHERE id >= 1000 AND id < 4000;

-- 清理API属性和API
DELETE FROM openplatform_v2_api_p_t WHERE parent_id >= 100 AND parent_id < 200;
DELETE FROM openplatform_v2_api_t WHERE id >= 100 AND id < 200;

-- 清理事件属性和事件
DELETE FROM openplatform_v2_event_p_t WHERE parent_id >= 200 AND parent_id < 300;
DELETE FROM openplatform_v2_event_t WHERE id >= 200 AND id < 300;

-- 清理回调属性和回调
DELETE FROM openplatform_v2_callback_p_t WHERE parent_id >= 300 AND parent_id < 400;
DELETE FROM openplatform_v2_callback_t WHERE id >= 300 AND id < 400;

-- 清理分类责任人（测试数据）
DELETE FROM openplatform_v2_category_owner_t WHERE id >= 100 AND id < 200;

-- =====================================================
-- 2. 初始化API数据（使用默认分类）
-- =====================================================

INSERT INTO openplatform_v2_api_t 
(id, name_cn, name_en, path, method, category_id, status, create_time, last_update_time, create_by, last_update_by)
VALUES
-- 已发布的API（消息分类下）
(100, '发送消息API', 'Send Message API', '/api/im/send-message', 'POST', 305652258039660544, 2, NOW(3), NOW(3), 'system', 'system'),
(101, '获取用户信息API', 'Get User Info API', '/api/user/info', 'GET', 305652258039660544, 2, NOW(3), NOW(3), 'system', 'system'),
-- 待审状态的API（会议分类下）
(102, '待审API-测试用', 'Pending API For Test', '/api/meeting/pending', 'GET', 305652314155253760, 1, NOW(3), NOW(3), 'system', 'system'),
-- 草稿状态的API（会议分类下）
(103, '草稿API-测试用', 'Draft API For Test', '/api/meeting/draft', 'GET', 305652314155253760, 0, NOW(3), NOW(3), 'system', 'system')
ON DUPLICATE KEY UPDATE 
    name_cn = VALUES(name_cn),
    status = VALUES(status),
    category_id = VALUES(category_id),
    last_update_time = NOW(3);

-- =====================================================
-- 3. 初始化事件数据（使用默认分类）
-- =====================================================

INSERT INTO openplatform_v2_event_t 
(id, name_cn, name_en, topic, category_id, status, create_time, last_update_time, create_by, last_update_by)
VALUES
-- 已发布的事件（事件分类下）
(200, '消息接收事件', 'Message Received Event', 'im.message.received.test', 305653292027871232, 2, NOW(3), NOW(3), 'system', 'system'),
-- 待审状态的事件（事件分类下）
(201, '待审事件-测试用', 'Pending Event For Test', 'test.event.pending', 305653292027871232, 1, NOW(3), NOW(3), 'system', 'system'),
-- 草稿状态的事件（事件分类下）
(202, '草稿事件-测试用', 'Draft Event For Test', 'test.event.draft', 305653292027871232, 0, NOW(3), NOW(3), 'system', 'system')
ON DUPLICATE KEY UPDATE 
    name_cn = VALUES(name_cn),
    status = VALUES(status),
    category_id = VALUES(category_id),
    last_update_time = NOW(3);

-- =====================================================
-- 4. 初始化回调数据（使用默认分类）
-- =====================================================

INSERT INTO openplatform_v2_callback_t 
(id, name_cn, name_en, category_id, status, create_time, last_update_time, create_by, last_update_by)
VALUES
-- 已发布的回调（回调分类下）
(300, '审批完成回调', 'Approval Completed Callback', 305654385424203776, 2, NOW(3), NOW(3), 'system', 'system'),
-- 待审状态的回调（回调分类下）
(301, '待审回调-测试用', 'Pending Callback For Test', 305654385424203776, 1, NOW(3), NOW(3), 'system', 'system'),
-- 草稿状态的回调（回调分类下）
(302, '草稿回调-测试用', 'Draft Callback For Test', 305654385424203776, 0, NOW(3), NOW(3), 'system', 'system')
ON DUPLICATE KEY UPDATE 
    name_cn = VALUES(name_cn),
    status = VALUES(status),
    category_id = VALUES(category_id),
    last_update_time = NOW(3);

-- =====================================================
-- 5. 初始化权限数据（使用默认分类）
-- =====================================================

-- API权限（消息分类）
INSERT INTO openplatform_v2_permission_t 
(id, name_cn, name_en, scope, resource_type, resource_id, category_id, status, create_time, last_update_time, create_by, last_update_by)
VALUES
(1000, '发送消息权限', 'Send Message Permission', 'api:im:send-message:test', 'api', 100, 305652258039660544, 1, NOW(3), NOW(3), 'system', 'system'),
(1001, '获取用户信息权限', 'Get User Info Permission', 'api:user:info:test', 'api', 101, 305652258039660544, 1, NOW(3), NOW(3), 'system', 'system'),
(1002, '待审API权限-测试用', 'Pending API Permission For Test', 'api:meeting:pending:test', 'api', 102, 305652314155253760, 1, NOW(3), NOW(3), 'system', 'system'),
(1003, '草稿API权限-测试用', 'Draft API Permission For Test', 'api:meeting:draft:test', 'api', 103, 305652314155253760, 1, NOW(3), NOW(3), 'system', 'system')
ON DUPLICATE KEY UPDATE 
    name_cn = VALUES(name_cn),
    scope = VALUES(scope),
    category_id = VALUES(category_id),
    last_update_time = NOW(3);

-- 事件权限（事件分类）
INSERT INTO openplatform_v2_permission_t 
(id, name_cn, name_en, scope, resource_type, resource_id, category_id, status, create_time, last_update_time, create_by, last_update_by)
VALUES
(2000, '消息接收权限', 'Message Received Permission', 'event:im:message-received:test', 'event', 200, 305653292027871232, 1, NOW(3), NOW(3), 'system', 'system'),
(2001, '待审事件权限-测试用', 'Pending Event Permission For Test', 'event:test:pending:test', 'event', 201, 305653292027871232, 1, NOW(3), NOW(3), 'system', 'system')
ON DUPLICATE KEY UPDATE 
    name_cn = VALUES(name_cn),
    scope = VALUES(scope),
    category_id = VALUES(category_id),
    last_update_time = NOW(3);

-- 回调权限（回调分类）
INSERT INTO openplatform_v2_permission_t 
(id, name_cn, name_en, scope, resource_type, resource_id, category_id, status, create_time, last_update_time, create_by, last_update_by)
VALUES
(3000, '审批完成回调权限', 'Approval Completed Callback Permission', 'callback:approval:completed:test', 'callback', 300, 305654385424203776, 1, NOW(3), NOW(3), 'system', 'system'),
(3001, '待审回调权限-测试用', 'Pending Callback Permission For Test', 'callback:test:pending:test', 'callback', 301, 305654385424203776, 1, NOW(3), NOW(3), 'system', 'system')
ON DUPLICATE KEY UPDATE 
    name_cn = VALUES(name_cn),
    scope = VALUES(scope),
    category_id = VALUES(category_id),
    last_update_time = NOW(3);

-- =====================================================
-- 6. 初始化订阅数据
-- =====================================================

-- API订阅（待审状态）
INSERT INTO openplatform_v2_subscription_t 
(id, app_id, permission_id, status, channel_type, channel_address, auth_type, create_time, last_update_time, create_by, last_update_by, approved_at, approved_by)
VALUES
(400, 10, 1000, 0, 1, 'http://localhost/webhook', 0, NOW(3), NOW(3), 'test_user', 'test_user', NULL, NULL)
ON DUPLICATE KEY UPDATE 
    status = VALUES(status),
    last_update_time = NOW(3);

-- 事件订阅（已授权状态）
INSERT INTO openplatform_v2_subscription_t 
(id, app_id, permission_id, status, channel_type, channel_address, auth_type, create_time, last_update_time, create_by, last_update_by, approved_at, approved_by)
VALUES
(401, 10, 2000, 1, 1, 'http://localhost/webhook/events', 0, NOW(3), NOW(3), 'test_user', 'test_user', NOW(3), 'admin')
ON DUPLICATE KEY UPDATE 
    status = VALUES(status),
    last_update_time = NOW(3);

-- 事件订阅（待审状态）
INSERT INTO openplatform_v2_subscription_t 
(id, app_id, permission_id, status, channel_type, channel_address, auth_type, create_time, last_update_time, create_by, last_update_by, approved_at, approved_by)
VALUES
(402, 10, 2001, 0, 1, 'http://localhost/webhook', 0, NOW(3), NOW(3), 'test_user', 'test_user', NULL, NULL)
ON DUPLICATE KEY UPDATE 
    status = VALUES(status),
    last_update_time = NOW(3);

-- 回调订阅（已授权状态）
INSERT INTO openplatform_v2_subscription_t 
(id, app_id, permission_id, status, channel_type, channel_address, auth_type, create_time, last_update_time, create_by, last_update_by, approved_at, approved_by)
VALUES
(403, 10, 3000, 1, 0, 'http://localhost/webhook/callbacks', 0, NOW(3), NOW(3), 'test_user', 'test_user', NOW(3), 'admin')
ON DUPLICATE KEY UPDATE 
    status = VALUES(status),
    last_update_time = NOW(3);

-- 回调订阅（待审状态）
INSERT INTO openplatform_v2_subscription_t 
(id, app_id, permission_id, status, channel_type, channel_address, auth_type, create_time, last_update_time, create_by, last_update_by, approved_at, approved_by)
VALUES
(404, 10, 3001, 0, 0, 'http://localhost/webhook', 0, NOW(3), NOW(3), 'test_user', 'test_user', NULL, NULL)
ON DUPLICATE KEY UPDATE 
    status = VALUES(status),
    last_update_time = NOW(3);

-- =====================================================
-- 7. 初始化审批记录数据（使用02脚本的审批流程）
-- =====================================================

-- 待审状态的审批记录
INSERT INTO openplatform_v2_approval_record_t 
(id, flow_id, business_type, business_id, applicant_id, applicant_name, status, current_node, create_time, last_update_time, create_by, last_update_by, completed_at)
VALUES
-- 用于同意审批测试
(500, 1, 'api_register', 102, 'test_user', '测试用户', 0, 0, NOW(3), NOW(3), 'test_user', 'test_user', NULL),
-- 用于驳回审批测试
(501, 1, 'api_register', 102, 'test_user', '测试用户', 0, 0, NOW(3), NOW(3), 'test_user', 'test_user', NULL),
-- 用于撤销审批测试
(502, 1, 'api_register', 102, 'test_user', '测试用户', 0, 0, NOW(3), NOW(3), 'test_user', 'test_user', NULL),
-- 用于批量同意审批测试
(503, 1, 'api_register', 102, 'test_user', '测试用户', 0, 0, NOW(3), NOW(3), 'test_user', 'test_user', NULL),
-- 用于批量驳回审批测试
(504, 1, 'api_register', 102, 'test_user', '测试用户', 0, 0, NOW(3), NOW(3), 'test_user', 'test_user', NULL)
ON DUPLICATE KEY UPDATE 
    status = VALUES(status),
    last_update_time = NOW(3);

-- =====================================================
-- 8. 初始化分类责任人数据（使用默认分类）
-- =====================================================

INSERT IGNORE INTO openplatform_v2_category_owner_t 
(id, category_id, user_id, user_name, create_time, last_update_time, create_by, last_update_by)
VALUES
(100, 305652258039660544, 'user001', '张三', NOW(3), NOW(3), 'system', 'system'),
(101, 305652258039660544, 'user002', '李四', NOW(3), NOW(3), 'system', 'system'),
(102, 305654385424203776, 'user003', '王五', NOW(3), NOW(3), 'system', 'system');

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- 初始化完成
-- =====================================================
SELECT '测试数据初始化完成！' AS message;
SELECT 'API数据：' AS info, COUNT(*) AS count FROM openplatform_v2_api_t WHERE id >= 100 AND id < 200;
SELECT '事件数据：' AS info, COUNT(*) AS count FROM openplatform_v2_event_t WHERE id >= 200 AND id < 300;
SELECT '回调数据：' AS info, COUNT(*) AS count FROM openplatform_v2_callback_t WHERE id >= 300 AND id < 400;
SELECT '权限数据：' AS info, COUNT(*) AS count FROM openplatform_v2_permission_t WHERE id >= 1000 AND id < 4000;
SELECT '订阅数据：' AS info, COUNT(*) AS count FROM openplatform_v2_subscription_t WHERE id >= 400 AND id < 500;
SELECT '审批记录：' AS info, COUNT(*) AS count FROM openplatform_v2_approval_record_t WHERE id >= 500 AND id < 600;