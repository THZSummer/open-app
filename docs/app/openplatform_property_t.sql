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

 Date: 18/06/2026 09:59:15
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for openplatform_property_t
-- ----------------------------
DROP TABLE IF EXISTS `openplatform_property_t`;
CREATE TABLE `openplatform_property_t`  (
  `id` bigint NOT NULL COMMENT '主键',
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '编码',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '名称',
  `value` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '值',
  `description` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
  `path` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '路径',
  `language` tinyint NOT NULL DEFAULT 1 COMMENT '语言: 1-中文 2-英文',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 0-失效 1-有效',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `last_update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最后更新人',
  `last_update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_path_code`(`path` ASC, `code` ASC) USING BTREE,
  INDEX `idx_path_name`(`path` ASC, `name` ASC) USING BTREE,
  INDEX `idx_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '数据字典表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of openplatform_property_t
-- ----------------------------
INSERT INTO `openplatform_property_t` VALUES (1, 'TEST_DICT', 'Updated Name', 'new_value', NULL, NULL, 1, 1, 'system', '2026-05-22 11:35:19.987', 'system', '2026-05-22 11:36:02.128');
INSERT INTO `openplatform_property_t` VALUES (4, 'www', 'www', 'ww', NULL, 'ww', 1, 1, 'system', '2026-05-22 11:43:03.744', 'system', '2026-05-22 11:43:03.744');
INSERT INTO `openplatform_property_t` VALUES (6, 'DICT_002', 'Updated Dictionary', 'updated_test', 'Updated Test', 'system/updated', 1, 1, 'system', '2026-05-22 12:56:23.688', 'system', '2026-05-22 12:56:45.472');
INSERT INTO `openplatform_property_t` VALUES (7, 'INTEGRATION_TEST', 'Integration Test Updated', 'updated-value', 'Updated', 'test/integration', 1, 0, 'system', '2026-05-22 12:58:41.703', 'system', '2026-05-22 12:58:48.414');
INSERT INTO `openplatform_property_t` VALUES (316650322204295168, 'wwdad', 'dasd', 'www', 'wwda', 'dada', 1, 1, 'system', '2026-05-23 18:55:19.892', 'system', '2026-05-23 18:55:25.483');
INSERT INTO `openplatform_property_t` VALUES (316711647425069056, 'USER_STATUS', '用户状态', 'active', '用户账户状态字典', 'system/user', 1, 1, 'system', '2026-05-23 22:59:00.964', 'system', '2026-05-23 22:59:00.964');
INSERT INTO `openplatform_property_t` VALUES (316713627908308992, 'TEST_DICT', 'Test Dict', 'test_val', 'test desc', '/test', 1, 1, 'system', '2026-05-23 23:06:53.148', 'system', '2026-05-23 23:06:53.148');
INSERT INTO `openplatform_property_t` VALUES (316717399967531008, 'D_A1', 'Dict A1', 'val1', 'desc', '/da1', 1, 1, 'system', '2026-05-23 23:21:52.477', 'system', '2026-05-23 23:21:52.477');
INSERT INTO `openplatform_property_t` VALUES (316717399975919616, 'D_B0', 'Dict B0', 'val0', 'desc', '/db0', 1, 1, 'system', '2026-05-23 23:21:52.479', 'system', '2026-05-23 23:21:52.479');
INSERT INTO `openplatform_property_t` VALUES (316717835122376704, 'D_NEW_A1', 'D A1', 'val1', 'desc', '/dna1', 1, 1, 'system', '2026-05-23 23:23:36.226', 'system', '2026-05-23 23:23:36.226');
INSERT INTO `openplatform_property_t` VALUES (316717835130765312, 'D_NEW_B0', 'D B0', 'val0', 'desc', '/dnb0', 1, 1, 'system', '2026-05-23 23:23:36.228', 'system', '2026-05-23 23:23:36.228');
INSERT INTO `openplatform_property_t` VALUES (316718803415203840, 'T_A1', 'Test A1', 'v1', 'd', '/ta1', 1, 1, 'system', '2026-05-23 23:27:27.085', 'system', '2026-05-23 23:27:27.085');
INSERT INTO `openplatform_property_t` VALUES (316718803423592448, 'T_B0', 'Test B0', 'v0', 'd', '/tb0', 1, 1, 'system', '2026-05-23 23:27:27.087', 'system', '2026-05-23 23:27:27.087');
INSERT INTO `openplatform_property_t` VALUES (316719469323878400, 'SIMPLE_B', 'Simple B', 'val_b', 'desc', '/sb', 1, 1, 'system', '2026-05-23 23:30:05.850', 'system', '2026-05-23 23:30:05.850');
INSERT INTO `openplatform_property_t` VALUES (317268588631162880, 'wds', '用户状态', 'active', '用户账户状态字典', 'system/user', 1, 0, 'system', '2026-05-25 11:52:06.095', 'system', '2026-05-25 11:52:06.095');
INSERT INTO `openplatform_property_t` VALUES (317319835144945664, 'TEST_LANG_3', 'TestLanguage3', 'test3', NULL, '/test3', 1, 1, 'system', '2026-05-25 15:15:44.217', 'system', '2026-05-25 15:15:44.217');
INSERT INTO `openplatform_property_t` VALUES (317319866191183872, 'TEST_LANG_4', '测试语言4', 'test4', NULL, '/test4', 1, 1, 'system', '2026-05-25 15:15:51.618', 'system', '2026-05-25 15:15:51.618');
INSERT INTO `openplatform_property_t` VALUES (317320047708078080, 'TEST_LANG_5', 'TestLang5', 'test5', NULL, '/test5', 1, 1, 'system', '2026-05-25 15:16:34.895', 'system', '2026-05-25 15:16:34.895');
INSERT INTO `openplatform_property_t` VALUES (317320642481356800, 'wwda', '测试修改', 'dww', NULL, 'd', 1, 1, 'system', '2026-05-25 15:18:56.700', 'system', '2026-05-26 16:05:25.209');
INSERT INTO `openplatform_property_t` VALUES (317336522384736256, 'wdda', 'wda-SPRING-CACHE-TEST', NULL, NULL, NULL, 1, 1, 'system', '2026-05-25 16:22:02.764', 'system', '2026-05-26 14:46:30.628');
INSERT INTO `openplatform_property_t` VALUES (317336522384736257, 'verify_type_multi_switch', '认证方式多选开关', 'true', '控制认证方式是否支持多选，true允许，false仅允许单选', 'app', 1, 1, 'system', '2026-06-12 17:40:40.900', 'system', '2026-06-12 17:55:03.629');

SET FOREIGN_KEY_CHECKS = 1;
