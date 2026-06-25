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

 Date: 18/06/2026 09:53:21
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for openplatform_app_p_t
-- ----------------------------
DROP TABLE IF EXISTS `openplatform_app_p_t`;
CREATE TABLE `openplatform_app_p_t`  (
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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '应用属性表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of openplatform_app_p_t
-- ----------------------------
INSERT INTO `openplatform_app_p_t` VALUES (324344126134812672, 324344126021566464, 'eamap_app_code', 'eamap_approval_003', 'default', 1, 'system', '2026-06-14 00:27:45.700', 'system', '2026-06-15 10:35:13.253');
INSERT INTO `openplatform_app_p_t` VALUES (324349300916092928, 324344126021566464, 'verify_type', '0,2', 'default', 1, 'system', '2026-06-14 00:48:19.459', 'system', '2026-06-14 00:48:19.459');
INSERT INTO `openplatform_app_p_t` VALUES (324349300937064448, 324344126021566464, 'api_secret', 'Abcdef1234567890', 'default', 1, 'system', '2026-06-14 00:48:19.464', 'system', '2026-06-14 00:48:19.464');
INSERT INTO `openplatform_app_p_t` VALUES (324468743834959872, 324468743616856064, 'eamap_app_code', 'eamap_workflow_001', 'default', 1, 'system', '2026-06-14 08:42:56.884', 'system', '2026-06-15 10:35:13.253');
INSERT INTO `openplatform_app_p_t` VALUES (324471510477570048, 324471510402072576, 'eamap_app_code', 'eamap_drive_010', 'default', 1, 'system', '2026-06-14 08:53:56.506', 'system', '2026-06-15 10:35:13.253');
INSERT INTO `openplatform_app_p_t` VALUES (324625973985148928, 324625973976760320, 'eamap_app_code', 'eamap_notification_002', 'default', 1, 'user_002', '2026-06-14 19:07:43.461', 'user_002', '2026-06-15 10:35:13.253');
INSERT INTO `openplatform_app_p_t` VALUES (324625973985148929, 324625973976760320, 'verify_type', '0', 'default', 1, 'user_002', '2026-06-14 19:07:43.461', 'user_002', '2026-06-14 19:07:43.461');
INSERT INTO `openplatform_app_p_t` VALUES (324629076813807616, 324471510402072576, 'verify_type', '0,0', 'default', 1, 'system', '2026-06-14 19:20:03.231', 'system', '2026-06-14 19:20:03.231');
INSERT INTO `openplatform_app_p_t` VALUES (324648483870998528, 324468743616856064, 'verify_type', '0', 'default', 1, 'system', '2026-06-14 20:37:10.234', 'system', '2026-06-14 20:37:10.234');
INSERT INTO `openplatform_app_p_t` VALUES (324651110218334208, 1002, 'eamap_app_code', 'eamap_expense_006', 'default', 1, 'system', '2026-06-14 20:47:36.404', 'system', '2026-06-15 10:35:13.253');
INSERT INTO `openplatform_app_p_t` VALUES (324655520185057280, 324655520172474368, 'eamap_app_code', 'eamap_report_013', 'default', 1, 'system', '2026-06-14 21:05:07.824', 'system', '2026-06-15 10:35:13.253');
INSERT INTO `openplatform_app_p_t` VALUES (324655911245185024, 324655520172474368, 'verify_type', '0,2', 'default', 1, 'system', '2026-06-14 21:06:41.057', 'system', '2026-06-14 21:06:41.057');
INSERT INTO `openplatform_app_p_t` VALUES (324655911253573632, 324655520172474368, 'api_secret', 'wwda211111111111', 'default', 1, 'system', '2026-06-14 21:06:41.060', 'system', '2026-06-14 21:06:41.060');
INSERT INTO `openplatform_app_p_t` VALUES (325027096281743360, 325027096269160448, 'eamap_app_code', 'eamap_iot_015', 'default', 1, 'system', '2026-06-15 21:41:38.469', 'system', '2026-06-15 21:41:38.469');
INSERT INTO `openplatform_app_p_t` VALUES (325027096281743361, 325027096269160448, 'verify_type', '0', 'default', 1, 'system', '2026-06-15 21:41:38.469', 'system', '2026-06-15 21:41:38.469');
INSERT INTO `openplatform_app_p_t` VALUES (325185686946709504, 325185686934126592, 'eamap_app_code', 'eamap_calendar_008', 'default', 1, 'system', '2026-06-16 08:11:49.429', 'system', '2026-06-16 08:11:49.429');
INSERT INTO `openplatform_app_p_t` VALUES (325185837077626880, 325185686934126592, 'verify_type', '0,2,3', 'default', 1, 'system', '2026-06-16 08:12:25.221', 'system', '2026-06-16 08:12:25.221');
INSERT INTO `openplatform_app_p_t` VALUES (325185837090209792, 325185686934126592, 'api_secret', 'wwwwww5555555555', 'default', 1, 'system', '2026-06-16 08:12:25.224', 'system', '2026-06-16 08:12:25.224');
INSERT INTO `openplatform_app_p_t` VALUES (325348693299429376, 325185686934126592, 'diagram_id_list', 'file_1781607561594_1875', 'default', 1, 'system', '2026-06-16 18:59:33.171', 'system', '2026-06-16 18:59:33.171');
INSERT INTO `openplatform_app_p_t` VALUES (325558260000096256, 325027096269160448, 'diagram_id_list', 'file_1781657537182_3886', 'default', 1, 'system', '2026-06-17 08:52:17.767', 'system', '2026-06-17 08:52:17.767');
INSERT INTO `openplatform_app_p_t` VALUES (325558609763106816, 325558609746329600, 'eamap_app_code', 'eamap_contacts_012', 'default', 1, 'system', '2026-06-17 08:53:41.157', 'system', '2026-06-17 08:53:41.157');
INSERT INTO `openplatform_app_p_t` VALUES (325724895642124288, 325558609746329600, 'diagram_id_list', 'file_1781680370242_2790', 'default', 1, 'system', '2026-06-17 19:54:26.798', 'system', '2026-06-17 19:54:26.798');
INSERT INTO `openplatform_app_p_t` VALUES (325724940684754944, 325558609746329600, 'verify_type', '0,2', 'default', 1, 'system', '2026-06-17 19:54:37.538', 'system', '2026-06-17 19:54:37.538');
INSERT INTO `openplatform_app_p_t` VALUES (325724940697337856, 325558609746329600, 'api_secret', 'ww22222222222222', 'default', 1, 'system', '2026-06-17 19:54:37.540', 'system', '2026-06-17 19:54:37.540');

SET FOREIGN_KEY_CHECKS = 1;
