-- ============================================================================
-- 能力开放平台数据库清理脚本
-- 版本: v1.0
-- 创建日期: 2026-04-23
-- 说明: 删除所有表（开发调试用）
-- 执行顺序: 00（最先执行）
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 删除主表和关联表
DROP TABLE IF EXISTS `openplatform_v2_user_authorization_t`;
DROP TABLE IF EXISTS `openplatform_v2_approval_log_t`;
DROP TABLE IF EXISTS `openplatform_v2_approval_record_t`;
DROP TABLE IF EXISTS `openplatform_v2_approval_flow_t`;
DROP TABLE IF EXISTS `openplatform_v2_subscription_t`;
DROP TABLE IF EXISTS `openplatform_v2_permission_p_t`;
DROP TABLE IF EXISTS `openplatform_v2_permission_t`;
DROP TABLE IF EXISTS `openplatform_v2_callback_p_t`;
DROP TABLE IF EXISTS `openplatform_v2_callback_t`;
DROP TABLE IF EXISTS `openplatform_v2_event_p_t`;
DROP TABLE IF EXISTS `openplatform_v2_event_t`;
DROP TABLE IF EXISTS `openplatform_v2_api_p_t`;
DROP TABLE IF EXISTS `openplatform_v2_api_t`;
DROP TABLE IF EXISTS `openplatform_v2_category_owner_t`;
DROP TABLE IF EXISTS `openplatform_v2_category_t`;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 清理完成
-- 已删除 15 张表
-- ============================================================================