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

 Date: 18/06/2026 09:53:03
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for openplatform_app_identity_t
-- ----------------------------
DROP TABLE IF EXISTS `openplatform_app_identity_t`;
CREATE TABLE `openplatform_app_identity_t`  (
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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '应用凭证表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of openplatform_app_identity_t
-- ----------------------------
INSERT INTO `openplatform_app_identity_t` VALUES (324341980400189440, 324341980035284992, 'AK_app_1781367554021_7844', 'SK_app_1781367554021_7844_1781367554108', 'v1', 'v1', 'AK_app_1781367554021_7844', 'default', 1, 'system', '2026-06-14 00:19:14.115', 'system', '2026-06-14 00:19:14.115');
INSERT INTO `openplatform_app_identity_t` VALUES (324344126189338624, 324344126021566464, 'AK_app_1781368065666_2789', 'SK_app_1781368065666_2789_1781368065706', 'v1', 'v1', 'AK_app_1781368065666_2789', 'default', 1, 'system', '2026-06-14 00:27:45.709', 'system', '2026-06-14 00:27:45.709');
INSERT INTO `openplatform_app_identity_t` VALUES (324468743931428864, 324468743616856064, 'AK_app_1781397776814_427', 'SK_app_1781397776814_427_1781397776889', 'v1', 'v1', 'AK_app_1781397776814_427', 'default', 1, 'system', '2026-06-14 08:42:56.894', 'system', '2026-06-14 08:42:56.894');
INSERT INTO `openplatform_app_identity_t` VALUES (324471510590816256, 324471510402072576, 'AK_app_1781398436467_8822', 'SK_app_1781398436467_8822_1781398436513', 'v1', 'v1', 'AK_app_1781398436467_8822', 'default', 1, 'system', '2026-06-14 08:53:56.518', 'system', '2026-06-14 08:53:56.518');
INSERT INTO `openplatform_app_identity_t` VALUES (324625974014509056, 324625973976760320, 'AK_app_202606141907435140', 'SK_app_202606141907435140_1781435263462', 'v1', 'v1', 'AK_app_202606141907435140', 'default', 1, 'user_002', '2026-06-14 19:07:43.467', 'user_002', '2026-06-14 19:07:43.467');
INSERT INTO `openplatform_app_identity_t` VALUES (324655520227000320, 324655520172474368, 'AK_app_202606142105072979', 'SK_app_202606142105072979_1781442307829', 'v1', 'v1', 'AK_app_202606142105072979', 'default', 1, 'system', '2026-06-14 21:05:07.834', 'system', '2026-06-14 21:05:07.834');
INSERT INTO `openplatform_app_identity_t` VALUES (325027096327880704, 325027096269160448, 'AK_app_202606152141382502', 'SK_app_202606152141382502_1781530898476', 'v1', 'v1', 'AK_app_202606152141382502', 'default', 1, 'system', '2026-06-15 21:41:38.478', 'system', '2026-06-15 21:41:38.478');
INSERT INTO `openplatform_app_identity_t` VALUES (325182989975683072, 325182989891796992, 'AK_app_202606160801066827', 'SK_app_202606160801066827_1781568066416', 'v1', 'v1', 'AK_app_202606160801066827', 'default', 1, 'system', '2026-06-16 08:01:06.420', 'system', '2026-06-16 08:01:06.420');
INSERT INTO `openplatform_app_identity_t` VALUES (325185686971875328, 325185686934126592, 'AK_app_202606160811491412', 'SK_app_202606160811491412_1781568709432', 'v1', 'v1', 'AK_app_202606160811491412', 'default', 1, 'system', '2026-06-16 08:11:49.434', 'system', '2026-06-16 08:11:49.434');
INSERT INTO `openplatform_app_identity_t` VALUES (325558609800855552, 325558609746329600, 'AK_app_202606170853413529', 'SK_app_202606170853413529_1781657621161', 'v1', 'v1', 'AK_app_202606170853413529', 'default', 1, 'system', '2026-06-17 08:53:41.167', 'system', '2026-06-17 08:53:41.167');

SET FOREIGN_KEY_CHECKS = 1;
