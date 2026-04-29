-- ============================================
-- 测试数据插入脚本
-- ============================================

-- 1. 分类节点表测试数据（2个）
INSERT INTO `openplatform_module_node_t` 
(`id`, `node_name_cn`, `node_name_en`, `parent_Node_id`, `is_parent_node`, `is_leaf_node`, `order_num`, `auth_type`, `status`, `create_by`, `create_time`, `last_update_by`, `last_update_time`)
VALUES
(1001, '用户服务', 'user-service', 0, 1, 0, 1, 1, 1, 'admin', NOW(3), 'admin', NOW(3)),
(1002, '订单服务', 'order-service', 0, 1, 0, 2, 1, 1, 'admin', NOW(3), 'admin', NOW(3));

-- 2. 权限主表测试数据（2个）
INSERT INTO `openplatform_permission_t`
(`id`, `permission_name_cn`, `permission_name_en`, `module_id`, `scope_id`, `permisssion_type`, `is_approval_required`, `auth_type`, `status`, `create_by`, `create_time`, `last_update_by`, `last_update_time`)
VALUES
(2001, '用户信息查询权限', 'user-info-query', 1001, 'user:read', 'API', 0, 1, 1, 'admin', NOW(3), 'admin', NOW(3)),
(2002, '订单管理权限', 'order-management', 1002, 'order:manage', 'API', 1, 1, 1, 'admin', NOW(3), 'admin', NOW(3));

-- 3. API权限表测试数据（2个）
INSERT INTO `openplatform_permission_api_t`
(`id`, `api_name_cn`, `api_name_en`, `permission_id`, `api_path`, `api_method`, `auth_type`, `status`, `create_by`, `create_time`, `last_update_by`, `last_update_time`)
VALUES
(3001, '获取用户信息', 'get-user-info', 2001, '/api/v1/user/info', 'GET', 1, 1, 'admin', NOW(3), 'admin', NOW(3)),
(3002, '创建订单', 'create-order', 2002, '/api/v1/order/create', 'POST', 1, 1, 'admin', NOW(3), 'admin', NOW(3));

-- 4. 事件表测试数据（1个）
INSERT INTO `openplatform_event_t`
(`id`, `event_name_cn`, `event_name_en`, `module_id`, `topic`, `event_type`, `is_approval_required`, `status`, `create_by`, `create_time`, `last_update_by`, `last_update_time`)
VALUES
(4001, '订单创建事件', 'order-created', 1002, 'topic.order.created', 'BUSINESS', 0, 1, 'admin', NOW(3), 'admin', NOW(3));

-- 5. 应用属性表测试数据（示例数据，用于获取通道配置）
INSERT INTO `openplatform_app_p_t`
(`id`, `parent_id`, `property_name`, `property_value`, `status`, `create_by`, `create_time`, `last_update_by`, `last_update_time`)
VALUES
(5001, 10001, 'channel_type', 'HTTP', 1, 'admin', NOW(3), 'admin', NOW(3)),
(5002, 10001, 'channel_timeout', '30000', 1, 'admin', NOW(3), 'admin', NOW(3));

-- 6. 订阅关系表测试数据（3个：2个API订阅 + 1个事件订阅）
INSERT INTO `openplatform_app_permission_t`
(`id`, `app_id`, `permission_id`, `tenant_id`, `permisssion_type`, `status`, `create_by`, `create_time`, `last_update_by`, `last_update_time`)
VALUES
(6001, 10001, 2001, 'tenant-001', '0', 1, 'user001', NOW(3), 'user001', NOW(3)),
(6002, 10001, 2002, 'tenant-001', '0', 0, 'user001', NOW(3), 'user001', NOW(3)),
(6003, 10001, 4001, 'tenant-001', '1', 1, 'user001', NOW(3), 'user001', NOW(3));

-- 7. 审批流程表测试数据（2个）
INSERT INTO `openplatform_eflow_t`
(`eflow_id`, `eflow_type`, `eflow_status`, `eflow_submit_user`, `eflow_submit_message`, `eflow_audit_user`, `eflow_audit_message`, `resource_type`, `resource_id`, `resource_info`, `resource_delta`, `tenant_id`, `status`, `create_by`, `create_time`, `last_update_by`, `last_update_time`)
VALUES
(7001, 'PERMISSION_APPLY', 1, 'user001', '申请订单管理权限', 'admin', '审批通过', 'PERMISSION', 2002, '{"permission_id":2002,"permission_name":"订单管理权限"}', NULL, 'tenant-001', 1, 'user001', NOW(3), 'admin', NOW(3)),
(7002, 'PERMISSION_APPLY', 0, 'user002', '申请用户信息查询权限', NULL, NULL, 'PERMISSION', 2001, '{"permission_id":2001,"permission_name":"用户信息查询权限"}', NULL, 'tenant-002', 1, 'user002', NOW(3), 'user002', NOW(3));

-- 8. 审批日志表测试数据（3个）
INSERT INTO `openplatform_eflow_log_t`
(`eflow_log_id`, `eflow_log_trace_id`, `eflow_log_type`, `eflow_log_user`, `eflow_log_message`, `status`, `create_by`, `create_time`, `last_update_by`, `last_update_time`)
VALUES
(8001, 7001, 'SUBMIT', 'user001', '提交权限申请', 1, 'user001', NOW(3), 'user001', NOW(3)),
(8002, 7001, 'AUDIT', 'admin', '审批通过', 1, 'admin', NOW(3), 'admin', NOW(3)),
(8003, 7002, 'SUBMIT', 'user002', '提交权限申请', 1, 'user002', NOW(3), 'user002', NOW(3));
