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

 Date: 18/06/2026 09:53:12
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for openplatform_app_member_t
-- ----------------------------
DROP TABLE IF EXISTS `openplatform_app_member_t`;
CREATE TABLE `openplatform_app_member_t`  (
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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '应用成员表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of openplatform_app_member_t
-- ----------------------------
INSERT INTO `openplatform_app_member_t` VALUES (2001, 'default', 1001, 'system', 'system', 'system', 1, 1, 'system', '2026-06-14 00:29:55.346', 'system', '2026-06-14 00:29:55.346');
INSERT INTO `openplatform_app_member_t` VALUES (2101, 'default', 1101, 'system', 'system', 'system', 1, 1, 'system', '2026-06-14 00:29:55.354', 'system', '2026-06-14 00:29:55.354');
INSERT INTO `openplatform_app_member_t` VALUES (2102, 'default', 1102, 'system', 'system', 'system', 1, 1, 'system', '2026-06-14 00:29:55.354', 'system', '2026-06-14 00:29:55.354');
INSERT INTO `openplatform_app_member_t` VALUES (2103, 'default', 1103, 'system', 'system', 'system', 1, 1, 'system', '2026-06-14 00:29:55.354', 'system', '2026-06-14 00:29:55.354');
INSERT INTO `openplatform_app_member_t` VALUES (2104, 'default', 1104, 'system', 'system', 'system', 1, 1, 'system', '2026-06-14 00:29:55.354', 'system', '2026-06-14 00:29:55.354');
INSERT INTO `openplatform_app_member_t` VALUES (2105, 'default', 1105, 'system', 'system', 'system', 1, 1, 'system', '2026-06-14 00:29:55.354', 'system', '2026-06-14 00:29:55.354');
INSERT INTO `openplatform_app_member_t` VALUES (2106, 'default', 1106, 'system', 'system', 'system', 1, 1, 'system', '2026-06-14 00:29:55.354', 'system', '2026-06-14 00:29:55.354');
INSERT INTO `openplatform_app_member_t` VALUES (2107, 'default', 1107, 'system', 'system', 'system', 1, 1, 'system', '2026-06-14 00:29:55.354', 'system', '2026-06-14 00:29:55.354');
INSERT INTO `openplatform_app_member_t` VALUES (2108, 'default', 1108, 'system', 'system', 'system', 1, 1, 'system', '2026-06-14 00:29:55.354', 'system', '2026-06-14 00:29:55.354');
INSERT INTO `openplatform_app_member_t` VALUES (2109, 'default', 1109, 'system', 'system', 'system', 1, 1, 'system', '2026-06-14 00:29:55.354', 'system', '2026-06-14 00:29:55.354');
INSERT INTO `openplatform_app_member_t` VALUES (2110, 'default', 1110, 'system', 'system', 'system', 1, 1, 'system', '2026-06-14 00:29:55.354', 'system', '2026-06-14 00:29:55.354');
INSERT INTO `openplatform_app_member_t` VALUES (2111, 'default', 1111, 'system', 'system', 'system', 1, 1, 'system', '2026-06-14 00:29:55.354', 'system', '2026-06-14 00:29:55.354');
INSERT INTO `openplatform_app_member_t` VALUES (9998, 'default', 9999, 'system', 'system', 'system', 1, 1, 'system', '2026-06-14 00:33:41.107', 'system', '2026-06-14 00:33:41.107');
INSERT INTO `openplatform_app_member_t` VALUES (324464727126179840, 'default', 324344126021566464, '张三', 'Zhang San', 'user_001', 0, 1, 'system', '2026-06-14 08:26:59.214', 'system', '2026-06-14 08:26:59.214');
INSERT INTO `openplatform_app_member_t` VALUES (324472540355362816, 'default', 324344126021566464, '张三', 'Zhang San', 'user_001', 1, 1, 'system', '2026-06-14 08:58:02.033', 'system', '2026-06-14 08:58:02.033');
INSERT INTO `openplatform_app_member_t` VALUES (324478755038822400, 'default', 324471510402072576, '张三', 'Zhang San', 'user_001', 2, 1, 'system', '2026-06-14 09:22:43.727', 'system', '2026-06-14 09:22:43.727');
INSERT INTO `openplatform_app_member_t` VALUES (324478921724657664, 'default', 324471510402072576, '李四', 'Li Si', 'user_002', 2, 1, 'system', '2026-06-14 09:23:23.467', 'system', '2026-06-14 09:23:23.467');
INSERT INTO `openplatform_app_member_t` VALUES (324478921783377920, 'default', 324471510402072576, '刘美丽', 'Liu Meili', 'user_012', 2, 1, 'system', '2026-06-14 09:23:23.482', 'system', '2026-06-14 09:23:23.482');
INSERT INTO `openplatform_app_member_t` VALUES (324493545387851776, 'default', 324471510402072576, '李四', 'Li Si', 'user_002', 1, 1, 'system', '2026-06-14 10:21:30.020', 'system', '2026-06-14 10:21:30.020');
INSERT INTO `openplatform_app_member_t` VALUES (324625974001926144, 'default', 324625973976760320, '李四', 'Li Si', 'user_002', 1, 1, 'user_002', '2026-06-14 19:07:43.463', 'user_002', '2026-06-14 19:07:43.463');
INSERT INTO `openplatform_app_member_t` VALUES (324642391107567616, 'default', 324468743616856064, '张三', 'Zhang San', 'user_001', 2, 1, 'system', '2026-06-14 20:12:57.606', 'system', '2026-06-14 20:12:57.606');
INSERT INTO `openplatform_app_member_t` VALUES (324642428634005504, 'default', 324468743616856064, '系统用户', 'System', 'system', 2, 1, 'system', '2026-06-14 20:13:06.553', 'system', '2026-06-14 20:13:06.553');
INSERT INTO `openplatform_app_member_t` VALUES (324653475159867392, 'default', 1002, '张三', 'Zhang San', 'user_001', 2, 1, 'system', '2026-06-14 20:57:00.250', 'system', '2026-06-14 20:57:00.250');
INSERT INTO `openplatform_app_member_t` VALUES (324653518998732800, 'default', 1002, '张三', 'Zhang San', 'user_001', 1, 1, 'system', '2026-06-14 20:57:10.701', 'system', '2026-06-14 20:57:10.701');
INSERT INTO `openplatform_app_member_t` VALUES (324655961551667200, 'default', 324655520172474368, '张三', 'Zhang San', 'user_001', 2, 1, 'system', '2026-06-14 21:06:53.051', 'system', '2026-06-14 21:06:53.051');
INSERT INTO `openplatform_app_member_t` VALUES (324655961614581760, 'default', 324655520172474368, '李四', 'Li Si', 'user_002', 2, 1, 'system', '2026-06-14 21:06:53.066', 'system', '2026-06-14 21:06:53.066');
INSERT INTO `openplatform_app_member_t` VALUES (324655961627164672, 'default', 324655520172474368, '林小红', 'Lin Xiaohong', 'user_010', 2, 1, 'system', '2026-06-14 21:06:53.069', 'system', '2026-06-14 21:06:53.069');
INSERT INTO `openplatform_app_member_t` VALUES (324656057961938944, 'default', 324655520172474368, '张三', 'Zhang San', 'user_001', 1, 1, 'system', '2026-06-14 21:07:16.037', 'system', '2026-06-14 21:07:16.037');
INSERT INTO `openplatform_app_member_t` VALUES (325027295972556800, 'default', 325027096269160448, '张三', 'Zhang San', 'user_001', 2, 1, 'system', '2026-06-15 21:42:26.078', 'system', '2026-06-15 21:42:26.078');
INSERT INTO `openplatform_app_member_t` VALUES (325182989954711552, 'default', 325182989891796992, '系统用户', 'System', 'system', 1, 1, 'system', '2026-06-16 08:01:06.416', 'system', '2026-06-16 08:01:06.416');
INSERT INTO `openplatform_app_member_t` VALUES (325233647693070336, 'default', 325185686934126592, '周八', 'Zhou Ba', 'user_006', 2, 1, 'system', '2026-06-16 11:22:24.165', 'system', '2026-06-16 11:22:24.165');
INSERT INTO `openplatform_app_member_t` VALUES (325233647709847552, 'default', 325185686934126592, '张三', 'Zhang San', 'user_001', 2, 1, 'system', '2026-06-16 11:22:24.165', 'system', '2026-06-16 11:22:24.165');
INSERT INTO `openplatform_app_member_t` VALUES (325266152680849408, 'default', 325185686934126592, '李四', 'Li Si', 'user_002', 2, 1, 'system', '2026-06-16 13:31:33.955', 'system', '2026-06-16 13:31:33.955');
INSERT INTO `openplatform_app_member_t` VALUES (325268584911601664, 'default', 325185686934126592, '周八', 'Zhou Ba', 'user_006', 1, 1, 'system', '2026-06-16 13:41:13.842', 'system', '2026-06-16 13:41:13.842');
INSERT INTO `openplatform_app_member_t` VALUES (325269565690871808, 'default', 325027096269160448, 'system', 'system', 'system', 1, 1, 'system', '2026-06-16 13:45:07.679', 'system', '2026-06-16 13:48:10.823');
INSERT INTO `openplatform_app_member_t` VALUES (325270597632262144, 'default', 324468743616856064, '张三', 'Zhang San', 'user_001', 1, 1, 'system', '2026-06-16 13:49:13.713', 'system', '2026-06-16 13:49:13.713');
INSERT INTO `openplatform_app_member_t` VALUES (325349558089416704, 'default', 325185686934126592, '系统用户', 'System', 'system', 2, 1, 'system', '2026-06-16 19:02:59.356', 'system', '2026-06-16 19:02:59.356');
INSERT INTO `openplatform_app_member_t` VALUES (325349582798061568, 'default', 325185686934126592, '周八', 'Zhou Ba', 'user_006', 1, 1, 'system', '2026-06-16 19:03:05.244', 'system', '2026-06-16 19:03:05.244');
INSERT INTO `openplatform_app_member_t` VALUES (325379883091886080, 'default', 325185686934126592, '吴九', 'Wu Jiu', 'user_007', 0, 1, 'system', '2026-06-16 21:03:29.404', 'system', '2026-06-16 21:03:29.404');
INSERT INTO `openplatform_app_member_t` VALUES (325558609779884032, 'default', 325558609746329600, '系统用户', 'System', 'system', 1, 1, 'system', '2026-06-17 08:53:41.161', 'system', '2026-06-17 08:53:41.161');
INSERT INTO `openplatform_app_member_t` VALUES (325584117930393600, 'default', 325558609746329600, '黄大伟', 'Huang Dawei', 'user_011', 0, 1, 'system', '2026-06-17 10:35:02.777', 'system', '2026-06-17 10:35:02.777');
INSERT INTO `openplatform_app_member_t` VALUES (325724970028105728, 'default', 325558609746329600, '周八', 'Zhou Ba', 'user_006', 2, 1, 'system', '2026-06-17 19:54:44.535', 'system', '2026-06-17 19:54:44.535');

SET FOREIGN_KEY_CHECKS = 1;
