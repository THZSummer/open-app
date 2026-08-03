-- =====================================================================
-- V9: 能力开放平台早期 schema — create_early_lookup_schema
-- 来源: docs/app/*.sql (Navicat Premium Data Transfer 导出, 早期建表)
-- 说明:
--   - 仅提取表结构 DDL，不含数据（数据不进迁移脚本）
--   - CREATE TABLE 统一为 IF NOT EXISTS：开发库已有表时安全跳过，
--     不影响既有数据；全新库正常建表
-- =====================================================================

CREATE TABLE IF NOT EXISTS `openplatform_lookup_classify_t` (
      `classify_id` bigint NOT NULL COMMENT '分类ID，主键',
      `classify_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类编码',
      `classify_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
      `path` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '路径，用于层级归类',
      `classify_desc` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分类描述',
      `status` tinyint NULL DEFAULT 1 COMMENT '状态：0=失效, 1=有效',
      `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人',
      `create_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
      `last_update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后更新人',
      `last_update_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
      PRIMARY KEY (`classify_id`) USING BTREE,
      UNIQUE INDEX `uk_code_path`(`classify_code` ASC, `path` ASC) USING BTREE,
      INDEX `idx_status`(`status` ASC) USING BTREE
);

CREATE TABLE IF NOT EXISTS `openplatform_lookup_item_t` (
      `item_id` bigint NOT NULL COMMENT '项ID，主键',
      `classify_id` bigint NOT NULL COMMENT '分类ID，外键',
      `item_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '项编码',
      `item_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '项名称',
      `item_value` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '项值',
      `item_index` int NULL DEFAULT 0 COMMENT '排序序号',
      `item_desc` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '项描述',
      `item_attr1` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '扩展属性1',
      `item_attr2` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '扩展属性2',
      `item_attr3` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '扩展属性3',
      `item_attr4` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '扩展属性4',
      `item_attr5` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '扩展属性5',
      `item_attr6` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '扩展属性6',
      `status` tinyint NULL DEFAULT 1 COMMENT '状态：0=失效, 1=有效',
      `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人',
      `create_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
      `last_update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后更新人',
      `last_update_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
      PRIMARY KEY (`item_id`) USING BTREE,
      UNIQUE INDEX `uk_classify_code`(`classify_id` ASC, `item_code` ASC) USING BTREE,
      INDEX `idx_classify_id`(`classify_id` ASC) USING BTREE,
      INDEX `idx_status`(`status` ASC) USING BTREE,
      INDEX `idx_item_index`(`item_index` ASC) USING BTREE
);
