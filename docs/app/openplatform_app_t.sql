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

 Date: 18/06/2026 09:53:29
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for openplatform_app_t
-- ----------------------------
DROP TABLE IF EXISTS `openplatform_app_t`;
CREATE TABLE `openplatform_app_t`  (
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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '应用表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of openplatform_app_t
-- ----------------------------
INSERT INTO `openplatform_app_t` VALUES (1001, 'app_pa_test_001', 'default', 'preset_chat', 'PA个人应用', 'PA Test', '', '', 0, 2, 1, 'system', '2026-06-14 00:29:55.295', 'system', '2026-06-15 10:52:10.938');
INSERT INTO `openplatform_app_t` VALUES (1002, 'app_la_test_002', 'default', 'preset_mail', 'LA存量应用', 'LA Test', '', '', 1, 4, 1, 'system', '2026-06-14 00:29:55.344', 'system', '2026-06-14 20:47:36.387');
INSERT INTO `openplatform_app_t` VALUES (1101, 'app_page_test_001', 'default', 'preset_file', '分页1', 'Page1', '', '', 0, 0, 1, 'system', '2026-06-14 00:28:55.350', 'system', '2026-06-15 10:55:03.451');
INSERT INTO `openplatform_app_t` VALUES (1102, 'app_page_test_002', 'default', 'preset_file', '分页2', 'Page2', '', '', 1, 4, 1, 'system', '2026-06-14 00:27:55.350', 'system', '2026-06-14 00:29:55.350');
INSERT INTO `openplatform_app_t` VALUES (1103, 'app_page_test_003', 'default', 'preset_file', '分页3', 'Page3', '', '', 1, 4, 1, 'system', '2026-06-14 00:26:55.350', 'system', '2026-06-14 00:29:55.350');
INSERT INTO `openplatform_app_t` VALUES (1104, 'app_page_test_004', 'default', 'preset_file', '分页4', 'Page4', '', '', 1, 4, 1, 'system', '2026-06-14 00:25:55.350', 'system', '2026-06-14 00:29:55.350');
INSERT INTO `openplatform_app_t` VALUES (1105, 'app_page_test_005', 'default', 'preset_file', '分页5', 'Page5', '', '', 1, 4, 1, 'system', '2026-06-14 00:24:55.350', 'system', '2026-06-14 00:29:55.350');
INSERT INTO `openplatform_app_t` VALUES (1106, 'app_page_test_006', 'default', 'preset_file', '分页6', 'Page6', '', '', 1, 4, 1, 'system', '2026-06-14 00:23:55.350', 'system', '2026-06-14 00:29:55.350');
INSERT INTO `openplatform_app_t` VALUES (1107, 'app_page_test_007', 'default', 'preset_file', '分页7', 'Page7', '', '', 1, 4, 1, 'system', '2026-06-14 00:22:55.350', 'system', '2026-06-14 00:29:55.350');
INSERT INTO `openplatform_app_t` VALUES (1108, 'app_page_test_008', 'default', 'preset_file', '分页8', 'Page8', '', '', 1, 4, 1, 'system', '2026-06-14 00:21:55.350', 'system', '2026-06-14 00:29:55.350');
INSERT INTO `openplatform_app_t` VALUES (1109, 'app_page_test_009', 'default', 'preset_file', '分页9', 'Page9', '', '', 1, 4, 1, 'system', '2026-06-14 00:20:55.350', 'system', '2026-06-14 00:29:55.350');
INSERT INTO `openplatform_app_t` VALUES (1110, 'app_page_test_010', 'default', 'preset_file', '分页10', 'Page10', '', '', 1, 4, 1, 'system', '2026-06-14 00:19:55.350', 'system', '2026-06-14 00:29:55.350');
INSERT INTO `openplatform_app_t` VALUES (1111, 'app_page_test_011', 'default', 'preset_file', '分页11', 'Page11', '', '', 1, 4, 1, 'system', '2026-06-14 00:18:55.350', 'system', '2026-06-14 00:29:55.350');
INSERT INTO `openplatform_app_t` VALUES (9999, 'app_disabled_test', 'default', 'preset_robot', '已禁用应用', 'Disabled', '', '', 1, 4, 0, 'system', '2026-06-14 00:33:41.104', 'system', '2026-06-14 00:33:41.104');
INSERT INTO `openplatform_app_t` VALUES (324344126021566464, 'app_1781368065666_2789', 'default', 'preset_robot', 'S1测试应用', 'S1 Test App', 'E2E测试编辑的描述', '', 1, 4, 1, 'system', '2026-06-14 00:27:45.670', 'system', '2026-06-14 01:12:23.644');
INSERT INTO `openplatform_app_t` VALUES (324468743616856064, 'app_1781397776814_427', 'default', 'preset_calendar', 'BUG010验证应用', 'BUG010 Verify', '', '', 1, 4, 1, 'system', '2026-06-14 08:42:56.823', 'system', '2026-06-14 10:25:15.583');
INSERT INTO `openplatform_app_t` VALUES (324471510402072576, 'app_1781398436467_8822', 'default', 'preset_robot', 'BUG008再验证', 'BUG008 Retest', 'E2E编辑描述验证', '', 1, 4, 1, 'system', '2026-06-14 08:53:56.475', 'system', '2026-06-14 20:49:35.681');
INSERT INTO `openplatform_app_t` VALUES (324625973976760320, 'app_202606141907435140', 'default', 'preset_robot', '图标删除测试', 'IconDel Test', '', '', 1, 4, 1, 'user_002', '2026-06-14 19:07:43.457', 'user_002', '2026-06-14 19:07:43.457');
INSERT INTO `openplatform_app_t` VALUES (324655520172474368, 'app_202606142105072979', 'default', 'preset_robot', '??????', 'logVerify', 'test desc', 'as', 1, 4, 1, 'system', '2026-06-14 21:05:07.819', 'system', '2026-06-16 18:59:49.075');
INSERT INTO `openplatform_app_t` VALUES (325027096269160448, 'app_202606152141382502', 'default', 'preset_mail', '测试1234ww', 'ddddww', 'ww', 'ww', 1, 4, 1, 'system', '2026-06-15 21:41:38.464', 'system', '2026-06-17 08:52:17.761');
INSERT INTO `openplatform_app_t` VALUES (325185686934126592, 'app_202606160811491412', 'default', 'preset_chat', 'test??', 'test', '', '', 1, 4, 1, 'system', '2026-06-16 08:11:49.426', 'system', '2026-06-16 18:59:33.166');
INSERT INTO `openplatform_app_t` VALUES (325558609746329600, 'app_202606170853413529', 'default', 'file_1781682108151_166', '测试123wDd', 'DAWs', 'W', 'w', 1, 4, 1, 'system', '2026-06-17 08:53:41.154', 'system', '2026-06-17 19:54:26.795');

SET FOREIGN_KEY_CHECKS = 1;
