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

 Date: 18/06/2026 09:59:27
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for openplatform_v2_approval_flow_t
-- ----------------------------
DROP TABLE IF EXISTS `openplatform_v2_approval_flow_t`;
CREATE TABLE `openplatform_v2_approval_flow_t`  (
  `id` bigint NOT NULL COMMENT '主键ID（雪花ID）',
  `name_cn` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '中文名称',
  `name_en` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '英文名称',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程编码：\n        global=全局审批流程，\n        api_register=API注册审批流程，\n        event_register=事件注册审批流程，\n        callback_register=回调注册审批流程，\n        api_permission_apply=API权限申请审批流程，\n        event_permission_apply=事件权限申请审批流程，\n        callback_permission_apply=回调权限申请审批流程',
  `description_cn` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '中文描述',
  `description_en` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '英文描述',
  `nodes` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '审批节点配置（JSON格式字符串）',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：0=禁用, 1=启用',
  `create_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `last_update_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人账号',
  `last_update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最后更新人账号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '审批流程模板表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of openplatform_v2_approval_flow_t
-- ----------------------------
INSERT INTO `openplatform_v2_approval_flow_t` VALUES (1, '全局审批流程', 'Global Approval Flow', 'global', '系统全局审批流程，适用于所有申请的最终审核', 'System global approval flow, applicable to final review of all applications', '[{\"type\":\"approver\",\"userId\":\"admin\",\"userName\":\"系统管理员\",\"order\":1}]', 1, '2026-05-19 09:17:29.015', '2026-05-19 09:17:29.015', 'system', 'system');
INSERT INTO `openplatform_v2_approval_flow_t` VALUES (2, 'API注册审批流程', 'API Registration Approval Flow', 'api_register', 'API资源注册审批流程', 'API resource registration approval flow', '[{\"type\":\"approver\",\"userId\":\"api_admin\",\"userName\":\"API管理员\",\"order\":1}]', 1, '2026-05-19 09:17:29.018', '2026-05-19 09:17:29.018', 'system', 'system');
INSERT INTO `openplatform_v2_approval_flow_t` VALUES (3, '事件注册审批流程', 'Event Registration Approval Flow', 'event_register', '事件资源注册审批流程', 'Event resource registration approval flow', '[{\"type\":\"approver\",\"userId\":\"event_admin\",\"userName\":\"事件管理员\",\"order\":1}]', 1, '2026-05-19 09:17:29.020', '2026-05-19 09:17:29.020', 'system', 'system');
INSERT INTO `openplatform_v2_approval_flow_t` VALUES (4, '回调注册审批流程', 'Callback Registration Approval Flow', 'callback_register', '回调资源注册审批流程', 'Callback resource registration approval flow', '[{\"type\":\"approver\",\"userId\":\"callback_admin\",\"userName\":\"回调管理员\",\"order\":1}]', 1, '2026-05-19 09:17:29.021', '2026-05-19 09:17:29.021', 'system', 'system');
INSERT INTO `openplatform_v2_approval_flow_t` VALUES (5, 'API权限申请审批流程', 'API Permission Apply Approval Flow', 'api_permission_apply', 'API权限申请审批流程', 'API permission application approval flow', '[{\"type\":\"approver\",\"userId\":\"perm_admin\",\"userName\":\"权限管理员\",\"order\":1}]', 1, '2026-05-19 09:17:29.023', '2026-05-19 09:17:29.023', 'system', 'system');
INSERT INTO `openplatform_v2_approval_flow_t` VALUES (6, '事件权限申请审批流程', 'Event Permission Apply Approval Flow', 'event_permission_apply', '事件权限申请审批流程', 'Event permission application approval flow', '[{\"type\":\"approver\",\"userId\":\"perm_admin\",\"userName\":\"权限管理员\",\"order\":1}]', 1, '2026-05-19 09:17:29.025', '2026-05-19 09:17:29.025', 'system', 'system');
INSERT INTO `openplatform_v2_approval_flow_t` VALUES (7, '回调权限申请审批流程', 'Callback Permission Apply Approval Flow', 'callback_permission_apply', '回调权限申请审批流程', 'Callback permission application approval flow', '[{\"type\":\"approver\",\"userId\":\"perm_admin\",\"userName\":\"权限管理员\",\"order\":1}]', 1, '2026-05-19 09:17:29.026', '2026-05-19 09:17:29.026', 'system', 'system');
INSERT INTO `openplatform_v2_approval_flow_t` VALUES (8, '应用版本发布审批流程', 'App Version Publish Approval Flow', 'app_version_publish', '应用版本发布审批流程，版本发布前需经管理员审核', 'App version publish approval flow, requires admin review before publishing', '[{type:approver,userId:admin,userName:系统管理员,order:1}]', 1, '2026-06-09 11:45:29.951', '2026-06-09 11:45:29.951', 'system', 'system');

SET FOREIGN_KEY_CHECKS = 1;
