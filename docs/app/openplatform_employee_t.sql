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

 Date: 18/06/2026 09:54:02
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for openplatform_employee_t
-- ----------------------------
DROP TABLE IF EXISTS `openplatform_employee_t`;
CREATE TABLE `openplatform_employee_t`  (
  `id` bigint NOT NULL COMMENT '主键',
  `welink_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'WeLink账号ID即member表account_id',
  `w3_account` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'W3工号',
  `chinese_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '中文名',
  `english_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '英文名',
  `department` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '部门',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态',
  `create_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3),
  `last_update_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_welink_id`(`welink_id` ASC) USING BTREE,
  INDEX `idx_w3_account`(`w3_account` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '人员信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of openplatform_employee_t
-- ----------------------------
INSERT INTO `openplatform_employee_t` VALUES (1, 'user_001', 'E10001', '张三', 'Zhang San', '平台研发部', 1, '2026-06-10 09:41:58.443', '2026-06-10 10:04:09.181');
INSERT INTO `openplatform_employee_t` VALUES (2, 'user_002', 'E10002', '李四', 'Li Si', '平台研发部', 1, '2026-06-10 09:41:58.443', '2026-06-10 10:04:09.181');
INSERT INTO `openplatform_employee_t` VALUES (3, 'user_003', 'E10003', '王五', 'Wang Wu', '产品设计部', 1, '2026-06-10 09:41:58.443', '2026-06-10 10:04:09.184');
INSERT INTO `openplatform_employee_t` VALUES (4, 'user_004', 'E10004', '赵六', 'Zhao Liu', '产品设计部', 1, '2026-06-10 09:41:58.443', '2026-06-10 10:04:09.184');
INSERT INTO `openplatform_employee_t` VALUES (5, 'user_005', 'E10005', '孙七', 'Sun Qi', '质量保障部', 1, '2026-06-10 09:41:58.443', '2026-06-10 10:04:09.186');
INSERT INTO `openplatform_employee_t` VALUES (6, 'user_006', 'E10006', '周八', 'Zhou Ba', '质量保障部', 1, '2026-06-10 09:41:58.443', '2026-06-10 10:04:09.186');
INSERT INTO `openplatform_employee_t` VALUES (7, 'user_007', 'E10007', '吴九', 'Wu Jiu', '运维部', 1, '2026-06-10 09:41:58.443', '2026-06-10 10:04:09.187');
INSERT INTO `openplatform_employee_t` VALUES (8, 'user_008', 'E10008', '郑十', 'Zheng Shi', '运维部', 1, '2026-06-10 09:41:58.443', '2026-06-10 10:04:09.187');
INSERT INTO `openplatform_employee_t` VALUES (9, 'user_009', 'E10009', '陈晓明', 'Chen Xiaoming', '前端开发组', 1, '2026-06-10 09:41:58.443', '2026-06-10 10:04:09.188');
INSERT INTO `openplatform_employee_t` VALUES (10, 'user_010', 'E10010', '林小红', 'Lin Xiaohong', '前端开发组', 1, '2026-06-10 09:41:58.443', '2026-06-10 10:04:09.188');
INSERT INTO `openplatform_employee_t` VALUES (11, 'user_011', 'E10011', '黄大伟', 'Huang Dawei', '后端开发组', 1, '2026-06-10 09:41:58.443', '2026-06-10 10:04:09.190');
INSERT INTO `openplatform_employee_t` VALUES (12, 'user_012', 'E10012', '刘美丽', 'Liu Meili', '后端开发组', 1, '2026-06-10 09:41:58.443', '2026-06-10 10:04:09.190');
INSERT INTO `openplatform_employee_t` VALUES (13, 'user_013', 'E10013', '杨帆', 'Yang Fan', '测试部', 1, '2026-06-10 09:41:58.443', '2026-06-10 10:04:09.191');
INSERT INTO `openplatform_employee_t` VALUES (14, 'user_014', 'E10014', '许静', 'Xu Jing', '测试部', 1, '2026-06-10 09:41:58.443', '2026-06-10 10:04:09.191');
INSERT INTO `openplatform_employee_t` VALUES (15, 'user_015', 'E10015', '何强', 'He Qiang', '安全部', 1, '2026-06-10 09:41:58.443', '2026-06-10 10:04:09.193');
INSERT INTO `openplatform_employee_t` VALUES (100, 'system', 'S00000', '系统用户', 'System', '系统', 1, '2026-06-10 09:41:58.443', '2026-06-10 10:04:09.194');

SET FOREIGN_KEY_CHECKS = 1;
