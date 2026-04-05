package io.github.zhubn123.catalog.autoconfigure;

import io.github.zhubn123.catalog.service.CatalogService;
import io.github.zhubn123.catalog.service.CatalogServiceImpl;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 目录模块自动配置。
 *
 * <p>在满足条件时自动注册目录服务、控制器以及 MyBatis Mapper 扫描。</p>
 *
 * @author zhubn
 * @date 2026/4/2
 */
@Configuration
@EnableConfigurationProperties(CatalogProperties.class)
@MapperScan("io.github.zhubn123.catalog.mapper")
@ConditionalOnProperty(prefix = "catalog", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CatalogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CatalogService catalogService() {
        return new CatalogServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "catalog", name = "enable-rest-api", havingValue = "true", matchIfMissing = true)
    public CatalogController catalogController() {
        return new CatalogController();
    }
}
