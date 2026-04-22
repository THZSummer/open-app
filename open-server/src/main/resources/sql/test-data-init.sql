-- =====================================================
-- 能力开放平台测试数据初始化脚本
-- 用于测试环境数据准备，解决测试数据唯一性冲突
-- 更新时间：2026-04-22
-- =====================================================

-- 禁用外键检查（避免删除顺序问题）
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 1. 清理旧的测试数据
-- =====================================================

-- 清理订阅数据（使用数值应用ID）
DELETE FROM openplatform_v2_subscription_t WHERE id >= 400 AND id < 500;

-- 清理审批日志和记录
DELETE FROM openplatform_v2_approval_log_t WHERE record_id >= 500 AND record_id < 600;
DELETE FROM openplatform_v2_approval_record_t WHERE id >= 500 AND id < 600;

-- 清理审批流程（ID=1和ID=2用于测试）
DELETE FROM openplatform_v2_approval_flow_t WHERE id IN (1, 2);

-- 清理权限数据
DELETE FROM openplatform_v2_permission_p_t WHERE parent_id >= 1000 AND parent_id < 4000;
DELETE FROM openplatform_v2_permission_t WHERE id >= 1000 AND id < 4000;

-- 清理API属性和API
DELETE FROM openplatform_v2_api_p_t WHERE parent_id >= 100 AND parent_id < 200;
DELETE FROM openplatform_v2_api_t WHERE id >= 100 AND id < 200;

-- 清理事件属性和事件
DELETE FROM openplatform_v2_event_p_t WHERE parent_id >= 200 AND parent_id < 300;
DELETE FROM openplatform_v2_event_t WHERE id >= 200 AND id < 300;

-- 清调回调属性和回调
DELETE FROM openplatform_v2_callback_p_t WHERE parent_id >= 300 AND parent_id < 400;
DELETE FROM openplatform_v2_callback_t WHERE id >= 300 AND id < 400;

-- 清理分类责任人
DELETE FROM openplatform_v2_category_owner_t WHERE category_id >= 1 AND category_id < 100;

-- =====================================================
-- 2. 确保测试应用存在 (使用数值ID)
-- =====================================================

-- 确保应用ID=10存在
INSERT IGNORE INTO applications 
(id, name, description, type, status, owner_id, owner_type, deleted_at, version, created_at, created_by, updated_at, updated_by)
VALUES
(10, '测试应用', '用于接口测试的测试应用', 'WEB', 'active', 'test_user', 'USER', 0, 1, NOW(), 'test_user', NOW(), 'test_user');

-- =====================================================
-- 3. 初始化分类数据（确保测试所需的分类存在）
-- =====================================================

-- 确保测试分类存在
INSERT IGNORE INTO openplatform_v2_category_t 
(id, category_alias, name_cn, name_en, parent_id, path, sort_order, status, create_time, last_update_time, create_by, last_update_by)
VALUES
(1, 'im', '即时通讯', 'Instant Messaging', 0, '/1/', 1, 1, NOW(), NOW(), 'system', 'system'),
(2, 'approval', '审批管理', 'Approval Management', 0, '/2/', 2, 1, NOW(), NOW(), 'system', 'system'),
(3, 'workflow', '工作流', 'Workflow', 0, '/3/', 3, 1, NOW(), NOW(), 'system', 'system'),
(4, 'test', '测试分类', 'Test Category', 0, '/4/', 4, 1, NOW(), NOW(), 'system', 'system'),
(5, 'callback_test', '回调测试分类', 'Callback Test Category', 0, '/5/', 5, 1, NOW(), NOW(), 'system', 'system'),
(99, 'delete_test', '删除测试分类', 'Delete Test Category', 0, '/99/', 99, 1, NOW(), NOW(), 'system', 'system');

-- =====================================================
-- 4. 初始化API数据
-- =====================================================

-- 已发布的API（用于查询测试）
INSERT INTO openplatform_v2_api_t 
(id, name_cn, name_en, path, method, category_id, status, create_time, last_update_time, create_by, last_update_by)
VALUES
(100, '发送消息API', 'Send Message API', '/api/im/send-message', 'POST', 1, 2, NOW(), NOW(), 'system', 'system'),
(101, '获取用户信息API', 'Get User Info API', '/api/user/info', 'GET', 1, 2, NOW(), NOW(), 'system', 'system'),
-- 待审状态的API（用于撤回测试 TC-API-006）
(102, '待审API-测试用', 'Pending API For Test', '/api/test/pending', 'GET', 4, 1, NOW(), NOW(), 'system', 'system'),
-- 草稿状态的API（用于删除测试 TC-API-005）
(103, '草稿API-测试用', 'Draft API For Test', '/api/test/draft', 'GET', 4, 0, NOW(), NOW(), 'system', 'system')
ON DUPLICATE KEY UPDATE 
    name_cn = VALUES(name_cn),
    status = VALUES(status),
    last_update_time = NOW();

-- =====================================================
-- 5. 初始化事件数据
-- =====================================================

-- 已发布的事件（用于查询测试）
INSERT INTO openplatform_v2_event_t 
(id, name_cn, name_en, topic, category_id, status, create_time, last_update_time, create_by, last_update_by)
VALUES
(200, '消息接收事件', 'Message Received Event', 'im.message.received.test', 1, 2, NOW(), NOW(), 'system', 'system'),
-- 待审状态的事件（用于撤回测试 TC-EVENT-006）
(201, '待审事件-测试用', 'Pending Event For Test', 'test.event.pending', 4, 1, NOW(), NOW(), 'system', 'system'),
-- 草稿状态的事件（用于删除测试 TC-EVENT-005）
(202, '草稿事件-测试用', 'Draft Event For Test', 'test.event.draft', 4, 0, NOW(), NOW(), 'system', 'system')
ON DUPLICATE KEY UPDATE 
    name_cn = VALUES(name_cn),
    status = VALUES(status),
    last_update_time = NOW();

-- =====================================================
-- 6. 初始化回调数据
-- =====================================================

-- 已发布的回调（用于查询测试）
INSERT INTO openplatform_v2_callback_t 
(id, name_cn, name_en, category_id, status, create_time, last_update_time, create_by, last_update_by)
VALUES
(300, '审批完成回调', 'Approval Completed Callback', 2, 2, NOW(), NOW(), 'system', 'system'),
-- 待审状态的回调（用于撤回测试 TC-CALLBACK-006）
(301, '待审回调-测试用', 'Pending Callback For Test', 4, 1, NOW(), NOW(), 'system', 'system'),
-- 草稿状态的回调（用于删除测试 TC-CALLBACK-005）
(302, '草稿回调-测试用', 'Draft Callback For Test', 4, 0, NOW(), NOW(), 'system', 'system')
ON DUPLICATE KEY UPDATE 
    name_cn = VALUES(name_cn),
    status = VALUES(status),
    last_update_time = NOW();

-- =====================================================
-- 7. 初始化权限数据
-- =====================================================

-- API权限
INSERT INTO openplatform_v2_permission_t 
(id, name_cn, name_en, scope, resource_type, resource_id, category_id, status, create_time, last_update_time, create_by, last_update_by)
VALUES
(1000, '发送消息权限', 'Send Message Permission', 'api:im:send-message:test', 'api', 100, 1, 1, NOW(), NOW(), 'system', 'system'),
(1001, '获取用户信息权限', 'Get User Info Permission', 'api:user:info:test', 'api', 101, 1, 1, NOW(), NOW(), 'system', 'system'),
(1002, '待审API权限-测试用', 'Pending API Permission For Test', 'api:test:pending', 'api', 102, 4, 1, NOW(), NOW(), 'system', 'system'),
(1003, '草稿API权限-测试用', 'Draft API Permission For Test', 'api:test:draft', 'api', 103, 4, 1, NOW(), NOW(), 'system', 'system')
ON DUPLICATE KEY UPDATE 
    name_cn = VALUES(name_cn),
    scope = VALUES(scope),
    last_update_time = NOW();

-- 事件权限
INSERT INTO openplatform_v2_permission_t 
(id, name_cn, name_en, scope, resource_type, resource_id, category_id, status, create_time, last_update_time, create_by, last_update_by)
VALUES
(2000, '消息接收权限', 'Message Received Permission', 'event:im:message-received:test', 'event', 200, 1, 1, NOW(), NOW(), 'system', 'system'),
(2001, '待审事件权限-测试用', 'Pending Event Permission For Test', 'event:test:pending', 'event', 201, 4, 1, NOW(), NOW(), 'system', 'system')
ON DUPLICATE KEY UPDATE 
    name_cn = VALUES(name_cn),
    scope = VALUES(scope),
    last_update_time = NOW();

-- 回调权限
INSERT INTO openplatform_v2_permission_t 
(id, name_cn, name_en, scope, resource_type, resource_id, category_id, status, create_time, last_update_time, create_by, last_update_by)
VALUES
(3000, '审批完成回调权限', 'Approval Completed Callback Permission', 'callback:approval:completed:test', 'callback', 300, 2, 1, NOW(), NOW(), 'system', 'system'),
(3001, '待审回调权限-测试用', 'Pending Callback Permission For Test', 'callback:test:pending', 'callback', 301, 4, 1, NOW(), NOW(), 'system', 'system')
ON DUPLICATE KEY UPDATE 
    name_cn = VALUES(name_cn),
    scope = VALUES(scope),
    last_update_time = NOW();

-- =====================================================
-- 8. 初始化订阅数据（使用数值应用ID=10）
-- =====================================================

-- API订阅（待审状态，用于撤回测试 TC-API-PERM-004）
INSERT INTO openplatform_v2_subscription_t 
(id, app_id, permission_id, status, channel_type, channel_address, auth_type, create_time, last_update_time, create_by, last_update_by, approved_at, approved_by)
VALUES
(400, 10, 1000, 0, 1, 'http://localhost/webhook', 0, NOW(), NOW(), 'test_user', 'test_user', NULL, NULL)
ON DUPLICATE KEY UPDATE 
    status = VALUES(status),
    last_update_time = NOW();

-- 事件订阅（已授权状态，用于配置测试 TC-EVENT-PERM-004）
INSERT INTO openplatform_v2_subscription_t 
(id, app_id, permission_id, status, channel_type, channel_address, auth_type, create_time, last_update_time, create_by, last_update_by, approved_at, approved_by)
VALUES
(401, 10, 2000, 1, 1, 'http://localhost/webhook/events', 0, NOW(), NOW(), 'test_user', 'test_user', NOW(), 'admin')
ON DUPLICATE KEY UPDATE 
    status = VALUES(status),
    last_update_time = NOW();

-- 事件订阅（待审状态，用于撤回测试 TC-EVENT-PERM-005）
INSERT INTO openplatform_v2_subscription_t 
(id, app_id, permission_id, status, channel_type, channel_address, auth_type, create_time, last_update_time, create_by, last_update_by, approved_at, approved_by)
VALUES
(402, 10, 2001, 0, 1, 'http://localhost/webhook', 0, NOW(), NOW(), 'test_user', 'test_user', NULL, NULL)
ON DUPLICATE KEY UPDATE 
    status = VALUES(status),
    last_update_time = NOW();

-- 回调订阅（已授权状态，用于配置测试 TC-CALLBACK-PERM-004）
INSERT INTO openplatform_v2_subscription_t 
(id, app_id, permission_id, status, channel_type, channel_address, auth_type, create_time, last_update_time, create_by, last_update_by, approved_at, approved_by)
VALUES
(403, 10, 3000, 1, 0, 'http://localhost/webhook/callbacks', 0, NOW(), NOW(), 'test_user', 'test_user', NOW(), 'admin')
ON DUPLICATE KEY UPDATE 
    status = VALUES(status),
    last_update_time = NOW();

-- 回调订阅（待审状态，用于撤回测试 TC-CALLBACK-PERM-005）
INSERT INTO openplatform_v2_subscription_t 
(id, app_id, permission_id, status, channel_type, channel_address, auth_type, create_time, last_update_time, create_by, last_update_by, approved_at, approved_by)
VALUES
(404, 10, 3001, 0, 0, 'http://localhost/webhook', 0, NOW(), NOW(), 'test_user', 'test_user', NULL, NULL)
ON DUPLICATE KEY UPDATE 
    status = VALUES(status),
    last_update_time = NOW();

-- =====================================================
-- 9. 初始化审批流程数据
-- =====================================================

-- 默认审批流程（如果不存在则创建）
INSERT INTO openplatform_v2_approval_flow_t 
(id, name_cn, name_en, code, description_cn, description_en, is_default, nodes, status, create_time, last_update_time, create_by, last_update_by)
VALUES
(1, '默认审批流程', 'Default Approval Flow', 'default', '默认审批流程', 'Default Approval Flow', 1, 
 '[{"type":"approver","userId":"admin","userName":"管理员","order":1}]', 
 1, NOW(), NOW(), 'system', 'system'),
(2, 'API注册审批流程', 'API Register Approval Flow', 'api_register', 'API注册审批流程', 'API Register Approval Flow', 0, 
 '[{"type":"approver","userId":"admin","userName":"管理员","order":1}]', 
 1, NOW(), NOW(), 'system', 'system')
ON DUPLICATE KEY UPDATE 
    nodes = VALUES(nodes),
    name_cn = VALUES(name_cn),
    last_update_time = NOW();

-- =====================================================
-- 10. 初始化审批记录数据
-- =====================================================

-- 待审状态的审批记录（用于审批操作测试）
-- 每个审批操作使用不同的记录，避免状态冲突
INSERT INTO openplatform_v2_approval_record_t 
(id, flow_id, business_type, business_id, applicant_id, applicant_name, status, current_node, create_time, last_update_time, create_by, last_update_by, completed_at)
VALUES
-- 用于同意审批测试 TC-APPROVAL-007
(500, 1, 'api_register', 102, 'test_user', '测试用户', 0, 0, NOW(), NOW(), 'test_user', 'test_user', NULL),
-- 用于驳回审批测试 TC-APPROVAL-008
(501, 1, 'api_register', 102, 'test_user', '测试用户', 0, 0, NOW(), NOW(), 'test_user', 'test_user', NULL),
-- 用于撤销审批测试 TC-APPROVAL-009
(502, 1, 'api_register', 102, 'test_user', '测试用户', 0, 0, NOW(), NOW(), 'test_user', 'test_user', NULL),
-- 用于批量同意审批测试 TC-APPROVAL-010
(503, 1, 'api_register', 102, 'test_user', '测试用户', 0, 0, NOW(), NOW(), 'test_user', 'test_user', NULL),
-- 用于批量驳回审批测试 TC-APPROVAL-011
(504, 1, 'api_register', 102, 'test_user', '测试用户', 0, 0, NOW(), NOW(), 'test_user', 'test_user', NULL)
ON DUPLICATE KEY UPDATE 
    status = VALUES(status),
    last_update_time = NOW();

-- =====================================================
-- 11. 初始化分类责任人数据
-- =====================================================

INSERT IGNORE INTO openplatform_v2_category_owner_t 
(id, category_id, user_id, user_name, create_time, last_update_time, create_by, last_update_by)
VALUES
(1, 1, 'user001', '张三', NOW(), NOW(), 'system', 'system'),
(2, 1, 'user002', '李四', NOW(), NOW(), 'system', 'system'),
(3, 2, 'user003', '王五', NOW(), NOW(), 'system', 'system');

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- 初始化完成
-- =====================================================
SELECT '测试数据初始化完成！' AS message;
SELECT '应用数据：' AS info, COUNT(*) AS count FROM applications WHERE id = '10';
SELECT 'API数据：' AS info, COUNT(*) AS count FROM openplatform_v2_api_t WHERE id >= 100 AND id < 200;
SELECT '事件数据：' AS info, COUNT(*) AS count FROM openplatform_v2_event_t WHERE id >= 200 AND id < 300;
SELECT '回调数据：' AS info, COUNT(*) AS count FROM openplatform_v2_callback_t WHERE id >= 300 AND id < 400;
SELECT '权限数据：' AS info, COUNT(*) AS count FROM openplatform_v2_permission_t WHERE id >= 1000 AND id < 4000;
SELECT '订阅数据：' AS info, COUNT(*) AS count FROM openplatform_v2_subscription_t WHERE id >= 400 AND id < 500;
