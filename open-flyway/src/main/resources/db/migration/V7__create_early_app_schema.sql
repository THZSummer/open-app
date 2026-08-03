-- =====================================================================
-- V7: 能力开放平台早期 schema — create_early_app_schema
-- 来源: docs/app/*.sql (Navicat Premium Data Transfer 导出, 早期建表)
-- 说明:
--   - 仅提取表结构 DDL，不含数据（数据不进迁移脚本）
--   - CREATE TABLE 统一为 IF NOT EXISTS：开发库已有表时安全跳过，
--     不影响既有数据；全新库正常建表
-- =====================================================================

CREATE TABLE IF NOT EXISTS `openplatform_app_t` (
      `id` bigint NOT NULL COMMENT '主键',
      `app_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '应用ID',
      `tenant_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '租户id',
      `icon_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '图标id',
      `app_name_cn` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '应用中文名',
      `app_name_en` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '应用英文名',
      `app_desc_cn` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '应用中文描述',
      `app_desc_en` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '应用英文描述',
      `app_type` tinyint(1) NULL DEFAULT 0 COMMENT '应用类型：0-个人应用 1-业务应用',
      `app_sub_type` tinyint NULL DEFAULT NULL COMMENT '应用子类型：0-存量个人应用 1-技能 2-个人助理 3-业务助理',
      `status` tinyint NULL DEFAULT 1 COMMENT '状态：0=失效, 1=有效',
      `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人',
      `create_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
      `last_update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后更新人',
      `last_update_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
      PRIMARY KEY (`id`) USING BTREE,
      UNIQUE INDEX `uniq_app_id`(`app_id` ASC) USING BTREE,
      UNIQUE INDEX `uniq_name_cn`(`app_name_cn` ASC) USING BTREE,
      UNIQUE INDEX `uniq_name_en`(`app_name_en` ASC) USING BTREE
);

CREATE TABLE IF NOT EXISTS `openplatform_app_p_t` (
      `id` bigint NOT NULL COMMENT '主键',
      `parent_id` bigint NOT NULL COMMENT '应用主键ID',
      `property_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '属性名',
      `property_value` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '属性值',
      `tenant_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '租户id',
      `status` tinyint NULL DEFAULT 1 COMMENT '状态：0=失效, 1=有效',
      `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人',
      `create_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
      `last_update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后更新人',
      `last_update_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
      PRIMARY KEY (`id`) USING BTREE,
      INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE
);

CREATE TABLE IF NOT EXISTS `openplatform_app_identity_t` (
      `id` bigint NOT NULL COMMENT '主键',
      `app_id` bigint NOT NULL COMMENT '应用主键id',
      `public_key` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'pk',
      `private_key` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '私钥',
      `key_version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '秘钥对版本,生成时yyyyMMddHHmmssSSS',
      `kit_version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '秘钥对生成算法套件版本',
      `ak` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'ak',
      `tenant_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '租户id',
      `status` tinyint NULL DEFAULT 1 COMMENT '状态：0=失效, 1=有效',
      `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人',
      `create_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
      `last_update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后更新人',
      `last_update_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
      PRIMARY KEY (`id`) USING BTREE,
      INDEX `idx_appid_keyversion_status`(`app_id` ASC, `key_version` ASC, `status` ASC) USING BTREE,
      INDEX `idx_ak`(`ak` ASC) USING BTREE
);

CREATE TABLE IF NOT EXISTS `openplatform_app_member_t` (
      `id` bigint NOT NULL COMMENT '主键',
      `tenant_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'default',
      `app_id` bigint NOT NULL COMMENT '应用主键ID',
      `member_name_cn` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '成员中文名',
      `member_name_en` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '成员英文名',
      `account_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '成员账号id',
      `member_type` tinyint(1) NULL DEFAULT 0 COMMENT '成员类型: 0:开发者 1：owner 2:管理员',
      `status` tinyint NULL DEFAULT 1 COMMENT '状态：0=失效, 1=有效',
      `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人',
      `create_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
      `last_update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后更新人',
      `last_update_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
      PRIMARY KEY (`id`) USING BTREE,
      INDEX `idx_app_id`(`app_id` ASC) USING BTREE
);

CREATE TABLE IF NOT EXISTS `openplatform_app_version_t` (
      `id` bigint NOT NULL COMMENT '主键',
      `app_id` bigint NOT NULL COMMENT '应用主键id',
      `version_desc_cn` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '版本中文描述',
      `version_desc_en` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '版本英文描述',
      `version_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '版本号',
      `tenant_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '租户id',
      `status` tinyint NULL DEFAULT 1 COMMENT '状态：0=失效, 1=有效',
      `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人',
      `create_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
      `last_update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后更新人',
      `last_update_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
      PRIMARY KEY (`id`) USING BTREE,
      INDEX `idx_app_id`(`app_id` ASC) USING BTREE
);

CREATE TABLE IF NOT EXISTS `openplatform_app_version_p_t` (
      `id` bigint NOT NULL COMMENT '主键',
      `parent_id` bigint NOT NULL COMMENT '版本主键id',
      `property_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '属性名',
      `property_value` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '属性值',
      `tenant_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '租户id',
      `status` tinyint NULL DEFAULT 1 COMMENT '状态：0=失效, 1=有效',
      `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人',
      `create_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
      `last_update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后更新人',
      `last_update_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
      PRIMARY KEY (`id`) USING BTREE,
      INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE
);

CREATE TABLE IF NOT EXISTS `openplatform_app_ability_relation_t` (
      `id` bigint NOT NULL COMMENT '主键',
      `app_id` bigint NOT NULL COMMENT '应用主键id',
      `ability_id` bigint NOT NULL COMMENT '能力主键id',
      `ability_type` tinyint(1) NOT NULL DEFAULT 0 COMMENT '能力类型 1-群置顶 2-群通知 3-链接增强 4-点对点通知 5-we码 6-应用入群通知 7-助手广场卡片',
      `tenant_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '租户id',
      `status` tinyint NULL DEFAULT 1 COMMENT '状态：0=失效, 1=有效',
      `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人',
      `create_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
      `last_update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后更新人',
      `last_update_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
      PRIMARY KEY (`id`) USING BTREE,
      UNIQUE INDEX `uniq_app_ability_id`(`app_id` ASC, `ability_id` ASC) USING BTREE,
      INDEX `idx_app_id`(`app_id` ASC) USING BTREE
);
