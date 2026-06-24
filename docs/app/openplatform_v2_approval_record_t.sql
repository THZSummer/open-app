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

 Date: 18/06/2026 09:59:45
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for openplatform_v2_approval_record_t
-- ----------------------------
DROP TABLE IF EXISTS `openplatform_v2_approval_record_t`;
CREATE TABLE `openplatform_v2_approval_record_t`  (
  `id` bigint NOT NULL COMMENT '主键ID（雪花ID）',
  `combined_nodes` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组合后的完整审批节点配置（JSON格式字符串）',
  `business_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务类型：\n        api_register = API注册审批，\n        event_register = 事件注册审批，\n        callback_register = 回调注册审批，\n        api_permission_apply = API权限申请审批，\n        event_permission_apply = 事件权限申请审批，\n        callback_permission_apply = 回调权限申请审批',
  `business_id` bigint NOT NULL COMMENT '业务对象ID',
  `applicant_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '申请人ID',
  `applicant_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '申请人姓名',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态：0=待审, 1=已通过, 2=已拒绝, 3=已撤销',
  `current_node` int NULL DEFAULT 0 COMMENT '当前审批节点索引',
  `completed_at` datetime(3) NULL DEFAULT NULL COMMENT '完成时间',
  `create_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `last_update_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人账号',
  `last_update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最后更新人账号',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_business`(`business_type` ASC, `business_id` ASC) USING BTREE,
  INDEX `idx_applicant`(`applicant_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '审批记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of openplatform_v2_approval_record_t
-- ----------------------------
INSERT INTO `openplatform_v2_approval_record_t` VALUES (322709022551572480, '[{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":1,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 321729852497461248, 'system', 'system', 3, 0, '2026-06-09 12:11:06.007', '2026-06-09 12:10:26.620', '2026-06-09 12:11:06.007', 'system', 'system');
INSERT INTO `openplatform_v2_approval_record_t` VALUES (322709337594134528, '[{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":1,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 321729852497461248, 'system', 'system', 1, 0, '2026-06-09 12:12:54.963', '2026-06-09 12:11:41.732', '2026-06-09 12:12:54.963', 'system', 'system');
INSERT INTO `openplatform_v2_approval_record_t` VALUES (322709744970104832, '[{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":1,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 321741039536701440, 'system', 'system', 2, 0, '2026-06-09 12:13:32.462', '2026-06-09 12:13:18.858', '2026-06-09 12:13:32.462', 'system', 'system');
INSERT INTO `openplatform_v2_approval_record_t` VALUES (322731911388594176, '[{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":1,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 322731438774419456, 'system', 'system', 3, 0, '2026-06-09 13:42:13.329', '2026-06-09 13:41:23.744', '2026-06-09 13:42:13.329', 'system', 'system');
INSERT INTO `openplatform_v2_approval_record_t` VALUES (322732175231287296, '[{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":1,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 322731438774419456, 'system', 'system', 3, 0, '2026-06-09 13:43:15.184', '2026-06-09 13:42:26.649', '2026-06-09 13:43:15.184', 'system', 'testuser');
INSERT INTO `openplatform_v2_approval_record_t` VALUES (322732383247794176, '[{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":1,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 322731438774419456, 'testuser', 'testuser', 1, 0, '2026-06-09 13:43:42.239', '2026-06-09 13:43:16.244', '2026-06-09 13:43:42.239', 'testuser', 'admin');
INSERT INTO `openplatform_v2_approval_record_t` VALUES (322733384591736832, '[{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":1,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 322733310751014912, 'system', 'system', 3, 0, '2026-06-09 13:49:22.678', '2026-06-09 13:47:14.983', '2026-06-09 13:49:22.678', 'system', 'system');
INSERT INTO `openplatform_v2_approval_record_t` VALUES (322735723352424448, '[{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":1,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 322735415318544384, 'e2euser', 'e2euser', 1, 0, '2026-06-09 13:56:32.735', '2026-06-09 13:56:32.587', '2026-06-09 13:56:32.735', 'e2euser', 'admin');
INSERT INTO `openplatform_v2_approval_record_t` VALUES (322737759548604416, '[{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":1,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 322737747557089280, 'system', 'system', 0, 0, NULL, '2026-06-09 14:04:38.054', '2026-06-09 14:04:38.054', 'system', 'system');
INSERT INTO `openplatform_v2_approval_record_t` VALUES (323041201294934016, '[{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":1,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 323041190792396800, 'system', 'system', 0, 0, NULL, '2026-06-10 10:10:24.204', '2026-06-10 10:10:24.204', 'system', 'system');
INSERT INTO `openplatform_v2_approval_record_t` VALUES (323067472510976000, '[{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":1,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 323067463707131904, 'system', 'system', 0, 0, NULL, '2026-06-10 11:54:47.750', '2026-06-10 11:54:47.750', 'system', 'system');
INSERT INTO `openplatform_v2_approval_record_t` VALUES (323067676794552320, '[{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":1,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 323067655831420928, 'system', 'system', 3, 0, '2026-06-10 12:10:28.131', '2026-06-10 11:55:36.455', '2026-06-10 12:10:28.131', 'system', 'system');
INSERT INTO `openplatform_v2_approval_record_t` VALUES (323177332267286528, '[{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":1,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 323177110208249856, 'system', 'system', 3, 0, '2026-06-10 19:12:01.382', '2026-06-10 19:11:20.357', '2026-06-10 19:12:01.382', 'system', 'system');
INSERT INTO `openplatform_v2_approval_record_t` VALUES (323185250626699264, '[{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":1,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 323177110208249856, 'system', 'system', 3, 0, '2026-06-10 19:43:18.793', '2026-06-10 19:42:48.241', '2026-06-10 19:43:18.793', 'system', 'system');
INSERT INTO `openplatform_v2_approval_record_t` VALUES (323383883183685632, '[{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":1,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 323177110208249856, 'system', 'system', 3, 0, '2026-06-11 08:52:22.603', '2026-06-11 08:52:05.933', '2026-06-11 08:52:22.603', 'system', 'system');
INSERT INTO `openplatform_v2_approval_record_t` VALUES (323384172083150848, '[{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":1,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 323177110208249856, 'system', 'system', 3, 0, '2026-06-11 08:53:37.233', '2026-06-11 08:53:14.812', '2026-06-11 08:53:37.233', 'system', 'system');
INSERT INTO `openplatform_v2_approval_record_t` VALUES (323385941496430592, '[{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":1,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 323177110208249856, 'system', 'system', 3, 0, '2026-06-11 09:01:12.255', '2026-06-11 09:00:16.673', '2026-06-11 09:01:12.255', 'system', 'system');
INSERT INTO `openplatform_v2_approval_record_t` VALUES (323386420183957504, '[{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":1,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 323177110208249856, 'system', 'system', 3, 0, '2026-06-11 09:02:38.556', '2026-06-11 09:02:10.801', '2026-06-11 09:02:38.556', 'system', 'system');
INSERT INTO `openplatform_v2_approval_record_t` VALUES (323548263305183232, '[{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":1,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 323548253746364416, 'system', 'system', 3, 0, '2026-06-11 19:45:29.061', '2026-06-11 19:45:17.208', '2026-06-11 19:45:29.061', 'system', 'system');
INSERT INTO `openplatform_v2_approval_record_t` VALUES (323775347449397248, '[{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":1,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 323775339543134208, 'system', 'system', 3, 0, '2026-06-12 15:23:46.259', '2026-06-12 10:47:38.287', '2026-06-12 15:23:46.259', 'system', 'system');
INSERT INTO `openplatform_v2_approval_record_t` VALUES (323847367893712896, '[{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":1,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 323847348549582848, 'system', 'system', 3, 0, '2026-06-12 15:44:37.946', '2026-06-12 15:33:49.299', '2026-06-12 15:44:37.946', 'system', 'system');
INSERT INTO `openplatform_v2_approval_record_t` VALUES (324290355681820672, '[{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":1,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 324290355207864320, 'system', 'system', 3, 0, '2026-06-13 22:11:53.482', '2026-06-13 20:54:05.818', '2026-06-13 22:11:53.482', 'system', 'api_admin');
INSERT INTO `openplatform_v2_approval_record_t` VALUES (324317700924899328, '[{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":1,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 324310610877612032, 'api_admin', 'api_admin', 0, 0, NULL, '2026-06-13 22:42:45.432', '2026-06-13 22:42:45.432', 'api_admin', 'api_admin');
INSERT INTO `openplatform_v2_approval_record_t` VALUES (324487548799811584, '[{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":1,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 324486946808135680, 'system', 'system', 3, 0, '2026-06-14 09:58:25.015', '2026-06-14 09:57:40.322', '2026-06-14 09:58:25.015', 'system', 'system');
INSERT INTO `openplatform_v2_approval_record_t` VALUES (324649524964360192, '[{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":1,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 324649316100603904, 'user_002', 'user_002', 3, 0, '2026-06-14 20:50:53.464', '2026-06-14 20:41:18.448', '2026-06-14 20:50:53.464', 'user_002', 'user_002');
INSERT INTO `openplatform_v2_approval_record_t` VALUES (324657437548216320, '[{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":1,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 324657224133640192, 'system', 'system', 3, 0, '2026-06-14 21:12:55.176', '2026-06-14 21:12:44.955', '2026-06-14 21:12:55.176', 'system', 'system');
INSERT INTO `openplatform_v2_approval_record_t` VALUES (324657502685757440, '[{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":1,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 324657224133640192, 'system', 'system', 0, 0, NULL, '2026-06-14 21:13:00.485', '2026-06-14 21:13:00.485', 'system', 'system');
INSERT INTO `openplatform_v2_approval_record_t` VALUES (325030797377536000, '[{\"type\":\"approver\",\"userId\":\"perm_admin\",\"userName\":\"权限管理员\",\"order\":1,\"level\":\"scene\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null},{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":2,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 325030786396848128, 'system', 'system', 3, 0, '2026-06-15 21:56:22.963', '2026-06-15 21:56:20.875', '2026-06-15 21:56:22.963', 'system', 'system');
INSERT INTO `openplatform_v2_approval_record_t` VALUES (325558366313119744, '[{\"type\":\"approver\",\"userId\":\"perm_admin\",\"userName\":\"权限管理员\",\"order\":1,\"level\":\"scene\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null},{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":2,\"level\":\"global\",\"status\":null,\"approveTime\":null,\"comment\":null,\"cardIds\":null}]', 'app_version_publish', 325558357018542080, 'system', 'system', 3, 0, '2026-06-17 08:52:48.556', '2026-06-17 08:52:43.111', '2026-06-17 08:52:48.556', 'system', 'system');

SET FOREIGN_KEY_CHECKS = 1;
