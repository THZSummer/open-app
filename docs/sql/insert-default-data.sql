-- ============================================================================
-- 能力开放平台默认数据初始化脚本
-- 版本: v1.0
-- 创建日期: 2026-04-21
-- 说明: 插入默认审批流等基础数据
-- ============================================================================

SET NAMES utf8mb4;

-- ============================================================================
-- 1. 默认审批流模板
-- ============================================================================
-- 默认审批流（code='default'）
-- 审批节点配置说明：
-- - type: 节点类型（approver=审批人, role=角色, expression=表达式）
-- - userId: 审批人ID（当type=approver时必填）
-- - userName: 审批人姓名
-- - order: 节点顺序
INSERT INTO `openplatform_v2_approval_flow_t` (
    `id`,
    `name_cn`,
    `name_en`,
    `code`,
    `description_cn`,
    `description_en`,
    `is_default`,
    `nodes`,
    `status`,
    `create_time`,
    `last_update_time`,
    `create_by`,
    `last_update_by`
) VALUES (
    1,
    '默认审批流',
    'Default Approval Flow',
    'default',
    '系统默认审批流程，适用于未配置审批流的场景',
    'System default approval flow, applicable to scenarios without configured approval flow',
    1,
    '[{"type":"approver","userId":"admin","userName":"系统管理员","order":1}]',
    1,
    NOW(3),
    NOW(3),
    'system',
    'system'
);

-- ============================================================================
-- 2. 示例分类数据（可选）
-- ============================================================================
-- 根分类示例（A类应用权限树）
INSERT INTO `openplatform_v2_category_t` (
    `id`,
    `category_alias`,
    `name_cn`,
    `name_en`,
    `parent_id`,
    `path`,
    `sort_order`,
    `status`,
    `create_time`,
    `last_update_time`,
    `create_by`,
    `last_update_by`
) VALUES (
    1,
    'app_type_a',
    'A类应用权限',
    'App Type A Permissions',
    NULL,
    '/1/',
    1,
    1,
    NOW(3),
    NOW(3),
    'system',
    'system'
);

-- 根分类示例（B类应用权限树）
INSERT INTO `openplatform_v2_category_t` (
    `id`,
    `category_alias`,
    `name_cn`,
    `name_en`,
    `parent_id`,
    `path`,
    `sort_order`,
    `status`,
    `create_time`,
    `last_update_time`,
    `create_by`,
    `last_update_by`
) VALUES (
    2,
    'app_type_b',
    'B类应用权限',
    'App Type B Permissions',
    NULL,
    '/2/',
    2,
    1,
    NOW(3),
    NOW(3),
    'system',
    'system'
);

-- 根分类示例（个人AKSK权限树）
INSERT INTO `openplatform_v2_category_t` (
    `id`,
    `category_alias`,
    `name_cn`,
    `name_en`,
    `parent_id`,
    `path`,
    `sort_order`,
    `status`,
    `create_time`,
    `last_update_time`,
    `create_by`,
    `last_update_by`
) VALUES (
    3,
    'personal_aksk',
    '个人AKSK权限',
    'Personal AKSK Permissions',
    NULL,
    '/3/',
    3,
    1,
    NOW(3),
    NOW(3),
    'system',
    'system'
);

-- ============================================================================
-- 3. 示例子分类数据（可选）
-- ============================================================================
-- A类应用 - IM模块子分类
INSERT INTO `openplatform_v2_category_t` (
    `id`,
    `category_alias`,
    `name_cn`,
    `name_en`,
    `parent_id`,
    `path`,
    `sort_order`,
    `status`,
    `create_time`,
    `last_update_time`,
    `create_by`,
    `last_update_by`
) VALUES (
    11,
    NULL,
    'IM即时通讯',
    'IM Instant Messaging',
    1,
    '/1/11/',
    1,
    1,
    NOW(3),
    NOW(3),
    'system',
    'system'
);

-- A类应用 - 会议模块子分类
INSERT INTO `openplatform_v2_category_t` (
    `id`,
    `category_alias`,
    `name_cn`,
    `name_en`,
    `parent_id`,
    `path`,
    `sort_order`,
    `status`,
    `create_time`,
    `last_update_time`,
    `create_by`,
    `last_update_by`
) VALUES (
    12,
    NULL,
    '会议服务',
    'Meeting Service',
    1,
    '/1/12/',
    2,
    1,
    NOW(3),
    NOW(3),
    'system',
    'system'
);

-- ============================================================================
-- 初始化完成
-- 已插入数据：
-- - 1 条默认审批流记录（code='default'）
-- - 3 条根分类记录（A类/B类/AKSK）
-- - 2 条子分类记录（IM/会议）
-- ============================================================================
