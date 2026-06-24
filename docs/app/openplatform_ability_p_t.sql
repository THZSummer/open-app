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

 Date: 18/06/2026 09:52:38
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for openplatform_ability_p_t
-- ----------------------------
DROP TABLE IF EXISTS `openplatform_ability_p_t`;
CREATE TABLE `openplatform_ability_p_t`  (
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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '能力属性表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of openplatform_ability_p_t
-- ----------------------------
INSERT INTO `openplatform_ability_p_t` VALUES (1, 1, 'icon', 'ability_icon_1', 1, 'system', '2026-06-07 13:21:05.288', 'system', '2026-06-14 19:35:40.812');
INSERT INTO `openplatform_ability_p_t` VALUES (3, 1, 'example_diagram', 'ability_diagram_1', 1, 'system', '2026-06-07 13:21:05.292', 'system', '2026-06-15 09:16:51.968');
INSERT INTO `openplatform_ability_p_t` VALUES (4, 2, 'icon', 'ability_icon_2', 1, 'system', '2026-06-07 13:21:05.295', 'system', '2026-06-14 19:35:40.812');
INSERT INTO `openplatform_ability_p_t` VALUES (6, 2, 'example_diagram', 'ability_diagram_2', 1, 'system', '2026-06-07 13:21:05.299', 'system', '2026-06-15 09:16:51.968');
INSERT INTO `openplatform_ability_p_t` VALUES (7, 3, 'icon', 'ability_icon_3', 1, 'system', '2026-06-07 13:21:05.301', 'system', '2026-06-14 19:35:40.812');
INSERT INTO `openplatform_ability_p_t` VALUES (9, 3, 'example_diagram', 'ability_diagram_3', 1, 'system', '2026-06-07 13:21:05.305', 'system', '2026-06-15 09:16:51.968');
INSERT INTO `openplatform_ability_p_t` VALUES (10, 4, 'icon', 'ability_icon_4', 1, 'system', '2026-06-07 13:21:05.307', 'system', '2026-06-14 19:35:40.812');
INSERT INTO `openplatform_ability_p_t` VALUES (12, 4, 'example_diagram', 'ability_diagram_4', 1, 'system', '2026-06-07 13:21:05.310', 'system', '2026-06-15 09:16:51.968');
INSERT INTO `openplatform_ability_p_t` VALUES (13, 5, 'icon', 'ability_icon_5', 1, 'system', '2026-06-07 13:21:05.312', 'system', '2026-06-14 19:35:40.812');
INSERT INTO `openplatform_ability_p_t` VALUES (15, 5, 'example_diagram', 'ability_diagram_5', 1, 'system', '2026-06-07 13:21:05.316', 'system', '2026-06-15 09:16:51.968');
INSERT INTO `openplatform_ability_p_t` VALUES (16, 6, 'icon', 'ability_icon_6', 1, 'system', '2026-06-07 13:21:05.318', 'system', '2026-06-14 19:35:40.812');
INSERT INTO `openplatform_ability_p_t` VALUES (18, 6, 'example_diagram', 'ability_diagram_6', 1, 'system', '2026-06-07 13:21:05.321', 'system', '2026-06-15 09:16:51.968');
INSERT INTO `openplatform_ability_p_t` VALUES (19, 7, 'icon', 'ability_icon_7', 1, 'system', '2026-06-07 13:21:05.323', 'system', '2026-06-14 19:35:40.812');
INSERT INTO `openplatform_ability_p_t` VALUES (21, 7, 'example_diagram', 'ability_diagram_7', 1, 'system', '2026-06-07 13:21:05.327', 'system', '2026-06-15 09:16:51.968');

SET FOREIGN_KEY_CHECKS = 1;
