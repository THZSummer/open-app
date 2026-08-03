-- =====================================================================
-- V10: 能力开放平台早期 schema — create_early_misc_schema
-- 来源: docs/app/*.sql (Navicat Premium Data Transfer 导出, 早期建表)
-- 说明:
--   - 仅提取表结构 DDL，不含数据（数据不进迁移脚本）
--   - CREATE TABLE 统一为 IF NOT EXISTS：开发库已有表时安全跳过，
--     不影响既有数据；全新库正常建表
-- =====================================================================

CREATE TABLE IF NOT EXISTS `openplatform_operate_log_t` (
      `id` bigint NOT NULL COMMENT '主键',
      `app_id` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '应用ID',
      `operate_type` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '操作类型',
      `operate_object` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '操作对象',
      `operate_desc_cn` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL COMMENT '中文描述',
      `operate_desc_en` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL COMMENT '英文描述',
      `operate_user` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '操作人',
      `ip_address` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '操作人地址',
      `before_data` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL COMMENT '操作前数据',
      `after_data` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL COMMENT '操作后数据',
      `status` tinyint(1) NULL DEFAULT 1 COMMENT '0:失败 1:成功',
      `create_by` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '' COMMENT '创建人',
      `create_time` datetime(3) NOT NULL COMMENT '创建时间',
      `last_update_by` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '' COMMENT '最后更新人',
      `last_update_time` datetime(3) NOT NULL COMMENT '最后更新时间',
      PRIMARY KEY (`id`) USING BTREE,
      INDEX `idx_app_id_operate_object`(`app_id` ASC, `operate_object` ASC) USING BTREE
);

CREATE TABLE IF NOT EXISTS `openplatform_property_t` (
      `id` bigint NOT NULL COMMENT '主键',
      `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '编码',
      `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '名称',
      `value` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '值',
      `description` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '描述',
      `path` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '路径',
      `language` tinyint NOT NULL DEFAULT 1 COMMENT '语言: 1-中文 2-英文',
      `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 0-失效 1-有效',
      `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人',
      `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
      `last_update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后更新人',
      `last_update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
      PRIMARY KEY (`id`) USING BTREE,
      UNIQUE INDEX `idx_path_code`(`path` ASC, `code` ASC) USING BTREE,
      INDEX `idx_path_name`(`path` ASC, `name` ASC) USING BTREE,
      INDEX `idx_name`(`name` ASC) USING BTREE
);

CREATE TABLE IF NOT EXISTS `openplatform_file_t` (
      `id` bigint NOT NULL COMMENT '主键',
      `file_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件ID',
      `file_name` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '原始文件名',
      `file_path` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '文件存储路径（磁盘相对路径）',
      `url` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '文件访问URL',
      `biz_type` tinyint(1) NOT NULL DEFAULT 0 COMMENT '业务类型：1-图标 2-功能示意图',
      `file_size` bigint NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
      `content_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '文件MIME类型',
      `tenant_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '租户id',
      `status` tinyint NULL DEFAULT 1 COMMENT '状态：0=失效, 1=有效',
      `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人',
      `create_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
      `last_update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后更新人',
      `last_update_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
      PRIMARY KEY (`id`) USING BTREE,
      UNIQUE INDEX `uniq_file_id`(`file_id` ASC) USING BTREE
);

CREATE TABLE IF NOT EXISTS `openplatform_employee_t` (
      `id` bigint NOT NULL COMMENT '主键',
      `welink_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'WeLink账号ID即member表account_id',
      `w3_account` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'W3工号',
      `chinese_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '中文名',
      `english_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '英文名',
      `department` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '部门',
      `status` tinyint NULL DEFAULT 1 COMMENT '状态',
      `create_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3),
      `last_update_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
      PRIMARY KEY (`id`) USING BTREE,
      UNIQUE INDEX `uk_welink_id`(`welink_id` ASC) USING BTREE,
      INDEX `idx_w3_account`(`w3_account` ASC) USING BTREE
);

CREATE TABLE IF NOT EXISTS `openplatform_eamap_t` (
      `id` bigint NOT NULL AUTO_INCREMENT,
      `eamap_app_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
      `name_cn` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
      `name_en` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '',
      `owner_account_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
      `status` tinyint NULL DEFAULT 1,
      `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'system',
      `create_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3),
      `last_update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'system',
      `last_update_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
      PRIMARY KEY (`id`) USING BTREE
);
