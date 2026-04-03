package com.berlin.catalog.autoconfigure;

import com.berlin.catalog.service.CatalogService;
import com.berlin.catalog.service.CatalogServiceImpl;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 目录模块自动配置类
 * 
 * <p>Spring Boot 自动配置，在应用启动时自动注册所需Bean。</p>
 * 
 * <h3>自动配置内容：</h3>
 * <ul>
 *   <li>CatalogService - 目录服务</li>
 *   <li>CatalogController - REST API（可选）</li>
 *   <li>Mapper扫描 - com.berlin.catalog.mapper</li>
 * </ul>
 * 
 * <h3>启用条件：</h3>
 * <p>配置 catalog.enabled=true（默认启用）</p>
 * 
 * @author zhubn
 * @date 2026/4/2
 */
@Configuration
@EnableConfigurationProperties(CatalogProperties.class)
@MapperScan("com.berlin.catalog.mapper")
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
