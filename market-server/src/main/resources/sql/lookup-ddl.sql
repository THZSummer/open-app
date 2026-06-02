-- LookUp分类表
CREATE TABLE IF NOT EXISTS openplatform_lookup_classify_t (
    classify_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    classify_code VARCHAR(100) NOT NULL COMMENT '分类编码',
    classify_name VARCHAR(100) NOT NULL COMMENT '分类名称',
    path VARCHAR(100) DEFAULT NULL COMMENT '路径，用于层级归类',
    classify_desc VARCHAR(500) DEFAULT NULL COMMENT '分类描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-失效，1-有效',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    last_update_by VARCHAR(64) DEFAULT NULL COMMENT '最后更新人',
    last_update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (classify_id),
    UNIQUE KEY uk_code_path (classify_code, path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='LookUp分类表';

-- LookUp项表
CREATE TABLE IF NOT EXISTS openplatform_lookup_item_t (
    item_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '项ID',
    classify_id BIGINT NOT NULL COMMENT '分类ID',
    item_code VARCHAR(100) NOT NULL COMMENT '项编码',
    item_name VARCHAR(100) NOT NULL COMMENT '项名称',
    item_value VARCHAR(100) NOT NULL COMMENT '项值',
    item_index INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    item_desc VARCHAR(500) DEFAULT NULL COMMENT '项描述',
    item_attr1 VARCHAR(100) DEFAULT NULL COMMENT '扩展属性1',
    item_attr2 VARCHAR(100) DEFAULT NULL COMMENT '扩展属性2',
    item_attr3 VARCHAR(100) DEFAULT NULL COMMENT '扩展属性3',
    item_attr4 VARCHAR(100) DEFAULT NULL COMMENT '扩展属性4',
    item_attr5 VARCHAR(100) DEFAULT NULL COMMENT '扩展属性5',
    item_attr6 VARCHAR(100) DEFAULT NULL COMMENT '扩展属性6',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-失效，1-有效',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    last_update_by VARCHAR(64) DEFAULT NULL COMMENT '最后更新人',
    last_update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (item_id),
    KEY idx_classify_id (classify_id),
    KEY idx_classify_code (classify_id, item_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='LookUp项表';

-- LookUp任务表
CREATE TABLE IF NOT EXISTS openplatform_lookup_task_t (
    task_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    task_type TINYINT NOT NULL COMMENT '任务类型：1-导入，2-导出',
    biz_type TINYINT NOT NULL COMMENT '业务类型：1-LookUp，2-数据字典',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待处理，1-处理中，2-已完成，3-失败',
    file_id VARCHAR(128) DEFAULT NULL COMMENT 'OBS文件ID',
    file_name VARCHAR(255) DEFAULT NULL COMMENT '文件名',
    result VARCHAR(500) DEFAULT NULL COMMENT '结果描述',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    last_update_by VARCHAR(64) DEFAULT NULL COMMENT '最后更新人',
    last_update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (task_id),
    KEY idx_task_type (task_type),
    KEY idx_biz_type (biz_type),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='LookUp任务表';

-- 插入测试分类
INSERT INTO openplatform_lookup_classify_t (classify_code, classify_name, path, classify_desc, status, create_by, create_time, last_update_by, last_update_time) VALUES
('USER_TYPE', '用户类型', '/system', '系统用户类型字典', 1, 'admin', NOW(), 'admin', NOW()),
('GENDER', '性别', '/system', '性别枚举', 1, 'admin', NOW(), 'admin', NOW()),
('ORDER_STATUS', '订单状态', '/business', '业务订单状态', 1, 'admin', NOW(), 'admin', NOW());

-- 插入测试项
INSERT INTO openplatform_lookup_item_t (classify_id, item_code, item_name, item_value, item_index, item_desc, status, create_by, create_time, last_update_by, last_update_time) VALUES
(1, 'ADMIN', '管理员', '1', 1, '系统管理员', 1, 'admin', NOW(), 'admin', NOW()),
(1, 'NORMAL', '普通用户', '2', 2, '普通用户角色', 1, 'admin', NOW(), 'admin', NOW()),
(1, 'GUEST', '访客', '3', 3, '访客角色', 0, 'admin', NOW(), 'admin', NOW()),
(2, 'MALE', '男', '1', 1, '男性', 1, 'admin', NOW(), 'admin', NOW()),
(2, 'FEMALE', '女', '2', 2, '女性', 1, 'admin', NOW(), 'admin', NOW()),
(3, 'PENDING', '待支付', '1', 1, '订单待支付', 1, 'admin', NOW(), 'admin', NOW()),
(3, 'PAID', '已支付', '2', 2, '订单已支付', 1, 'admin', NOW(), 'admin', NOW()),
(3, 'SHIPPED', '已发货', '3', 3, '订单已发货', 1, 'admin', NOW(), 'admin', NOW()),
(3, 'COMPLETED', '已完成', '4', 4, '订单已完成', 1, 'admin', NOW(), 'admin', NOW()),
(3, 'CANCELLED', '已取消', '5', 5, '订单已取消', 0, 'admin', NOW(), 'admin', NOW());
