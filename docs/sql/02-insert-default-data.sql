-- ============================================================================
-- 能力开放平台默认数据初始化脚本
-- 版本: v1.0
-- 创建日期: 2026-04-23
-- 说明: 插入生产环境必需的默认数据（分类、审批流程）
-- 执行顺序: 02（默认数据）
-- 包含数据:
--   - 分类数据（分类树结构）
--   - 审批流程数据（默认审批流程）
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- 一、分类表数据 (openplatform_v2_category_t)
-- 说明: 包含系统默认的分类树结构数据
-- 记录数: 13条
-- ============================================================================

-- 清空表（如果需要重新初始化）
-- TRUNCATE TABLE `openplatform_v2_category_t`;

-- 根分类：API-业务应用-应用身份-SOA
INSERT INTO `openplatform_v2_category_t` (`id`, `category_alias`, `name_cn`, `name_en`, `parent_id`, `path`, `sort_order`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(305650853237227520, 'api_business_app_soa', 'API-业务应用-应用身份-SOA', 'appsoa', NULL, '/305650853237227520/', 1, 1, NOW(3), NOW(3), 'system', 'system');

-- 根分类：API-业务应用-应用身份-APIG
INSERT INTO `openplatform_v2_category_t` (`id`, `category_alias`, `name_cn`, `name_en`, `parent_id`, `path`, `sort_order`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(305650973508894720, 'api_business_app_apig', 'API-业务应用-应用身份-APIG', 'appapig', NULL, '/305650973508894720/', 2, 1, NOW(3), NOW(3), 'system', 'system');

-- 根分类：API-业务应用-用户身份-SOA
INSERT INTO `openplatform_v2_category_t` (`id`, `category_alias`, `name_cn`, `name_en`, `parent_id`, `path`, `sort_order`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(305664724417118208, 'api_business_user_soa', 'API-业务应用-用户身份-SOA', 'api_business_user_soa', NULL, '/305664724417118208/', 3, 1, NOW(3), NOW(3), 'system', 'system');

-- 根分类：API-业务应用-用户身份-APIG
INSERT INTO `openplatform_v2_category_t` (`id`, `category_alias`, `name_cn`, `name_en`, `parent_id`, `path`, `sort_order`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(305664845166936064, 'api_business_user_apig', 'API-业务应用-用户身份-APIG', 'api_business_user_apig', NULL, '/305664845166936064/', 4, 1, NOW(3), NOW(3), 'system', 'system');

-- 根分类：API-个人应用-用户身份-AKSK
INSERT INTO `openplatform_v2_category_t` (`id`, `category_alias`, `name_cn`, `name_en`, `parent_id`, `path`, `sort_order`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(305654654555914240, 'api_personal_user_aksk', 'API-个人应用-用户身份-AKSK', 'persional_aksk', NULL, '/305654654555914240/', 5, 1, NOW(3), NOW(3), 'system', 'system');

-- 根分类：回调
INSERT INTO `openplatform_v2_category_t` (`id`, `category_alias`, `name_cn`, `name_en`, `parent_id`, `path`, `sort_order`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(305654385424203776, 'callback', '回调', 'callback', NULL, '/305654385424203776/', 6, 1, NOW(3), NOW(3), 'system', 'system');

-- 根分类：事件
INSERT INTO `openplatform_v2_category_t` (`id`, `category_alias`, `name_cn`, `name_en`, `parent_id`, `path`, `sort_order`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(305653292027871232, 'event', '事件', 'event', NULL, '/305653292027871232/', 9, 1, NOW(3), NOW(3), 'system', 'system');

-- 子分类：API-业务应用-应用身份-SOA -> 消息
INSERT INTO `openplatform_v2_category_t` (`id`, `category_alias`, `name_cn`, `name_en`, `parent_id`, `path`, `sort_order`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(305652258039660544, NULL, '消息', 'IM', 305650853237227520, '/305650853237227520/305652258039660544/', 0, 1, NOW(3), NOW(3), 'system', 'system');

-- 子分类：API-业务应用-应用身份-SOA -> 会议
INSERT INTO `openplatform_v2_category_t` (`id`, `category_alias`, `name_cn`, `name_en`, `parent_id`, `path`, `sort_order`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(305652314155253760, NULL, '会议', 'Metting', 305650853237227520, '/305650853237227520/305652314155253760/', 0, 1, NOW(3), NOW(3), 'system', 'system');

-- 子分类：API-业务应用-应用身份-APIG -> 云盘
INSERT INTO `openplatform_v2_category_t` (`id`, `category_alias`, `name_cn`, `name_en`, `parent_id`, `path`, `sort_order`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(305652407616929792, NULL, '云盘', 'CloudBox', 305650973508894720, '/305650973508894720/305652407616929792/', 0, 1, NOW(3), NOW(3), 'system', 'system');

-- 子分类：API-个人应用-用户身份-AKSK -> 文档
INSERT INTO `openplatform_v2_category_t` (`id`, `category_alias`, `name_cn`, `name_en`, `parent_id`, `path`, `sort_order`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(305654770410979328, NULL, '文档', 'Doc', 305654654555914240, '/305654654555914240/305654770410979328/', 0, 1, NOW(3), NOW(3), 'system', 'system');

-- 子分类：API-业务应用-用户身份-SOA -> 日历
INSERT INTO `openplatform_v2_category_t` (`id`, `category_alias`, `name_cn`, `name_en`, `parent_id`, `path`, `sort_order`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(305664882441424896, NULL, '日历', 'Calendar', 305664724417118208, '/305664724417118208/305664882441424896/', 0, 1, NOW(3), NOW(3), 'system', 'system');

-- 子分类：API-业务应用-用户身份-APIG -> 待办
INSERT INTO `openplatform_v2_category_t` (`id`, `category_alias`, `name_cn`, `name_en`, `parent_id`, `path`, `sort_order`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(305664906697940992, NULL, '待办', 'Todo', 305664845166936064, '/305664845166936064/305664906697940992/', 0, 1, NOW(3), NOW(3), 'system', 'system');

-- ============================================================================
-- 二、审批流程模板表数据 (openplatform_v2_approval_flow_t)
-- 说明: 审批流程模板定义（生产环境必需）
-- ============================================================================

-- 1. 默认审批流
INSERT INTO `openplatform_v2_approval_flow_t` (`id`, `name_cn`, `name_en`, `code`, `description_cn`, `description_en`, `is_default`, `nodes`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(1, '默认审批流', 'Default Approval Flow', 'default', '系统默认审批流程，适用于未配置审批流的场景', 'System default approval flow, applicable to scenarios without configured approval flow', 1, '[{"type":"approver","userId":"admin","userName":"系统管理员","order":1}]', 1, NOW(3), NOW(3), 'system', 'system');

-- 2. API注册审批流（二级审批）
INSERT INTO `openplatform_v2_approval_flow_t` (`id`, `name_cn`, `name_en`, `code`, `description_cn`, `description_en`, `is_default`, `nodes`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(2, 'API注册审批流', 'API Registration Approval Flow', 'api_register', 'API资源注册审批流程，需要分类责任人和平台管理员二级审批', 'API resource registration approval flow, requires two-level approval from category owner and platform admin', 0, '[{"type":"approver","userId":"category_owner","userName":"分类责任人","order":1},{"type":"approver","userId":"admin","userName":"平台管理员","order":2}]', 1, NOW(3), NOW(3), 'system', 'system');

-- 3. 权限申请审批流（三级审批）
INSERT INTO `openplatform_v2_approval_flow_t` (`id`, `name_cn`, `name_en`, `code`, `description_cn`, `description_en`, `is_default`, `nodes`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(3, '权限申请审批流', 'Permission Apply Approval Flow', 'permission_apply', '权限申请审批流程，需要应用管理员、分类责任人和安全管理员三级审批', 'Permission application approval flow, requires three-level approval', 0, '[{"type":"approver","userId":"app_manager","userName":"应用管理员","order":1},{"type":"approver","userId":"category_owner","userName":"分类责任人","order":2},{"type":"approver","userId":"security_admin","userName":"安全管理员","order":3}]', 1, NOW(3), NOW(3), 'system', 'system');

-- 4. 事件注册审批流
INSERT INTO `openplatform_v2_approval_flow_t` (`id`, `name_cn`, `name_en`, `code`, `description_cn`, `description_en`, `is_default`, `nodes`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(4, '事件注册审批流', 'Event Registration Approval Flow', 'event_register', '事件资源注册审批流程', 'Event resource registration approval flow', 0, '[{"type":"approver","userId":"category_owner","userName":"分类责任人","order":1},{"type":"approver","userId":"admin","userName":"平台管理员","order":2}]', 1, NOW(3), NOW(3), 'system', 'system');

-- 5. 快速审批流（单级）
INSERT INTO `openplatform_v2_approval_flow_t` (`id`, `name_cn`, `name_en`, `code`, `description_cn`, `description_en`, `is_default`, `nodes`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(5, '快速审批流', 'Quick Approval Flow', 'quick', '快速审批流程，适用于低风险操作', 'Quick approval flow for low-risk operations', 0, '[{"type":"approver","userId":"auto_approver","userName":"自动审批","order":1}]', 1, NOW(3), NOW(3), 'system', 'system');

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 数据初始化完成
-- 
-- 数据统计：
-- - 分类表 (category_t): 13 条
-- - 审批流程模板表 (approval_flow_t): 5 条
-- ============================================================================