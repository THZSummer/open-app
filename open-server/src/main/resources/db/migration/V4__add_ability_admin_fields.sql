-- ============================================================================
-- 嵌入能力平台面 数据库 Schema 迁移
-- 版本: V4
-- 创建日期: 2026-07-17
-- 设计原则:
--   - 幂等设计: 所有 DDL 通过存储过程安全判断，支持重复执行不报错
--   - 每条语句独立: 单条失败不影响其他，业务失败另行处理
--   - 无物理外键
--   - 审计字段: create_time, last_update_time, create_by, last_update_by
-- ============================================================================

-- ============================================================================
-- 第 0 部分: 公共存储过程（幂等 DDL 安全执行）
--   同一类逻辑抽取公共存储过程，通过 information_schema 判断对象是否存在
--   满足条件才执行，不满足则跳过，实现无条件可重复执行
--   所有存储过程均兼容表不存在的场景（表不存在时安全跳过，不报错）
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 0.1 safe_add_column: 表存在且列不存在时才添加
--     参数: p_table=表名, p_column=列名, p_definition=列定义(类型+约束+COMMENT)
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS safe_add_column;
DELIMITER $$
CREATE PROCEDURE safe_add_column(
    IN p_table  VARCHAR(128),
    IN p_column VARCHAR(128),
    IN p_definition TEXT
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

-- ----------------------------------------------------------------------------
-- 0.2 safe_modify_column: 表存在且列存在时才修改
--     参数: p_table=表名, p_column=列名, p_definition=列定义(类型+约束+COMMENT)
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS safe_modify_column;
DELIMITER $$
CREATE PROCEDURE safe_modify_column(
    IN p_table  VARCHAR(128),
    IN p_column VARCHAR(128),
    IN p_definition TEXT
)
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = p_table
    ) AND EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND column_name = p_column
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` MODIFY COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;


-- ============================================================================
-- 第 1 部分: 已有表结构变更（openplatform_ability_t）
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1.1 调整 ability_type: 统一枚举字段列类型为 TINYINT(10)，更新注释以反映自定义类型
--     原: tinyint(1) NOT NULL DEFAULT 0 COMMENT '能力类型 1-群置顶...'
--     改: TINYINT(10) UNSIGNED NOT NULL DEFAULT 0 COMMENT '能力类型编码（1-7 预置...）'
-- ----------------------------------------------------------------------------
CALL safe_modify_column('openplatform_ability_t', 'ability_type',
    'TINYINT(10) UNSIGNED NOT NULL DEFAULT 0 COMMENT ''能力类型编码（1-7 预置，自定义类型统一分配，不区分范围）''');

-- ----------------------------------------------------------------------------
-- 1.2 新增嵌入能力相关 6 字段
--     字段顺序：entry_url → hidden → route_path → alias_name → require_release → load_type
-- ----------------------------------------------------------------------------
CALL safe_add_column('openplatform_ability_t', 'entry_url',
    'varchar(1000) NULL DEFAULT NULL COMMENT ''进入地址（微前端子应用入口）''');

CALL safe_add_column('openplatform_ability_t', 'hidden',
    'TINYINT(10) NOT NULL DEFAULT 1 COMMENT ''是否在开放面展示：0=展示, 1=隐藏''');

CALL safe_add_column('openplatform_ability_t', 'route_path',
    'varchar(255) NULL DEFAULT NULL COMMENT ''路由路径（子应用激活路由）''');

CALL safe_add_column('openplatform_ability_t', 'alias_name',
    'varchar(100) NULL DEFAULT NULL COMMENT ''别名（子应用唯一标识）''');

CALL safe_add_column('openplatform_ability_t', 'require_release',
    'TINYINT(10) NOT NULL DEFAULT 0 COMMENT ''是否需要版本发布才生效：0=即时生效, 1=需版本发布''');

CALL safe_add_column('openplatform_ability_t', 'load_type',
    'TINYINT(10) NOT NULL DEFAULT 1 COMMENT ''加载类型：1=路由加载, 2=微前端加载''');

-- ============================================================================
-- 第 2 部分: 新建表（此版本无新建表）
-- ============================================================================

-- ============================================================================
-- 第 3 部分: 清理存储过程
-- ============================================================================
DROP PROCEDURE IF EXISTS safe_add_column;
DROP PROCEDURE IF EXISTS safe_modify_column;

-- ============================================================================
-- 迁移完成标记
-- ============================================================================
-- 嵌入能力平台面 V4 Schema 迁移完成（幂等版，支持重复执行）
-- 变更汇总:
--   公共存储过程 (2): safe_add_column, safe_modify_column
--   ALTER (1 表 / 7 条): MODIFY ability_type, ADD entry_url, ADD hidden, ADD route_path, ADD alias_name, ADD require_release, ADD load_type
-- ============================================================================
