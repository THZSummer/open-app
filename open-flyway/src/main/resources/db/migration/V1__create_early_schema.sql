-- =====================================================================
-- V1: 能力开放平台早期 schema（docs/app 早期建表合并）
-- 来源: docs/app/*.sql (Navicat Premium Data Transfer 导出)
-- 说明:
--   - 16 张早期表合并为一个迁移文件（应用域 7 + 能力域 2 + 数据字典 2 + 运维域 5）
--   - 仅提取表结构 DDL，不含数据
--   - CREATE TABLE 统一 IF NOT EXISTS：开发库已有表时安全跳过
--   - collation 已适配 5.7（utf8mb4_0900_ai_ci → utf8mb4_unicode_ci）
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
