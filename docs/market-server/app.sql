CREATE TABLE `openplatform_app_t` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `app_id` varchar(100) NOT NULL COMMENT '应用ID',
  `tenant_id` varchar(64) NOT NULL DEFAULT '' COMMENT '租户id',
  `icon_id` varchar(64) NOT NULL DEFAULT '' COMMENT '图标id',
  `app_name_cn` varchar(255) NOT NULL COMMENT '应用中文名',
	`app_name_en` varchar(255) NOT NULL COMMENT '应用英文名',
	`app_desc_cn` varchar(2000) NOT NULL DEFAULT '' COMMENT '应用中文描述',
	`app_desc_en` varchar(2000) NOT NULL DEFAULT '' COMMENT '应用英文描述',
	`app_type` tinyint(1) DEFAULT '0' COMMENT '应用类型：0-个人应用 1-业务应用',
	`app_sub_type` tinyint(10) DEFAULT NULL COMMENT '应用子类型：0-存量个人应用 1-技能 2-个人助理 3-业务助理',
  `status` tinyint DEFAULT '1' COMMENT '状态：0=失效, 1=有效',
  `create_by` varchar(100) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `last_update_by` varchar(100) DEFAULT NULL COMMENT '最后更新人',
  `last_update_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_app_id` (`app_id`) USING BTREE,
  UNIQUE KEY `uniq_name_cn` (`app_name_cn`) USING BTREE,
	UNIQUE KEY `uniq_name_en` (`app_name_en`) USING BTREE
) ENGINE=InnoDB COMMENT='应用表';

CREATE TABLE `openplatform_app_p_t` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `parent_id` bigint(20) NOT NULL COMMENT '应用主键ID',
  `property_name` varchar(255) NOT NULL COMMENT '属性名',
  `property_value` varchar(2000) NOT NULL DEFAULT '' COMMENT '属性值',
  `tenant_id` varchar(64) NOT NULL DEFAULT '' COMMENT '租户id',
  `status` tinyint DEFAULT '1' COMMENT '状态：0=失效, 1=有效',
  `create_by` varchar(100) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `last_update_by` varchar(100) DEFAULT NULL COMMENT '最后更新人',
  `last_update_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`) USING BTREE
) ENGINE=InnoDB COMMENT='应用属性表';

CREATE TABLE `openplatform_app_member_t` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `app_id` bigint(20) NOT NULL COMMENT '应用主键ID',
  `member_name_cn` varchar(255) NOT NULL COMMENT '成员中文名',
	`member_name_en` varchar(255) NOT NULL COMMENT '成员英文名',
  `account_id` varchar(255) NOT NULL DEFAULT '' COMMENT '成员账号id',
  `member_type` TINYINT(1) DEFAULT '0' COMMENT '成员类型: 0:开发者 1：owner 2:管理员',
  `status` tinyint DEFAULT '1' COMMENT '状态：0=失效, 1=有效',
  `create_by` varchar(100) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `last_update_by` varchar(100) DEFAULT NULL COMMENT '最后更新人',
  `last_update_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_app_id` (`app_id`) USING BTREE
) ENGINE=InnoDB COMMENT='应用成员表';

CREATE TABLE `openplatform_ability_t` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `ability_name_cn` varchar(255) NOT NULL COMMENT '能力中文名',
  `ability_name_en` varchar(255) NOT NULL COMMENT '能力英文名',
	`ability_desc_cn` varchar(2000) NOT NULL DEFAULT '' COMMENT '能力中文描述',
	`ability_desc_en` varchar(2000) NOT NULL DEFAULT '' COMMENT '能力英文描述',
  `ability_type` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '能力类型 1-群置顶 2-群通知 3-链接增强 4-点对点通知 5-we码 6-应用入群通知 7-助手广场卡片',
  `order_num` int(11) NOT NULL COMMENT '序号',
  `status` tinyint DEFAULT '1' COMMENT '状态：0=失效, 1=有效',
  `create_by` varchar(100) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `last_update_by` varchar(100) DEFAULT NULL COMMENT '最后更新人',
  `last_update_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ability_type` (`ability_type`) USING BTREE
) ENGINE=InnoDB COMMENT='能力表';

CREATE TABLE `openplatform_ability_p_t` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `parent_id` bigint(20) NOT NULL COMMENT '能力id',
  `property_name` varchar(255) NOT NULL COMMENT '属性名',
	`property_value` varchar(2000) NOT NULL DEFAULT '' COMMENT '属性值',
  `status` tinyint DEFAULT '1' COMMENT '状态：0=失效, 1=有效',
  `create_by` varchar(100) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `last_update_by` varchar(100) DEFAULT NULL COMMENT '最后更新人',
  `last_update_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`) USING BTREE
) ENGINE=InnoDB COMMENT='能力属性表';


CREATE TABLE `openplatform_app_ability_relation_t` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `app_id` bigint(20) NOT NULL COMMENT '应用主键id',
  `ability_id` bigint(20) NOT NULL COMMENT '能力主键id',
	`ability_type` tinyint(1) NOT NULL DEFAULT '0' COMMENT '能力类型 1-群置顶 2-群通知 3-链接增强 4-点对点通知 5-we码 6-应用入群通知 7-助手广场卡片',
	`tenant_id` varchar(64) NOT NULL DEFAULT '' COMMENT '租户id',
  `status` tinyint DEFAULT '1' COMMENT '状态：0=失效, 1=有效',
  `create_by` varchar(100) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `last_update_by` varchar(100) DEFAULT NULL COMMENT '最后更新人',
  `last_update_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
	UNIQUE KEY `uniq_app_ability_id` (`app_id`,`ability_id`) USING BTREE,
  KEY `idx_app_id` (`app_id`) USING BTREE
) ENGINE=InnoDB COMMENT='应用能力关联表';

CREATE TABLE `openplatform_app_identity_t` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `app_id` bigint(20) NOT NULL COMMENT '应用主键id',
  `public_key` varchar(2000) DEFAULT NULL COMMENT 'pk',
	`private_key` varchar(2000) NOT NULL DEFAULT '' COMMENT '私钥',
	`key_version` varchar(50) NOT NULL DEFAULT '' COMMENT '秘钥对版本,生成时yyyyMMddHHmmssSSS',
	`kit_version` varchar(50) NOT NULL DEFAULT '' COMMENT '秘钥对生成算法套件版本',
	`ak` varchar(255) DEFAULT NULL COMMENT 'ak',
	`tenant_id` varchar(64) NOT NULL DEFAULT '' COMMENT '租户id',
  `status` tinyint DEFAULT '1' COMMENT '状态：0=失效, 1=有效',
  `create_by` varchar(100) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `last_update_by` varchar(100) DEFAULT NULL COMMENT '最后更新人',
  `last_update_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
	KEY `idx_appid_keyversion_status` (`app_id`,`key_version`,`status`) USING BTREE,
  KEY `idx_ak` (`ak`) USING BTREE
) ENGINE=InnoDB COMMENT='应用凭证表';


CREATE TABLE `openplatform_app_version_t` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `app_id` bigint(20) NOT NULL COMMENT '应用主键id',
  `version_desc_cn` varchar(2000) DEFAULT NULL COMMENT '版本中文描述',
	`version_desc_en` varchar(2000) DEFAULT NULL COMMENT '版本英文描述',
	`version_code` varchar(100) NOT NULL COMMENT '版本号',
	`tenant_id` varchar(64) NOT NULL DEFAULT '' COMMENT '租户id',
  `status` tinyint DEFAULT '1' COMMENT '状态：0=失效, 1=有效',
  `create_by` varchar(100) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `last_update_by` varchar(100) DEFAULT NULL COMMENT '最后更新人',
  `last_update_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
	KEY `idx_app_id` (`app_id`) USING BTREE
) ENGINE=InnoDB COMMENT='应用版本表';

CREATE TABLE `openplatform_app_version_p_t` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `parent_id` bigint(20) NOT NULL COMMENT '版本主键id',
  `property_name` varchar(255) DEFAULT NULL COMMENT '属性名',
	`property_value` varchar(2000) NOT NULL DEFAULT '' COMMENT '属性值',
	`tenant_id` varchar(64) NOT NULL DEFAULT '' COMMENT '租户id',
  `status` tinyint DEFAULT '1' COMMENT '状态：0=失效, 1=有效',
  `create_by` varchar(100) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `last_update_by` varchar(100) DEFAULT NULL COMMENT '最后更新人',
  `last_update_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
	KEY `idx_parent_id` (`parent_id`) USING BTREE
) ENGINE=InnoDB COMMENT='应用版本属性表';