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

 Date: 18/06/2026 09:52:56
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for openplatform_app_ability_relation_t
-- ----------------------------
DROP TABLE IF EXISTS `openplatform_app_ability_relation_t`;
CREATE TABLE `openplatform_app_ability_relation_t`  (
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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '应用能力关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of openplatform_app_ability_relation_t
-- ----------------------------
INSERT INTO `openplatform_app_ability_relation_t` VALUES (321729853608951808, 321729849888604160, 1, 1, 'default', 1, 'system', '2026-06-06 19:19:34.555', 'system', '2026-06-06 19:19:34.555');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (321741039415066624, 321740975556788224, 1, 1, 'default', 1, 'system', '2026-06-06 20:04:01.458', 'system', '2026-06-06 20:04:01.458');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (321742881465303040, 321742878940332032, 2, 2, 'default', 1, 'system', '2026-06-06 20:11:20.637', 'system', '2026-06-06 20:11:20.637');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (321746305263075328, 321745716915470336, 3, 3, 'default', 1, 'system', '2026-06-06 20:24:56.934', 'system', '2026-06-06 20:24:56.934');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (321770741638889472, 321755451064582144, 1, 1, 'default', 1, 'system', '2026-06-06 22:02:03.022', 'system', '2026-06-06 22:02:03.022');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (321771465068249088, 321755451064582144, 2, 2, 'default', 1, 'system', '2026-06-06 22:04:55.498', 'system', '2026-06-06 22:04:55.498');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (321776106506027008, 321755451064582144, 3, 3, 'default', 1, 'system', '2026-06-06 22:23:22.106', 'system', '2026-06-06 22:23:22.106');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (321777980210675712, 321755451064582144, 4, 4, 'default', 1, 'system', '2026-06-06 22:30:48.829', 'system', '2026-06-06 22:30:48.829');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (321778851132735488, 321755451064582144, 5, 5, 'default', 1, 'system', '2026-06-06 22:34:16.474', 'system', '2026-06-06 22:34:16.474');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (321783705179783168, 321755451064582144, 6, 6, 'default', 1, 'system', '2026-06-06 22:53:33.769', 'system', '2026-06-06 22:53:33.769');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (321921578294050816, 321755338334273536, 1, 1, 'default', 1, 'system', '2026-06-07 08:01:25.280', 'system', '2026-06-07 08:01:25.280');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (321922880134709248, 321755338334273536, 2, 2, 'default', 1, 'system', '2026-06-07 08:06:35.664', 'system', '2026-06-07 08:06:35.664');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (321924030347083776, 321755338334273536, 3, 3, 'default', 1, 'system', '2026-06-07 08:11:09.896', 'system', '2026-06-07 08:11:09.896');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (321925454585921536, 321755338334273536, 4, 4, 'default', 1, 'system', '2026-06-07 08:16:49.461', 'system', '2026-06-07 08:16:49.461');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (321932349610655744, 321755338334273536, 5, 5, 'default', 1, 'system', '2026-06-07 08:44:13.362', 'system', '2026-06-07 08:44:13.362');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (321933426179440640, 321755338334273536, 6, 6, 'default', 1, 'system', '2026-06-07 08:48:30.036', 'system', '2026-06-07 08:48:30.036');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (321942887728152576, 321739314356551680, 1, 1, 'default', 1, 'system', '2026-06-07 09:26:05.846', 'system', '2026-06-07 09:26:05.846');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (321943705332219904, 321739314356551680, 2, 2, 'default', 1, 'system', '2026-06-07 09:29:20.777', 'system', '2026-06-07 09:29:20.777');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (321944168010088448, 321739314356551680, 3, 3, 'default', 1, 'system', '2026-06-07 09:31:11.088', 'system', '2026-06-07 09:31:11.088');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (321945127239024640, 321739314356551680, 4, 4, 'default', 1, 'system', '2026-06-07 09:34:59.786', 'system', '2026-06-07 09:34:59.786');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (321945815515922432, 321739314356551680, 5, 5, 'default', 1, 'system', '2026-06-07 09:37:43.884', 'system', '2026-06-07 09:37:43.884');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (322016948160299008, 321739314356551680, 7, 7, 'default', 1, 'system', '2026-06-07 14:20:23.230', 'system', '2026-06-07 14:20:23.230');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (322112420615028736, 322042238597070848, 1, 1, 'default', 1, 'system', '2026-06-07 20:39:45.635', 'system', '2026-06-07 20:39:45.635');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (322140059551662080, 322042238597070848, 2, 2, 'default', 1, 'system', '2026-06-07 22:29:35.271', 'system', '2026-06-07 22:29:35.271');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (322141313694695424, 322042238597070848, 3, 4, 'default', 1, 'system', '2026-06-07 22:34:34.283', 'system', '2026-06-07 22:34:34.283');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (322437855286657024, 322397141458747392, 1, 1, 'default', 1, 'system', '2026-06-08 18:12:55.307', 'system', '2026-06-08 18:12:55.307');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (322437912492769280, 322397141458747392, 2, 2, 'default', 1, 'system', '2026-06-08 18:13:08.946', 'system', '2026-06-08 18:13:08.946');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (322437912727650304, 322397141458747392, 3, 4, 'default', 1, 'system', '2026-06-08 18:13:09.003', 'system', '2026-06-08 18:13:09.003');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (322437912849285120, 322397141458747392, 5, 5, 'default', 1, 'system', '2026-06-08 18:13:09.032', 'system', '2026-06-08 18:13:09.032');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (323041112228888576, 323028858804633600, 1, 1, 'default', 1, 'system', '2026-06-10 10:10:02.971', 'system', '2026-06-10 10:10:02.971');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (323041135100428288, 323028858804633600, 2, 2, 'default', 1, 'system', '2026-06-10 10:10:08.423', 'system', '2026-06-10 10:10:08.423');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (323172621598326784, 1, 1, 1, 'default', 1, 'system', '2026-06-10 18:52:37.249', 'system', '2026-06-10 18:52:37.249');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (323177106978635776, 900000000000000001, 1, 1, 'default', 1, 'system', '2026-06-10 19:10:26.650', 'system', '2026-06-10 19:10:26.650');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (323177179145830400, 900000000000000001, 2, 2, 'default', 1, 'system', '2026-06-10 19:10:43.862', 'system', '2026-06-10 19:10:43.862');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (323185030669008896, 900000000000000001, 3, 4, 'default', 1, 'system', '2026-06-10 19:41:55.802', 'system', '2026-06-10 19:41:55.802');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (323185141490909184, 900000000000000001, 4, 3, 'default', 1, 'system', '2026-06-10 19:42:22.227', 'system', '2026-06-10 19:42:22.227');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (323383496976367616, 900000000000000001, 5, 5, 'default', 1, 'system', '2026-06-11 08:50:33.857', 'system', '2026-06-11 08:50:33.857');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (323383552341180416, 900000000000000001, 7, 7, 'default', 1, 'system', '2026-06-11 08:50:47.057', 'system', '2026-06-11 08:50:47.057');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (323518445209518080, 323179669635465216, 1, 1, 'default', 1, 'system', '2026-06-11 17:46:48.027', 'system', '2026-06-11 17:46:48.027');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (323529559649550336, 323460836767039488, 1, 1, 'default', 1, 'system', '2026-06-11 18:30:57.913', 'system', '2026-06-11 18:30:57.913');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (323534924743180288, 323179669635465216, 2, 2, 'default', 1, 'system', '2026-06-11 18:52:17.050', 'system', '2026-06-11 18:52:17.050');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (323534950345211904, 323179669635465216, 3, 4, 'default', 1, 'system', '2026-06-11 18:52:23.153', 'system', '2026-06-11 18:52:23.153');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (323535056935059456, 323179669635465216, 7, 7, 'default', 1, 'system', '2026-06-11 18:52:48.566', 'system', '2026-06-11 18:52:48.566');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (323535095476518912, 323179669635465216, 5, 5, 'default', 1, 'system', '2026-06-11 18:52:57.755', 'system', '2026-06-11 18:52:57.755');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (323535117630832640, 323179669635465216, 4, 3, 'default', 1, 'system', '2026-06-11 18:53:03.036', 'system', '2026-06-11 18:53:03.036');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (323852040109293568, 323773828993908736, 1, 1, 'default', 1, 'system', '2026-06-12 15:52:23.244', 'system', '2026-06-12 15:52:23.244');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (324271780833263616, 323773828993908736, 2, 2, 'default', 1, 'system', '2026-06-13 19:40:17.236', 'system', '2026-06-13 19:40:17.236');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (324317645891436544, 323773379175776256, 1, 1, 'default', 1, 'system', '2026-06-13 22:42:32.314', 'system', '2026-06-13 22:42:32.314');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (324490711879647232, 324344126021566464, 1, 1, 'default', 1, 'system', '2026-06-14 10:10:14.461', 'system', '2026-06-14 10:10:14.461');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (324639113451929600, 1, 2, 2, 'default', 1, 'system', '2026-06-14 19:59:56.152', 'system', '2026-06-14 19:59:56.152');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (324639537340874752, 324344126021566464, 3, 4, 'default', 1, 'user_001', '2026-06-14 20:01:37.215', 'user_001', '2026-06-14 20:01:37.215');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (324656783530393600, 324655520172474368, 1, 1, 'default', 1, 'system', '2026-06-14 21:10:09.027', 'system', '2026-06-14 21:10:09.027');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (324656794867597312, 324655520172474368, 2, 2, 'default', 1, 'system', '2026-06-14 21:10:11.730', 'system', '2026-06-14 21:10:11.730');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (324656804648714240, 324655520172474368, 4, 3, 'default', 1, 'system', '2026-06-14 21:10:14.061', 'system', '2026-06-14 21:10:14.061');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (324891724360974336, 324655520172474368, 3, 4, 'default', 1, 'system', '2026-06-15 12:43:43.286', 'system', '2026-06-15 12:43:43.286');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (324891750143361024, 324655520172474368, 5, 5, 'default', 1, 'system', '2026-06-15 12:43:49.432', 'system', '2026-06-15 12:43:49.432');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (324892289065287680, 324655520172474368, 7, 7, 'default', 1, 'system', '2026-06-15 12:45:57.921', 'system', '2026-06-15 12:45:57.921');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (324905414258851840, 324468743616856064, 1, 1, 'default', 1, 'system', '2026-06-15 13:38:07.212', 'system', '2026-06-15 13:38:07.212');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (325027187319111680, 325027096269160448, 1, 1, 'default', 1, 'system', '2026-06-15 21:42:00.172', 'system', '2026-06-15 21:42:00.172');
INSERT INTO `openplatform_app_ability_relation_t` VALUES (325661255350091776, 325558609746329600, 1, 1, 'default', 1, 'system', '2026-06-17 15:41:33.772', 'system', '2026-06-17 15:41:33.772');

SET FOREIGN_KEY_CHECKS = 1;
