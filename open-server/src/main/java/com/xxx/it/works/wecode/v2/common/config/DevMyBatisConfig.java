package com.xxx.it.works.wecode.v2.common.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * 开发环境 MyBatis 配置
 *
 * <p>配置 MyBatis SqlSessionFactory 和 Mapper 扫描</p>
 * <p>只有在有 DataSource 时才生效</p>
 *
 * <p>注意：标准环境由基础模块处理，无需此配置</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Configuration
@Profile({"dev", "development", "local"})
@MapperScan("com.xxx.it.works.wecode.v2.modules.*.mapper")
public class DevMyBatisConfig {

    @Bean
    @ConditionalOnBean(DataSource.class)
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);

        // 设置 Mapper XML 文件位置
        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath:mapper/*.xml")
        );

        return factoryBean.getObject();
    }
}
