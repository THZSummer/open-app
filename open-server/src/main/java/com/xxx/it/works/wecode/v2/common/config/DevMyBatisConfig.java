package com.xxx.it.works.wecode.v2.common.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * 开发环境 MyBatis 配置
 *
 * <p>配置 MyBatis SqlSessionFactory
 * <p>Mapper 扫描由 ConnectorMyBatisConfig 统一管理（无 Profile 限制）
 *
 * @author SDDU Build Agent
 */
@Configuration
@Profile({"dev", "development", "local"})
public class DevMyBatisConfig {

    @Bean
    @ConditionalOnBean(DataSource.class)
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);

        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath:mapper/*.xml")
        );

        return factoryBean.getObject();
    }
}
