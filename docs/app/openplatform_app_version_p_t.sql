/*
 Navicat Premium Data Transfer

 Source Server         : test
 Source Server Type    : MySQL
 Source Server Version : 80409 (8.4.9)
 Source Host           : localhost:3306
 Source Schema         : openapp

 Target Server Type    : MySQL
 Target Server Version : 80409 (8.4.9)
 File Encoding         : 65001

 Date: 18/06/2026 09:53:36
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for openplatform_app_version_p_t
-- ----------------------------
DROP TABLE IF EXISTS `openplatform_app_version_p_t`;
CREATE TABLE `openplatform_app_version_p_t`  (
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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '应用版本属性表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of openplatform_app_version_p_t
-- ----------------------------
INSERT INTO `openplatform_app_version_p_t` VALUES (324486947026239488, 324486946808135680, 'abilityIds', '', 'default', 1, 'system', '2026-06-14 09:55:16.849', 'system', '2026-06-14 09:55:16.849');
INSERT INTO `openplatform_app_version_p_t` VALUES (324649316180295680, 324649316100603904, 'abilityIds', '', 'default', 1, 'system', '2026-06-14 20:40:28.673', 'system', '2026-06-14 20:40:28.673');
INSERT INTO `openplatform_app_version_p_t` VALUES (324657224339161088, 324657224133640192, 'abilityIds', '1,2,4', 'default', 1, 'system', '2026-06-14 21:11:54.124', 'system', '2026-06-14 21:11:54.124');
INSERT INTO `openplatform_app_version_p_t` VALUES (324905383791427584, 324905383783038976, 'abilityIds', '', 'default', 1, 'system', '2026-06-15 13:37:59.948', 'system', '2026-06-15 13:37:59.948');
INSERT INTO `openplatform_app_version_p_t` VALUES (324905446571769856, 324905446563381248, 'abilityIds', '1', 'default', 1, 'system', '2026-06-15 13:38:14.916', 'system', '2026-06-15 13:38:14.916');
INSERT INTO `openplatform_app_version_p_t` VALUES (324905878207594496, 324905878144679936, 'abilityIds', '1', 'default', 1, 'system', '2026-06-15 13:39:57.826', 'system', '2026-06-15 13:39:57.826');
INSERT INTO `openplatform_app_version_p_t` VALUES (325027341199736832, 325027341178765312, 'abilityIds', '1', 'default', 1, 'system', '2026-06-15 21:42:36.860', 'system', '2026-06-15 21:42:36.860');
INSERT INTO `openplatform_app_version_p_t` VALUES (325030786417819648, 325030786396848128, 'abilityIds', '1', 'default', 1, 'system', '2026-06-15 21:56:18.264', 'system', '2026-06-15 21:56:18.264');
INSERT INTO `openplatform_app_version_p_t` VALUES (325558357035319296, 325558357018542080, 'abilityIds', '1', 'default', 1, 'system', '2026-06-17 08:52:40.901', 'system', '2026-06-17 08:52:40.901');
INSERT INTO `openplatform_app_version_p_t` VALUES (325724998373212160, 325724998356434944, 'abilityIds', '1', 'default', 1, 'system', '2026-06-17 19:54:51.291', 'system', '2026-06-17 19:54:51.291');

SET FOREIGN_KEY_CHECKS = 1;
