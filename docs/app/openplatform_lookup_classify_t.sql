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

 Date: 18/06/2026 09:58:39
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for openplatform_lookup_classify_t
-- ----------------------------
DROP TABLE IF EXISTS `openplatform_lookup_classify_t`;
CREATE TABLE `openplatform_lookup_classify_t`  (
  `classify_id` bigint NOT NULL COMMENT '分类ID，主键',
  `classify_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类编码',
  `classify_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `path` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '路径，用于层级归类',
  `classify_desc` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分类描述',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：0=失效, 1=有效',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `last_update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最后更新人',
  `last_update_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  PRIMARY KEY (`classify_id`) USING BTREE,
  UNIQUE INDEX `uk_code_path`(`classify_code` ASC, `path` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 124 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'LookUp分类表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of openplatform_lookup_classify_t
-- ----------------------------
INSERT INTO `openplatform_lookup_classify_t` VALUES (1, 'USER_TYPE', '用户类型', '/system', '系统用户类型字典', 1, 'admin', '2026-05-20 20:28:21.000', 'admin', '2026-05-20 20:28:21.000');
INSERT INTO `openplatform_lookup_classify_t` VALUES (2, 'GENDER', '性别', '/system', '性别枚举', 1, 'admin', '2026-05-20 20:28:25.000', 'admin', '2026-05-20 20:28:25.000');
INSERT INTO `openplatform_lookup_classify_t` VALUES (3, 'ORDER_STATUS', '订单状态', '/business', '业务订单状态', 1, 'admin', '2026-05-20 20:28:25.000', 'admin', '2026-05-20 20:28:25.000');
INSERT INTO `openplatform_lookup_classify_t` VALUES (7, 'CRUD_NEW', 'CRUD新建', '/crud', '测试CRUD', 1, 'system', '2026-05-20 20:37:35.000', 'system', '2026-05-20 20:37:35.000');
INSERT INTO `openplatform_lookup_classify_t` VALUES (9, 'CHINESE_TEST', '中文分类测试', '/chinese', '测试中文字符', 1, 'system', '2026-05-20 20:38:50.000', 'system', '2026-05-20 20:38:50.000');
INSERT INTO `openplatform_lookup_classify_t` VALUES (10, 'TEST123', 'Test123', '', NULL, 1, 'system', '2026-05-20 20:41:53.147', 'system', '2026-05-20 20:41:53.147');
INSERT INTO `openplatform_lookup_classify_t` VALUES (11, 'TEST_NOW', 'TestNow', '', NULL, 1, 'system', '2026-05-20 20:42:03.527', 'system', '2026-05-20 20:42:03.527');
INSERT INTO `openplatform_lookup_classify_t` VALUES (12, 'LOOKUP_TEST', 'LookUp测试', '/lookup', 'LookUp中文测试数据', 1, 'system', '2026-05-20 20:43:50.000', 'system', '2026-05-20 20:43:50.000');
INSERT INTO `openplatform_lookup_classify_t` VALUES (13, 'ces1', 'ces1', '2222', NULL, 1, 'system', '2026-05-21 08:48:19.102', 'system', '2026-05-21 08:48:19.102');
INSERT INTO `openplatform_lookup_classify_t` VALUES (16, 'TEST_CJ_001', '测试新增分类', '', NULL, 1, 'system', '2026-05-21 19:09:28.476', 'system', '2026-05-21 19:09:28.476');
INSERT INTO `openplatform_lookup_classify_t` VALUES (17, 'TEST_CLASSIFY_1779363052', '测试分类', '/test', '测试分类描述', 1, 'system', '2026-05-21 19:30:52.617', 'system', '2026-05-21 19:30:52.617');
INSERT INTO `openplatform_lookup_classify_t` VALUES (18, 'CASCADE_TEST_1779363052', '级联删除测试分类', '/cascade', '用于测试级联删除', 1, 'system', '2026-05-21 19:30:52.654', 'system', '2026-05-21 19:30:52.654');
INSERT INTO `openplatform_lookup_classify_t` VALUES (19, 'ITEM_TEST_CLASSIFY_1779363052', '项测试分类', '/item_test', '用于项测试的分类', 1, 'system', '2026-05-21 19:30:52.664', 'system', '2026-05-21 19:30:52.664');
INSERT INTO `openplatform_lookup_classify_t` VALUES (20, 'IMPORT_EXPORT_TEST_1779363052', '导入导出测试分类', '/import_export', '用于测试导入导出', 1, 'system', '2026-05-21 19:30:52.675', 'system', '2026-05-21 19:30:52.675');
INSERT INTO `openplatform_lookup_classify_t` VALUES (21, 'E2E_TEST_1779363052', '端到端测试分类', '/e2e', '端到端测试', 1, 'system', '2026-05-21 19:30:52.800', 'system', '2026-05-21 19:30:52.800');
INSERT INTO `openplatform_lookup_classify_t` VALUES (22, 'TEST_CLASSIFY_1779363138', '测试分类', '/test', '测试分类描述', 1, 'system', '2026-05-21 19:32:18.729', 'system', '2026-05-21 19:32:18.729');
INSERT INTO `openplatform_lookup_classify_t` VALUES (23, 'CASCADE_TEST_1779363138', '级联删除测试分类', '/cascade', '用于测试级联删除', 1, 'system', '2026-05-21 19:32:18.818', 'system', '2026-05-21 19:32:18.818');
INSERT INTO `openplatform_lookup_classify_t` VALUES (24, 'ITEM_TEST_CLASSIFY_1779363138', '项测试分类', '/item_test', '用于项测试的分类', 1, 'system', '2026-05-21 19:32:18.831', 'system', '2026-05-21 19:32:18.831');
INSERT INTO `openplatform_lookup_classify_t` VALUES (25, 'IMPORT_EXPORT_TEST_1779363138', '导入导出测试分类', '/import_export', '用于测试导入导出', 1, 'system', '2026-05-21 19:32:18.872', 'system', '2026-05-21 19:32:18.872');
INSERT INTO `openplatform_lookup_classify_t` VALUES (26, 'E2E_TEST_1779363139', '端到端测试分类', '/e2e', '端到端测试', 1, 'system', '2026-05-21 19:32:19.541', 'system', '2026-05-21 19:32:19.541');
INSERT INTO `openplatform_lookup_classify_t` VALUES (27, 'TEST_CLASSIFY_1779363177', '测试分类', '/test', '测试分类描述', 1, 'system', '2026-05-21 19:32:57.257', 'system', '2026-05-21 19:32:57.257');
INSERT INTO `openplatform_lookup_classify_t` VALUES (28, 'CASCADE_TEST_1779363177', '级联删除测试分类', '/cascade', '用于测试级联删除', 1, 'system', '2026-05-21 19:32:57.296', 'system', '2026-05-21 19:32:57.296');
INSERT INTO `openplatform_lookup_classify_t` VALUES (29, 'ITEM_TEST_CLASSIFY_1779363177', '项测试分类', '/item_test', '用于项测试的分类', 1, 'system', '2026-05-21 19:32:57.306', 'system', '2026-05-21 19:32:57.306');
INSERT INTO `openplatform_lookup_classify_t` VALUES (30, 'IMPORT_EXPORT_TEST_1779363177', '导入导出测试分类', '/import_export', '用于测试导入导出', 1, 'system', '2026-05-21 19:32:57.324', 'system', '2026-05-21 19:32:57.324');
INSERT INTO `openplatform_lookup_classify_t` VALUES (31, 'E2E_TEST_1779363177', '端到端测试分类', '/e2e', '端到端测试', 1, 'system', '2026-05-21 19:32:57.467', 'system', '2026-05-21 19:32:57.467');
INSERT INTO `openplatform_lookup_classify_t` VALUES (32, 'TEST_CLASSIFY_1779363182', '测试分类', '/test', '测试分类描述', 1, 'system', '2026-05-21 19:33:02.237', 'system', '2026-05-21 19:33:02.237');
INSERT INTO `openplatform_lookup_classify_t` VALUES (33, 'CASCADE_TEST_1779363182', '级联删除测试分类', '/cascade', '用于测试级联删除', 1, 'system', '2026-05-21 19:33:02.306', 'system', '2026-05-21 19:33:02.306');
INSERT INTO `openplatform_lookup_classify_t` VALUES (34, 'ITEM_TEST_CLASSIFY_1779363182', '项测试分类', '/item_test', '用于项测试的分类', 1, 'system', '2026-05-21 19:33:02.317', 'system', '2026-05-21 19:33:02.317');
INSERT INTO `openplatform_lookup_classify_t` VALUES (35, 'IMPORT_EXPORT_TEST_1779363182', '导入导出测试分类', '/import_export', '用于测试导入导出', 1, 'system', '2026-05-21 19:33:02.333', 'system', '2026-05-21 19:33:02.333');
INSERT INTO `openplatform_lookup_classify_t` VALUES (36, 'E2E_TEST_1779363182', '端到端测试分类', '/e2e', '端到端测试', 1, 'system', '2026-05-21 19:33:02.475', 'system', '2026-05-21 19:33:02.475');
INSERT INTO `openplatform_lookup_classify_t` VALUES (37, 'TEST_CLASSIFY_1779363219', '测试分类', '/test', '测试分类描述', 1, 'system', '2026-05-21 19:33:39.873', 'system', '2026-05-21 19:33:39.873');
INSERT INTO `openplatform_lookup_classify_t` VALUES (38, 'CASCADE_TEST_1779363219', '级联删除测试分类', '/cascade', '用于测试级联删除', 1, 'system', '2026-05-21 19:33:39.942', 'system', '2026-05-21 19:33:39.942');
INSERT INTO `openplatform_lookup_classify_t` VALUES (39, 'ITEM_TEST_CLASSIFY_1779363219', '项测试分类', '/item_test', '用于项测试的分类', 1, 'system', '2026-05-21 19:33:39.950', 'system', '2026-05-21 19:33:39.950');
INSERT INTO `openplatform_lookup_classify_t` VALUES (40, 'IMPORT_EXPORT_TEST_1779363219', '导入导出测试分类', '/import_export', '用于测试导入导出', 1, 'system', '2026-05-21 19:33:39.964', 'system', '2026-05-21 19:33:39.964');
INSERT INTO `openplatform_lookup_classify_t` VALUES (41, 'TEST_CLASSIFY_1779363224', '测试分类', '/test', '测试分类描述', 1, 'system', '2026-05-21 19:33:44.713', 'system', '2026-05-21 19:33:44.713');
INSERT INTO `openplatform_lookup_classify_t` VALUES (42, 'CASCADE_TEST_1779363224', '级联删除测试分类', '/cascade', '用于测试级联删除', 1, 'system', '2026-05-21 19:33:44.747', 'system', '2026-05-21 19:33:44.747');
INSERT INTO `openplatform_lookup_classify_t` VALUES (43, 'ITEM_TEST_CLASSIFY_1779363224', '项测试分类', '/item_test', '用于项测试的分类', 1, 'system', '2026-05-21 19:33:44.755', 'system', '2026-05-21 19:33:44.755');
INSERT INTO `openplatform_lookup_classify_t` VALUES (44, 'IMPORT_EXPORT_TEST_1779363224', '导入导出测试分类', '/import_export', '用于测试导入导出', 1, 'system', '2026-05-21 19:33:44.768', 'system', '2026-05-21 19:33:44.768');
INSERT INTO `openplatform_lookup_classify_t` VALUES (45, 'E2E_TEST_1779363224', '端到端测试分类', '/e2e', '端到端测试', 1, 'system', '2026-05-21 19:33:44.897', 'system', '2026-05-21 19:33:44.897');
INSERT INTO `openplatform_lookup_classify_t` VALUES (46, 'TEST_CLASSIFY_1779363244', '测试分类', '/test', '测试分类描述', 1, 'system', '2026-05-21 19:34:04.494', 'system', '2026-05-21 19:34:04.494');
INSERT INTO `openplatform_lookup_classify_t` VALUES (47, 'CASCADE_TEST_1779363244', '级联删除测试分类', '/cascade', '用于测试级联删除', 1, 'system', '2026-05-21 19:34:04.528', 'system', '2026-05-21 19:34:04.528');
INSERT INTO `openplatform_lookup_classify_t` VALUES (48, 'ITEM_TEST_CLASSIFY_1779363244', '项测试分类', '/item_test', '用于项测试的分类', 1, 'system', '2026-05-21 19:34:04.536', 'system', '2026-05-21 19:34:04.536');
INSERT INTO `openplatform_lookup_classify_t` VALUES (49, 'IMPORT_EXPORT_TEST_1779363244', '导入导出测试分类', '/import_export', '用于测试导入导出', 1, 'system', '2026-05-21 19:34:04.552', 'system', '2026-05-21 19:34:04.552');
INSERT INTO `openplatform_lookup_classify_t` VALUES (50, 'E2E_TEST_1779363244', '端到端测试分类', '/e2e', '端到端测试', 1, 'system', '2026-05-21 19:34:04.693', 'system', '2026-05-21 19:34:04.693');
INSERT INTO `openplatform_lookup_classify_t` VALUES (52, 'CASCADE_TEST_1779363364', '级联删除测试分类', '/cascade', '用于测试级联删除', 1, 'system', '2026-05-21 19:36:04.414', 'system', '2026-05-21 19:36:04.414');
INSERT INTO `openplatform_lookup_classify_t` VALUES (53, 'ITEM_TEST_CLASSIFY_1779363364', '项测试分类', '/item_test', '用于项测试的分类', 1, 'system', '2026-05-21 19:36:04.424', 'system', '2026-05-21 19:36:04.424');
INSERT INTO `openplatform_lookup_classify_t` VALUES (54, 'IMPORT_EXPORT_TEST_1779363364', '导入导出测试分类', '/import_export', '用于测试导入导出', 1, 'system', '2026-05-21 19:36:04.445', 'system', '2026-05-21 19:36:04.445');
INSERT INTO `openplatform_lookup_classify_t` VALUES (55, 'E2E_TEST_1779363364', '端到端测试分类', '/e2e', '端到端测试', 1, 'system', '2026-05-21 19:36:04.629', 'system', '2026-05-21 19:36:04.629');
INSERT INTO `openplatform_lookup_classify_t` VALUES (57, 'CASCADE_TEST_1779363382', '级联删除测试分类', '/cascade', '用于测试级联删除', 1, 'system', '2026-05-21 19:36:22.350', 'system', '2026-05-21 19:36:22.350');
INSERT INTO `openplatform_lookup_classify_t` VALUES (58, 'ITEM_TEST_CLASSIFY_1779363382', '项测试分类', '/item_test', '用于项测试的分类', 1, 'system', '2026-05-21 19:36:22.359', 'system', '2026-05-21 19:36:22.359');
INSERT INTO `openplatform_lookup_classify_t` VALUES (59, 'IMPORT_EXPORT_TEST_1779363382', '导入导出测试分类', '/import_export', '用于测试导入导出', 1, 'system', '2026-05-21 19:36:22.375', 'system', '2026-05-21 19:36:22.375');
INSERT INTO `openplatform_lookup_classify_t` VALUES (60, 'E2E_TEST_1779363382', '端到端测试分类', '/e2e', '端到端测试', 1, 'system', '2026-05-21 19:36:22.525', 'system', '2026-05-21 19:36:22.525');
INSERT INTO `openplatform_lookup_classify_t` VALUES (61, 'TEST_CLASSIFY_1779363482', '更新后的名称', '/updated_path', '更新后的描述', 1, 'system', '2026-05-21 19:38:02.605', 'system', '2026-05-21 19:38:02.637');
INSERT INTO `openplatform_lookup_classify_t` VALUES (62, 'TEST_CLASSIFY_1779363518', '测试分类', '/test', '测试分类描述', 1, 'system', '2026-05-21 19:38:38.256', 'system', '2026-05-21 19:38:38.256');
INSERT INTO `openplatform_lookup_classify_t` VALUES (63, 'TEST_CLASSIFY_1779363542', '更新后的名称', '/updated_path', '更新后的描述', 1, 'system', '2026-05-21 19:39:02.833', 'system', '2026-05-21 19:39:02.864');
INSERT INTO `openplatform_lookup_classify_t` VALUES (65, 'CASCADE_TEST_1779363626', '级联删除测试分类', '/cascade', '用于测试级联删除', 1, 'system', '2026-05-21 19:40:26.305', 'system', '2026-05-21 19:40:26.305');
INSERT INTO `openplatform_lookup_classify_t` VALUES (66, 'ITEM_TEST_CLASSIFY_1779363626', '项测试分类', '/item_test', '用于项测试的分类', 1, 'system', '2026-05-21 19:40:26.312', 'system', '2026-05-21 19:40:26.312');
INSERT INTO `openplatform_lookup_classify_t` VALUES (67, 'IMPORT_EXPORT_TEST_1779363626', '导入导出测试分类', '/import_export', '用于测试导入导出', 1, 'system', '2026-05-21 19:40:26.325', 'system', '2026-05-21 19:40:26.325');
INSERT INTO `openplatform_lookup_classify_t` VALUES (68, 'E2E_TEST_1779363626', '端到端测试分类', '/e2e', '端到端测试', 1, 'system', '2026-05-21 19:40:26.453', 'system', '2026-05-21 19:40:26.453');
INSERT INTO `openplatform_lookup_classify_t` VALUES (69, 'TEST_CLASSIFY_1779363678', '测试分类', NULL, NULL, 0, 'system', '2026-05-21 19:41:18.454', 'system', '2026-05-21 19:41:18.536');
INSERT INTO `openplatform_lookup_classify_t` VALUES (71, 'CASCADE_TEST_1779363693', '级联删除测试分类', '/cascade', '用于测试级联删除', 1, 'system', '2026-05-21 19:41:33.567', 'system', '2026-05-21 19:41:33.567');
INSERT INTO `openplatform_lookup_classify_t` VALUES (72, 'ITEM_TEST_CLASSIFY_1779363693', '项测试分类', '/item_test', '用于项测试的分类', 1, 'system', '2026-05-21 19:41:33.576', 'system', '2026-05-21 19:41:33.576');
INSERT INTO `openplatform_lookup_classify_t` VALUES (73, 'IMPORT_EXPORT_TEST_1779363693', '导入导出测试分类', '/import_export', '用于测试导入导出', 1, 'system', '2026-05-21 19:41:33.591', 'system', '2026-05-21 19:41:33.591');
INSERT INTO `openplatform_lookup_classify_t` VALUES (74, 'E2E_TEST_1779363693', '端到端测试分类', '/e2e', '端到端测试', 1, 'system', '2026-05-21 19:41:33.749', 'system', '2026-05-21 19:41:33.749');
INSERT INTO `openplatform_lookup_classify_t` VALUES (76, 'CASCADE_TEST_1779365247', '级联删除测试分类', '/cascade', '用于测试级联删除', 1, 'system', '2026-05-21 20:07:27.878', 'system', '2026-05-21 20:07:27.878');
INSERT INTO `openplatform_lookup_classify_t` VALUES (77, 'E2E_TEST_1779365248', '端到端测试分类', '/e2e', '端到端测试', 1, 'system', '2026-05-21 20:07:28.689', 'system', '2026-05-21 20:07:28.689');
INSERT INTO `openplatform_lookup_classify_t` VALUES (78, 'TEST_API_CLASSIFY', '测试分类', NULL, NULL, 1, 'system', '2026-05-21 20:08:30.265', 'system', '2026-05-21 20:10:07.376');
INSERT INTO `openplatform_lookup_classify_t` VALUES (79, 'CASCADE_TEST_1779365310', '级联删除测试分类', '/cascade', '用于测试级联删除', 1, 'system', '2026-05-21 20:08:30.332', 'system', '2026-05-21 20:08:30.332');
INSERT INTO `openplatform_lookup_classify_t` VALUES (80, 'E2E_TEST_1779365311', '端到端测试分类', '/e2e', '端到端测试', 1, 'system', '2026-05-21 20:08:31.147', 'system', '2026-05-21 20:08:31.147');
INSERT INTO `openplatform_lookup_classify_t` VALUES (81, 'CASCADE_TEST_1779365374', '级联删除测试分类', '/cascade', '用于测试级联删除', 1, 'system', '2026-05-21 20:09:34.234', 'system', '2026-05-21 20:09:34.234');
INSERT INTO `openplatform_lookup_classify_t` VALUES (82, 'E2E_TEST_1779365374', '端到端测试分类', '/e2e', '端到端测试', 1, 'system', '2026-05-21 20:09:34.952', 'system', '2026-05-21 20:09:34.952');
INSERT INTO `openplatform_lookup_classify_t` VALUES (83, 'TEST_API_CLASSIFY', '测试分类', NULL, NULL, 1, 'system', '2026-05-21 20:10:07.275', 'system', '2026-05-21 21:38:07.292');
INSERT INTO `openplatform_lookup_classify_t` VALUES (86, 'TEST_API_001', 'cesAPI', 'test', 'ces', 1, 'system', '2026-05-21 20:36:58.517', 'system', '2026-05-21 20:36:58.517');
INSERT INTO `openplatform_lookup_classify_t` VALUES (87, 'ces2', 'ces1', '2222', 'www', 1, 'system', '2026-05-21 20:38:22.756', 'system', '2026-05-21 20:38:22.756');
INSERT INTO `openplatform_lookup_classify_t` VALUES (88, 'TEST_NEW_001', '测试新增', 'test/path', '测试描述', 1, 'system', '2026-05-21 20:39:28.368', 'system', '2026-05-21 20:39:28.368');
INSERT INTO `openplatform_lookup_classify_t` VALUES (89, 'E2E_TEST_1779370605', '端到端测试分类', '/e2e', '端到端测试', 1, 'system', '2026-05-21 21:36:46.188', 'system', '2026-05-21 21:36:46.188');
INSERT INTO `openplatform_lookup_classify_t` VALUES (90, 'CASCADE_TEST_1779370677', '级联删除测试分类', '/cascade', '用于测试级联删除', 1, 'system', '2026-05-21 21:37:57.188', 'system', '2026-05-21 21:37:57.188');
INSERT INTO `openplatform_lookup_classify_t` VALUES (91, 'TEST_API_CLASSIFY', '测试分类', NULL, NULL, 1, 'system', '2026-05-21 21:38:07.169', 'system', '2026-05-21 21:40:52.879');
INSERT INTO `openplatform_lookup_classify_t` VALUES (92, 'CASCADE_TEST_1779370687', '级联删除测试分类', '/cascade', '用于测试级联删除', 1, 'system', '2026-05-21 21:38:07.303', 'system', '2026-05-21 21:38:07.303');
INSERT INTO `openplatform_lookup_classify_t` VALUES (93, 'E2E_TEST_1779370688', '端到端测试分类', '/e2e', '端到端测试', 1, 'system', '2026-05-21 21:38:08.080', 'system', '2026-05-21 21:38:08.080');
INSERT INTO `openplatform_lookup_classify_t` VALUES (94, 'CASCADE_TEST_1779370801', '级联删除测试分类', '/cascade', '用于测试级联删除', 1, 'system', '2026-05-21 21:40:01.956', 'system', '2026-05-21 21:40:01.956');
INSERT INTO `openplatform_lookup_classify_t` VALUES (95, 'E2E_TEST_1779370803', '端到端测试分类', '/e2e', '端到端测试', 1, 'system', '2026-05-21 21:40:03.692', 'system', '2026-05-21 21:40:03.692');
INSERT INTO `openplatform_lookup_classify_t` VALUES (96, 'TEST_API_CLASSIFY', '测试分类', NULL, NULL, 1, 'system', '2026-05-21 21:40:52.752', 'system', '2026-05-22 08:48:54.384');
INSERT INTO `openplatform_lookup_classify_t` VALUES (97, 'CASCADE_TEST_1779370852', '级联删除测试分类', '/cascade', '用于测试级联删除', 1, 'system', '2026-05-21 21:40:52.893', 'system', '2026-05-21 21:40:52.893');
INSERT INTO `openplatform_lookup_classify_t` VALUES (98, 'E2E_TEST_1779370855', '端到端测试分类', '/e2e', '端到端测试', 1, 'system', '2026-05-21 21:40:55.148', 'system', '2026-05-21 21:40:55.148');
INSERT INTO `openplatform_lookup_classify_t` VALUES (99, 'CASCADE_TEST_1779370862', '级联删除测试分类', '/cascade', '用于测试级联删除', 1, 'system', '2026-05-21 21:41:02.416', 'system', '2026-05-21 21:41:02.416');
INSERT INTO `openplatform_lookup_classify_t` VALUES (100, 'E2E_TEST_1779370864', '端到端测试分类', '/e2e', '端到端测试', 1, 'system', '2026-05-21 21:41:04.214', 'system', '2026-05-21 21:41:04.214');
INSERT INTO `openplatform_lookup_classify_t` VALUES (101, 'TEST_API_CLASSIFY', '测试分类', NULL, NULL, 1, 'system', '2026-05-22 08:48:54.271', 'system', '2026-05-22 08:55:06.882');
INSERT INTO `openplatform_lookup_classify_t` VALUES (102, 'CASCADE_TEST_1779410934', '级联删除测试分类', '/cascade', '用于测试级联删除', 1, 'system', '2026-05-22 08:48:54.395', 'system', '2026-05-22 08:48:54.395');
INSERT INTO `openplatform_lookup_classify_t` VALUES (103, 'E2E_TEST_1779410934', '端到端测试分类', '/e2e', '端到端测试', 1, 'system', '2026-05-22 08:48:54.406', 'system', '2026-05-22 08:48:54.406');
INSERT INTO `openplatform_lookup_classify_t` VALUES (104, 'CASCADE_TEST_1779411291', '级联删除测试分类', '/cascade', '用于测试级联删除', 1, 'system', '2026-05-22 08:54:51.991', 'system', '2026-05-22 08:54:51.991');
INSERT INTO `openplatform_lookup_classify_t` VALUES (105, 'E2E_TEST_1779411292', '端到端测试分类', '/e2e', '端到端测试', 1, 'system', '2026-05-22 08:54:52.012', 'system', '2026-05-22 08:54:52.012');
INSERT INTO `openplatform_lookup_classify_t` VALUES (106, 'TEST_API_CLASSIFY', '测试分类', NULL, NULL, 1, 'system', '2026-05-22 08:55:06.750', 'system', '2026-05-22 09:07:15.197');
INSERT INTO `openplatform_lookup_classify_t` VALUES (107, 'CASCADE_TEST_1779411306', '级联删除测试分类', '/cascade', '用于测试级联删除', 1, 'system', '2026-05-22 08:55:06.898', 'system', '2026-05-22 08:55:06.898');
INSERT INTO `openplatform_lookup_classify_t` VALUES (108, 'E2E_TEST_1779411306', '端到端测试分类', '/e2e', '端到端测试', 1, 'system', '2026-05-22 08:55:06.914', 'system', '2026-05-22 08:55:06.914');
INSERT INTO `openplatform_lookup_classify_t` VALUES (109, 'CASCADE_TEST_1779411714', '级联删除测试分类', '/cascade', '用于测试级联删除', 1, 'system', '2026-05-22 09:01:54.223', 'system', '2026-05-22 09:01:54.223');
INSERT INTO `openplatform_lookup_classify_t` VALUES (110, 'E2E_TEST_1779411714', '端到端测试分类', '/e2e', '端到端测试', 1, 'system', '2026-05-22 09:01:54.234', 'system', '2026-05-22 09:01:54.234');
INSERT INTO `openplatform_lookup_classify_t` VALUES (111, 'TEST_API_CLASSIFY', '测试分类', NULL, NULL, 1, 'system', '2026-05-22 09:07:15.061', 'system', '2026-05-22 09:21:26.253');
INSERT INTO `openplatform_lookup_classify_t` VALUES (112, 'CASCADE_TEST_1779412035', '级联删除测试分类', '/cascade', '用于测试级联删除', 1, 'system', '2026-05-22 09:07:15.209', 'system', '2026-05-22 09:07:15.209');
INSERT INTO `openplatform_lookup_classify_t` VALUES (113, 'E2E_TEST_1779412035', '端到端测试分类', '/e2e', '端到端测试', 1, 'system', '2026-05-22 09:07:15.221', 'system', '2026-05-22 09:07:15.221');
INSERT INTO `openplatform_lookup_classify_t` VALUES (114, 'CASCADE_TEST_1779412403', '级联删除测试分类', '/cascade', '用于测试级联删除', 1, 'system', '2026-05-22 09:13:23.401', 'system', '2026-05-22 09:13:23.401');
INSERT INTO `openplatform_lookup_classify_t` VALUES (115, 'E2E_TEST_1779412403', '端到端测试分类', '/e2e', '端到端测试', 1, 'system', '2026-05-22 09:13:23.413', 'system', '2026-05-22 09:13:23.413');
INSERT INTO `openplatform_lookup_classify_t` VALUES (116, 'TEST_API_CLASSIFY', '测试分类', NULL, NULL, 1, 'system', '2026-05-22 09:21:26.129', 'system', '2026-05-22 09:31:06.916');
INSERT INTO `openplatform_lookup_classify_t` VALUES (117, 'CASCADE_TEST_1779412886', '级联删除测试分类', '/cascade', '用于测试级联删除', 1, 'system', '2026-05-22 09:21:26.265', 'system', '2026-05-22 09:21:26.265');
INSERT INTO `openplatform_lookup_classify_t` VALUES (118, 'E2E_TEST_1779412886', '端到端测试分类', '/e2e', '端到端测试', 1, 'system', '2026-05-22 09:21:26.276', 'system', '2026-05-22 09:21:26.276');
INSERT INTO `openplatform_lookup_classify_t` VALUES (119, 'CASCADE_TEST_1779413143', '级联删除测试分类', '/cascade', '用于测试级联删除', 1, 'system', '2026-05-22 09:25:43.649', 'system', '2026-05-22 09:25:43.649');
INSERT INTO `openplatform_lookup_classify_t` VALUES (120, 'E2E_TEST_1779413143', '端到端测试分类', '/e2e', '端到端测试', 1, 'system', '2026-05-22 09:25:43.662', 'system', '2026-05-22 09:25:43.662');
INSERT INTO `openplatform_lookup_classify_t` VALUES (121, 'TEST_API_CLASSIFY', '测试分类', '/test', '测试分类描述', 1, 'system', '2026-05-22 09:31:06.765', 'system', '2026-05-22 09:31:06.765');
INSERT INTO `openplatform_lookup_classify_t` VALUES (122, 'CASCADE_TEST_1779413466', '级联删除测试分类', '/cascade', '用于测试级联删除', 1, 'system', '2026-05-22 09:31:06.927', 'system', '2026-05-22 09:31:06.927');
INSERT INTO `openplatform_lookup_classify_t` VALUES (123, 'E2E_TEST_1779413466', '端到端测试分类', '/e2e', '端到端测试', 1, 'system', '2026-05-22 09:31:06.938', 'system', '2026-05-22 09:31:06.938');
INSERT INTO `openplatform_lookup_classify_t` VALUES (999999999, 'APP_UI_WHITELIST', '应用管理UI灰度白名单', '/APP_UI_WHITELIST', '应用管理新旧UI人员白名单（不分业务模块）', 1, 'system', '2026-06-10 16:35:17.226', 'system', '2026-06-10 16:35:17.226');
INSERT INTO `openplatform_lookup_classify_t` VALUES (88950135402319872, '1', '1', '1', '1', 1, '1', '2026-06-01 12:09:58.426', '1', '2026-06-01 12:09:58.426');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316245453106577408, 'IMPORT_TEST', 'Import Test', '/import/test', 'Testing import', 1, 'system', '2026-05-22 16:06:31.577', 'system', '2026-05-22 16:06:31.577');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316559638403219456, 'TEST_C001', '测试分类1', '/test', '测试描述1', 1, 'admin', '2026-05-23 12:54:59.190', 'admin', '2026-05-23 12:54:59.190');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316560489020653568, 'TEST001', '测试分类', '/test', '测试描述', 1, 'admin', '2026-05-23 12:58:21.992', 'admin', '2026-05-23 12:58:21.992');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316561300488454144, '001', 'Test Item 1', '100', NULL, 1, 'admin', '2026-05-23 13:01:35.461', 'admin', '2026-05-23 13:01:35.461');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316561300492648448, '002', 'Test Item 2', '200', NULL, 1, 'admin', '2026-05-23 13:01:35.462', 'admin', '2026-05-23 13:01:35.462');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316562897696194560, 'USER_TYPEw', '用户类型', '/system', '用户类型分类', 1, 'system', '2026-05-23 13:07:56.265', 'system', '2026-05-23 13:07:56.265');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316565651437125632, 'Product A', 'A001', '100', NULL, 1, 'admin', '2026-05-23 13:18:52.808', 'admin', '2026-05-23 13:18:52.808');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316565651445514240, 'Product B', 'A002', '200', NULL, 1, 'admin', '2026-05-23 13:18:52.810', 'admin', '2026-05-23 13:18:52.810');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316636701692788736, 'Zhang San', '28', 'Beijing', 'Sales', 1, 'admin', '2026-05-23 18:01:12.509', 'admin', '2026-05-23 18:01:12.509');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316636701701177344, 'Li Si', '35', 'Shanghai', 'Marketing', 1, 'admin', '2026-05-23 18:01:12.511', 'admin', '2026-05-23 18:01:12.511');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316636701705371648, 'Wang Wu', '42', 'Guangzhou', 'IT', 1, 'admin', '2026-05-23 18:01:12.512', 'admin', '2026-05-23 18:01:12.512');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316636701709565952, 'Zhao Liu', '31', 'Shenzhen', 'HR', 1, 'admin', '2026-05-23 18:01:12.513', 'admin', '2026-05-23 18:01:12.513');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316655716863574016, 'TEST001', 'Test Item', 'val1', 'desc1', 1, 'system', '2026-05-23 19:16:46.079', 'system', '2026-05-23 19:16:46.079');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316656851322142720, 'FINAL001', 'Final Test', 'val', NULL, 1, 'system', '2026-05-23 19:21:16.555', 'system', '2026-05-23 19:21:16.555');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316659090279366656, 'TEST01', 'Test', 'val', NULL, 1, 'system', '2026-05-23 19:30:10.364', 'system', '2026-05-23 19:30:10.364');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316660311165763584, 'TEST001', 'Test Item', 'test_val', NULL, 1, 'system', '2026-05-23 19:35:01.446', 'system', '2026-05-23 19:35:01.446');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316662369264599040, 'NEW001', 'New Test Classify', '/test/path', 'Test description', 1, 'system', '2026-05-23 19:43:12.135', 'system', '2026-05-23 19:43:12.135');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316667613180592128, '001', '测试分类', '测试描述', '1', 1, 'system', '2026-05-23 20:04:02.382', 'system', '2026-05-23 20:04:02.382');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316673203474792448, 'NEW001', 'New Classify', '/new', 'New one - should insert', 1, 'system', '2026-05-23 20:26:15.212', 'system', '2026-05-23 20:26:15.212');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316674012308570112, 'NEW_UNIQUE_TEST', 'New Unique Test', '/unique', 'Should insert', 1, 'system', '2026-05-23 20:29:28.053', 'system', '2026-05-23 20:29:28.053');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316674268534407168, 'ANOTHER_NEW_TEST', 'Another New', '/another', 'Should insert', 1, 'system', '2026-05-23 20:30:29.142', 'system', '2026-05-23 20:30:29.142');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316676951450320896, 'COUNT_TEST_A', 'Count A', '/cA', 'Test A', 1, 'system', '2026-05-23 20:41:08.799', 'system', '2026-05-23 20:41:08.799');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316676951462903808, 'COUNT_TEST_B', 'Count B', '/cB', 'Test B', 1, 'system', '2026-05-23 20:41:08.802', 'system', '2026-05-23 20:41:08.802');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316682315910610944, 'NAME_TEST_001', 'Name Test', '/nametest', 'Test', 1, 'system', '2026-05-23 21:02:27.786', 'system', '2026-05-23 21:02:27.786');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316687039812599808, 'TEST_C001', 'Test Classify 1', '/tc1', 'Test', 1, 'system', '2026-05-23 21:21:14.052', 'system', '2026-05-23 21:21:14.052');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316693458695749632, 'TST001', 'Test', '/tst', NULL, 1, 'system', '2026-05-23 21:46:44.434', 'system', '2026-05-23 21:46:44.434');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316695594108190720, 'C001', 'N001', '/p1', NULL, 1, 'system', '2026-05-23 21:55:13.555', 'system', '2026-05-23 21:55:13.555');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316700375849631744, 'NEW_C99', 'New Classify 99', '/new99', 'test desc', 1, 'system', '2026-05-23 22:14:13.611', 'system', '2026-05-23 22:14:13.611');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316701856652853248, 'UNIQUE_2026', 'Unique Test', '/unique2026', 'desc', 1, 'system', '2026-05-23 22:20:06.662', 'system', '2026-05-23 22:20:06.662');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316706006274932736, 'R_C001', 'Result Classify', '/rc', 'desc c', 1, 'system', '2026-05-23 22:36:36.009', 'system', '2026-05-23 22:36:36.009');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316707815123058688, 'T_C001', 'Test C', '/tc', 'desc c', 1, 'system', '2026-05-23 22:43:47.272', 'system', '2026-05-23 22:43:47.272');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316709189357404160, 'NEW_C999', 'New C', '/nc999', 'desc', 1, 'system', '2026-05-23 22:49:14.916', 'system', '2026-05-23 22:49:14.916');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316709944688640000, 'USER_TYPEdes', '用户类型', '/system', '用户类型分类', 1, 'system', '2026-05-23 22:52:15.000', 'system', '2026-05-23 22:52:15.000');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316717369349111808, 'L_A1', 'Lookup A1', '/la1', 'desc', 1, 'system', '2026-05-23 23:21:45.177', 'system', '2026-05-23 23:21:45.177');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316717369365889024, 'L_B0', 'Lookup B0', '/lb0', 'desc', 0, 'system', '2026-05-23 23:21:45.181', 'system', '2026-05-23 23:21:45.181');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316960405916221440, 'djw', 'djw', 'djw', 'w', 1, 'system', '2026-05-24 15:27:29.610', 'system', '2026-05-24 15:27:29.610');
INSERT INTO `openplatform_lookup_classify_t` VALUES (316997256618180608, 'abc', '用户类型', '/system', '用户类型分类', 1, 'system', '2026-05-24 17:53:55.502', 'system', '2026-05-24 17:53:55.502');
INSERT INTO `openplatform_lookup_classify_t` VALUES (317255870368972800, 'Test Item 1', 'TI001', 'Test Description 1', NULL, 1, 'system', '2026-05-25 11:01:33.825', 'system', '2026-05-25 11:01:33.825');
INSERT INTO `openplatform_lookup_classify_t` VALUES (317404131272687616, 'wdqada', 'dasdd', 'dasd', NULL, 1, 'system', '2026-05-25 20:50:41.979', 'system', '2026-05-26 11:18:15.812');
INSERT INTO `openplatform_lookup_classify_t` VALUES (324680000000000001, 'app.default.icon', '应用默认图标', 'CEC.Open', NULL, 1, 'system', '2026-06-15 16:04:55.628', 'system', '2026-06-15 16:04:55.628');

SET FOREIGN_KEY_CHECKS = 1;
