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

 Date: 18/06/2026 09:52:48
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for openplatform_ability_t
-- ----------------------------
DROP TABLE IF EXISTS `openplatform_ability_t`;
CREATE TABLE `openplatform_ability_t`  (
  `id` bigint NOT NULL COMMENT '主键',
  `ability_name_cn` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '能力中文名',
  `ability_name_en` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '能力英文名',
  `ability_desc_cn` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '能力中文描述',
  `ability_desc_en` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '能力英文描述',
  `ability_type` tinyint(1) NOT NULL DEFAULT 0 COMMENT '能力类型 1-群置顶 2-群通知 3-链接增强 4-点对点通知 5-we码 6-应用入群通知 7-助手广场卡片',
  `order_num` int NOT NULL COMMENT '序号',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：0=失效, 1=有效',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `last_update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后更新人',
  `last_update_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ability_type`(`ability_type` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '能力表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of openplatform_ability_t
-- ----------------------------
INSERT INTO `openplatform_ability_t` VALUES (1, '群置顶服务', '群置顶', '群置顶', '群置顶', 1, 1, 1, 'w', '2026-06-07 09:26:14.900', 'w', '2026-06-12 15:53:08.168');
INSERT INTO `openplatform_ability_t` VALUES (2, '群通知服务', '群通知', '群通知', '群通知', 2, 2, 1, 'w', '2026-06-07 09:26:14.900', 'w', '2026-06-12 15:53:09.534');
INSERT INTO `openplatform_ability_t` VALUES (3, '点对点通知服务', '点对点', '点对点', '点对点', 4, 3, 1, 'w', '2026-06-07 09:26:14.900', 'w', '2026-06-12 15:53:17.969');
INSERT INTO `openplatform_ability_t` VALUES (4, 'URL链接增强服务', 'URL链接增强', 'URL链接增强', 'URL链接增强', 3, 4, 1, 'w', '2026-06-07 09:26:14.900', 'w', '2026-06-12 15:53:22.833');
INSERT INTO `openplatform_ability_t` VALUES (5, 'we码', 'we码', 'we码', 'we码', 5, 5, 1, 'w', '2026-06-07 09:26:14.900', 'w', '2026-06-07 09:28:08.732');
INSERT INTO `openplatform_ability_t` VALUES (6, '应用入群通知', 'Group Join Notification', '当有新成员加入群聊时通知应用', 'Notify app when new member joins group', 6, 6, 1, 'system', '2026-06-07 13:21:05.266', 'system', '2026-06-07 13:21:05.266');
INSERT INTO `openplatform_ability_t` VALUES (7, '助手广场卡片', 'Assistant Square Card', '在助手广场展示应用卡片', 'Display app card in assistant square', 7, 7, 1, 'system', '2026-06-07 13:21:05.266', 'system', '2026-06-07 13:21:05.266');

SET FOREIGN_KEY_CHECKS = 1;
