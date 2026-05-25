package com.xxx.it.works.wecode.v2.common.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 连接器模块 MyBatis Mapper 扫描配置
 *
 * <p>统一管理 V2 模块下所有 MyBatis Mapper 接口的扫描注册。
 * 仅在存在 DataSource 时生效，避免在 @WebMvcTest 等无 DB 上下文中触发。
 *
 * <p>覆盖范围：com.xxx.it.works.wecode.v2.modules.*.mapper
 * （如 connector.mapper、flow.mapper 等）
 *
 * @author SDDU Build Agent
 */
@Configuration
@ConditionalOnBean(DataSource.class)
@MapperScan("com.xxx.it.works.wecode.v2.modules.*.mapper")
public class ConnectorMyBatisConfig {
}
