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

 Date: 18/06/2026 09:53:52
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for openplatform_eamap_t
-- ----------------------------
DROP TABLE IF EXISTS `openplatform_eamap_t`;
CREATE TABLE `openplatform_eamap_t`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `eamap_app_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `name_cn` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `name_en` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '',
  `owner_account_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` tinyint NULL DEFAULT 1,
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'system',
  `create_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3),
  `last_update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'system',
  `last_update_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of openplatform_eamap_t
-- ----------------------------
INSERT INTO `openplatform_eamap_t` VALUES (1, 'eamap_approval_003', '审批中心', 'Approval Center', 'system', 1, 'api_admin', '2026-06-07 09:39:34.079', 'system', '2026-06-13 22:50:25.634');
INSERT INTO `openplatform_eamap_t` VALUES (2, 'eamap_notification_002', '消息通知中心', 'Notification Center', 'user_002', 1, 'system', '2026-06-07 09:39:34.079', 'system', '2026-06-14 19:06:48.723');
INSERT INTO `openplatform_eamap_t` VALUES (3, 'eamap_workflow_001', '工作流引擎', 'Workflow Engine', 'system', 1, 'system', '2026-06-07 09:39:34.079', 'system', '2026-06-13 22:50:25.689');
INSERT INTO `openplatform_eamap_t` VALUES (5, 'eamap_leave_005', '请假审批', 'Leave Approval', 'system', 1, 'system', '2026-06-11 09:24:41.336', 'system', '2026-06-15 21:39:43.097');
INSERT INTO `openplatform_eamap_t` VALUES (6, 'eamap_expense_006', '费用报销', 'Expense Reimbursement', 'system', 1, 'system', '2026-06-11 09:24:41.336', 'system', '2026-06-13 22:50:25.707');
INSERT INTO `openplatform_eamap_t` VALUES (7, 'eamap_meeting_007', '会议管理', 'Meeting Management', 'system', 1, 'system', '2026-06-11 09:24:41.336', 'system', '2026-06-13 22:50:25.711');
INSERT INTO `openplatform_eamap_t` VALUES (8, 'eamap_calendar_008', '日程管理', 'Calendar Management', 'system', 1, 'system', '2026-06-11 09:24:41.336', 'system', '2026-06-13 22:50:25.718');
INSERT INTO `openplatform_eamap_t` VALUES (9, 'eamap_im_009', '即时通讯', 'Instant Messaging', 'system', 1, 'system', '2026-06-11 09:24:41.336', 'system', '2026-06-13 22:50:25.721');
INSERT INTO `openplatform_eamap_t` VALUES (10, 'eamap_drive_010', '云盘存储', 'Cloud Drive', 'system', 1, 'system', '2026-06-11 09:24:41.336', 'system', '2026-06-13 22:50:25.723');
INSERT INTO `openplatform_eamap_t` VALUES (11, 'eamap_mail_011', '邮件服务', 'Email Service', 'system', 1, 'system', '2026-06-11 09:24:41.336', 'system', '2026-06-13 22:50:25.727');
INSERT INTO `openplatform_eamap_t` VALUES (12, 'eamap_contacts_012', '通讯录', 'Contacts', 'system', 1, 'system', '2026-06-11 09:24:41.336', 'system', '2026-06-13 22:50:25.733');
INSERT INTO `openplatform_eamap_t` VALUES (13, 'eamap_report_013', '报表中心', 'Report Center', 'system', 1, 'system', '2026-06-11 09:24:41.336', 'system', '2026-06-13 22:50:25.735');
INSERT INTO `openplatform_eamap_t` VALUES (14, 'eamap_sso_014', '统一认证', 'Single Sign-On', '13', 1, 'system', '2026-06-11 09:24:41.336', 'system', '2026-06-13 22:50:32.216');
INSERT INTO `openplatform_eamap_t` VALUES (15, 'eamap_iot_015', '物联网平台', 'IoT Platform', 'system', 1, 'system', '2026-06-11 09:24:41.336', 'system', '2026-06-13 22:50:25.740');
INSERT INTO `openplatform_eamap_t` VALUES (16, 'eamap_attendance_004', '考勤管理', 'Attendance', 'user_002', 1, 'system', '2026-06-14 19:09:49.613', 'system', '2026-06-14 19:09:49.613');

SET FOREIGN_KEY_CHECKS = 1;
