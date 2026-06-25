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

 Date: 18/06/2026 09:53:43
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for openplatform_app_version_t
-- ----------------------------
DROP TABLE IF EXISTS `openplatform_app_version_t`;
CREATE TABLE `openplatform_app_version_t`  (
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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '应用版本表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of openplatform_app_version_t
-- ----------------------------
INSERT INTO `openplatform_app_version_t` VALUES (324649316100603904, 324471510402072576, 'BUG017验证', NULL, '3.0.0', 'default', 1, 'user_002', '2026-06-14 20:40:28.656', 'user_002', '2026-06-14 20:50:53.474');
INSERT INTO `openplatform_app_version_t` VALUES (324657224133640192, 324655520172474368, 'ww', NULL, '1.0.0', 'default', 4, 'system', '2026-06-14 21:11:54.075', 'system', '2026-06-14 21:13:48.328');
INSERT INTO `openplatform_app_version_t` VALUES (324905878144679936, 324468743616856064, 'w', NULL, '0.0.0', 'default', 1, 'system', '2026-06-15 13:39:57.811', 'system', '2026-06-15 13:39:57.811');
INSERT INTO `openplatform_app_version_t` VALUES (325558357018542080, 325027096269160448, 'www', NULL, '1.0.0', 'default', 1, 'system', '2026-06-17 08:52:40.897', 'system', '2026-06-17 08:52:48.560');
INSERT INTO `openplatform_app_version_t` VALUES (325724998356434944, 325558609746329600, 'www', NULL, '0.0.0', 'default', 1, 'system', '2026-06-17 19:54:51.288', 'system', '2026-06-17 19:54:51.288');

SET FOREIGN_KEY_CHECKS = 1;
