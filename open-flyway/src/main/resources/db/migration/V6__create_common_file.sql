-- ============================================================================
-- 通用文件表 Schema 迁移
-- 版本: V5
-- 创建日期: 2026-07-17
-- 设计原则:
--   - 幂等设计: 通过存储过程安全判断，支持重复执行不报错
--   - 每条语句独立: 单条失败不影响其他，业务失败另行处理
--   - 无物理外键
--   - 审计字段: create_time
-- ============================================================================

-- ============================================================================
-- 第 0 部分: 公共存储过程（幂等 DDL 安全执行）
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 0.1 safe_create_table: 表不存在时才创建
--     参数: p_table=表名, p_definition=完整 CREATE TABLE 语句（不含末尾分号）
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS safe_create_table;
DELIMITER $$
CREATE PROCEDURE safe_create_table(
    IN p_table       VARCHAR(128),
    IN p_definition  TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = p_table
    ) THEN
        SET @sql = p_definition;
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- ----------------------------------------------------------------------------
-- 0.2 safe_add_column: 表存在且列不存在时才添加
--     参数: p_table=表名, p_column=列名, p_definition=列定义(类型+约束+COMMENT)
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS safe_add_column;
DELIMITER $$
CREATE PROCEDURE safe_add_column(
    IN p_table       VARCHAR(128),
    IN p_column      VARCHAR(128),
    IN p_definition  TEXT
)
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = p_table
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND column_name = p_column
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;


-- ============================================================================
-- 第 1 部分: 新建表 openplatform_common_file_t
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1.1 创建通用文件表
--     用途：开发环境临时表，记录上传文件的元数据
-- ----------------------------------------------------------------------------
CALL safe_create_table('openplatform_common_file_t',
    'CREATE TABLE `openplatform_common_file_t` (
        `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT ''主键'',
        `batch_id`     varchar(100) NOT NULL COMMENT ''文件批次ID'',
        `file_name`    varchar(500) NOT NULL COMMENT ''原始文件名'',
        `file_path`    varchar(1000) NOT NULL COMMENT ''磁盘路径（开发环境为本地临时目录）'',
        `biz_type`     tinyint      NOT NULL COMMENT ''业务类型：1=能力图标，2=能力示意图'',
        `file_size`    bigint       NOT NULL COMMENT ''文件大小（字节）'',
        `content_type` varchar(100) DEFAULT NULL COMMENT ''MIME类型'',
        `create_time`  datetime(3)  DEFAULT CURRENT_TIMESTAMP(3) COMMENT ''创建时间'',
        PRIMARY KEY (`id`),
        UNIQUE KEY `uk_batch_id` (`batch_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT=''通用文件表（开发环境临时表）'''
);


-- ============================================================================
-- 第 2 部分: 清理存储过程
-- ============================================================================
DROP PROCEDURE IF EXISTS safe_create_table;
DROP PROCEDURE IF EXISTS safe_add_column;

-- ============================================================================
-- 迁移完成标记
-- ============================================================================
-- V5 Schema 迁移完成（幂等版，支持重复执行）
-- 变更汇总:
--   公共存储过程 (2): safe_create_table, safe_add_column
--   CREATE TABLE (1): openplatform_common_file_t
-- ============================================================================
