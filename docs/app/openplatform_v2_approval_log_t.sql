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

 Date: 18/06/2026 09:59:36
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for openplatform_v2_approval_log_t
-- ----------------------------
DROP TABLE IF EXISTS `openplatform_v2_approval_log_t`;
CREATE TABLE `openplatform_v2_approval_log_t`  (
  `id` bigint NOT NULL COMMENT '主键ID（雪花ID）',
  `record_id` bigint NOT NULL COMMENT '审批记录ID',
  `node_index` int NOT NULL COMMENT '节点索引',
  `level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审批级别：global=全局, scene=场景, resource=资源',
  `operator_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人姓名',
  `action` tinyint NOT NULL COMMENT '操作类型：0=同意, 1=拒绝, 2=撤销, 3=转交',
  `comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '审批意见',
  `create_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `last_update_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人账号',
  `last_update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最后更新人账号',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_record_id`(`record_id` ASC) USING BTREE,
  INDEX `idx_level`(`level` ASC) USING BTREE,
  INDEX `idx_operator`(`operator_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '审批操作日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of openplatform_v2_approval_log_t
-- ----------------------------
INSERT INTO `openplatform_v2_approval_log_t` VALUES (322709644696879104, 322709337594134528, 0, 'global', 'system', '系统用户', 0, 'Approved via API test', '2026-06-09 12:12:54.951', '2026-06-09 12:12:54.951', 'system', 'system');
INSERT INTO `openplatform_v2_approval_log_t` VALUES (322709802016833536, 322709744970104832, 0, 'global', 'system', '系统用户', 1, 'Rejected via API test', '2026-06-09 12:13:32.459', '2026-06-09 12:13:32.459', 'system', 'system');
INSERT INTO `openplatform_v2_approval_log_t` VALUES (322732492270338048, 322732383247794176, 0, 'global', 'admin', 'admin', 0, 'E2E test approve', '2026-06-09 13:43:42.237', '2026-06-09 13:43:42.237', 'admin', 'admin');
INSERT INTO `openplatform_v2_approval_log_t` VALUES (322735723956404224, 322735723352424448, 0, 'global', 'admin', 'admin', 0, 'OK', '2026-06-09 13:56:32.731', '2026-06-09 13:56:32.731', 'admin', 'admin');

SET FOREIGN_KEY_CHECKS = 1;
