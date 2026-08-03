-- =====================================================================
-- V8: 能力开放平台早期 schema — create_early_ability_schema
-- 来源: docs/app/*.sql (Navicat Premium Data Transfer 导出, 早期建表)
-- 说明:
--   - 仅提取表结构 DDL，不含数据（数据不进迁移脚本）
--   - CREATE TABLE 统一为 IF NOT EXISTS：开发库已有表时安全跳过，
--     不影响既有数据；全新库正常建表
-- =====================================================================

CREATE TABLE IF NOT EXISTS `openplatform_ability_t` (
      `id` bigint NOT NULL COMMENT '主键',
      `ability_name_cn` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '能力中文名',
      `ability_name_en` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '能力英文名',
      `ability_desc_cn` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '能力中文描述',
      `ability_desc_en` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '能力英文描述',
      `ability_type` tinyint(1) NOT NULL DEFAULT 0 COMMENT '能力类型 1-群置顶 2-群通知 3-链接增强 4-点对点通知 5-we码 6-应用入群通知 7-助手广场卡片',
      `order_num` int NOT NULL COMMENT '序号',
      `status` tinyint NULL DEFAULT 1 COMMENT '状态：0=失效, 1=有效',
      `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人',
      `create_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
      `last_update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后更新人',
      `last_update_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
      PRIMARY KEY (`id`) USING BTREE,
      INDEX `idx_ability_type`(`ability_type` ASC) USING BTREE
);

CREATE TABLE IF NOT EXISTS `openplatform_ability_p_t` (
      `id` bigint NOT NULL COMMENT '主键',
      `parent_id` bigint NOT NULL COMMENT '能力id',
      `property_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '属性名',
      `property_value` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '属性值',
      `status` tinyint NULL DEFAULT 1 COMMENT '状态：0=失效, 1=有效',
      `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人',
      `create_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
      `last_update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后更新人',
      `last_update_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
      PRIMARY KEY (`id`) USING BTREE,
      INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE
);
