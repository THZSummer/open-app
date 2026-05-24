package com.xxx.it.works.wecode.v2.common.config;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import static io.r2dbc.spi.ConnectionFactoryOptions.*;

/**
 * R2DBC 响应式数据库配置
 * <p>
 * 配置 R2DBC 连接工厂, 启用响应式 Repository 支持。
 * connector-api 不维护 DDL 脚本, 不执行数据库迁移,
 * 仅通过 R2DBC 访问已初始化的开放平台共库表。
 * </p>
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "com.xxx.it.works.wecode.v2.modules")
public class R2dbcConfig extends AbstractR2dbcConfiguration {

    @Value("${spring.r2dbc.url}")
    private String r2dbcUrl;

    @Value("${spring.r2dbc.username}")
    private String username;

    @Value("${spring.r2dbc.password}")
    private String password;

    @Override
    @Bean
    public ConnectionFactory connectionFactory() {
        // 从 r2dbc URL 解析连接选项
        // 格式: r2dbc:mysql://host:port/database?params
        String url = r2dbcUrl.replace("r2dbc:mysql://", "");
        String host = url.contains(":") ? url.substring(0, url.indexOf(":")) : url.substring(0, url.indexOf("/"));
        String hostPort = url.substring(0, url.indexOf("/"));
        String database = url.substring(url.indexOf("/") + 1);
        if (database.contains("?")) {
            database = database.substring(0, database.indexOf("?"));
        }
        int port = 3306;
        if (hostPort.contains(":")) {
            port = Integer.parseInt(hostPort.substring(hostPort.indexOf(":") + 1));
        }

        return ConnectionFactories.get(ConnectionFactoryOptions.builder()
                .option(DRIVER, "mysql")
                .option(HOST, host)
                .option(PORT, port)
                .option(USER, username)
                .option(PASSWORD, password)
                .option(DATABASE, database)
                .option(CONNECT_TIMEOUT, java.time.Duration.ofSeconds(10))
                .option(SSL, false)
                .build());
    }
}