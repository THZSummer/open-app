-- =====================================================
-- 能力开放平台测试数据清理脚本
-- 用于测试环境数据重置，清理所有测试数据
-- =====================================================

-- 禁用外键检查（避免删除顺序问题）
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 1. 清理订阅数据
-- =====================================================
DELETE FROM openplatform_v2_subscription_t WHERE id >= 300 AND id < 400;

-- =====================================================
-- 2. 清理审批数据
-- =====================================================
DELETE FROM openplatform_v2_approval_log_t WHERE record_id >= 500 AND record_id < 600;
DELETE FROM openplatform_v2_approval_record_t WHERE id >= 500 AND id < 600;
DELETE FROM openplatform_v2_approval_flow_t WHERE id >= 1 AND id < 100;

-- =====================================================
-- 3. 清理权限数据
-- =====================================================
DELETE FROM openplatform_v2_permission_p_t WHERE permission_id >= 1000 AND permission_id < 1100;
DELETE FROM openplatform_v2_permission_t WHERE id >= 1000 AND id < 1100;

-- =====================================================
-- 4. 清理资源数据（API/事件/回调）
-- =====================================================
DELETE FROM openplatform_v2_api_p_t WHERE parent_id >= 100 AND parent_id < 200;
DELETE FROM openplatform_v2_api_t WHERE id >= 100 AND id < 200;

DELETE FROM openplatform_v2_event_p_t WHERE parent_id >= 200 AND parent_id < 300;
DELETE FROM openplatform_v2_event_t WHERE id >= 200 AND id < 300;

DELETE FROM openplatform_v2_callback_p_t WHERE parent_id >= 300 AND parent_id < 400;
DELETE FROM openplatform_v2_callback_t WHERE id >= 300 AND id < 400;

-- =====================================================
-- 5. 清理分类数据
-- =====================================================
DELETE FROM openplatform_v2_category_owner_t WHERE category_id >= 1 AND category_id < 100;
DELETE FROM openplatform_v2_category_t WHERE id >= 1 AND id < 100;

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- 清理完成
-- =====================================================
SELECT '测试数据清理完成！' AS message;
