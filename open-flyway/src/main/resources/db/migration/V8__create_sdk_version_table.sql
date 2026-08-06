-- SDK版本管理表
CREATE TABLE IF NOT EXISTS openplatform_sdk_version_t (
    id                  BIGINT          NOT NULL COMMENT '主键ID',
    version_name        VARCHAR(64)     NOT NULL COMMENT '版本号',
    update_notes        VARCHAR(2000)   NULL     COMMENT '更新说明',
    artifact_url        VARCHAR(512)    NULL     COMMENT '构建产物下载地址',
    permission_id       BIGINT          NULL     COMMENT '关联权限ID(可选)',
    status              TINYINT         NOT NULL DEFAULT 1 COMMENT '1-已发布 2-已废弃',
    deprecate_reason    VARCHAR(2000)   NULL     COMMENT '废弃原因',
    create_by           VARCHAR(64)     NOT NULL,
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_update_by      VARCHAR(64)     NOT NULL,
    last_update_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_version_permission (version_name, permission_id),
    KEY idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SDK版本表';
