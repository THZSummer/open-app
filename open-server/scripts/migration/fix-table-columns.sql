-- ============================================
-- 修复数据库表字段名拼写错误
-- ============================================

-- 修复订阅关系表字段名
ALTER TABLE openplatform_app_permission_t 
CHANGE COLUMN permisssion_type permission_type VARCHAR(20) COMMENT '0=API权限, 1=事件';

-- 修复权限主表字段名（如果存在）
ALTER TABLE openplatform_permission_t 
CHANGE COLUMN permisssion_type permission_type VARCHAR(20) COMMENT '权限类型';
