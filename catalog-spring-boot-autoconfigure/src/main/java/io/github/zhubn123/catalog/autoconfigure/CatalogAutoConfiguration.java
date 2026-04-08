package io.github.zhubn123.catalog.autoconfigure;

import io.github.zhubn123.catalog.mapper.CatalogNodeMapper;
import io.github.zhubn123.catalog.mapper.CatalogRelMapper;
import io.github.zhubn123.catalog.service.CatalogService;
import io.github.zhubn123.catalog.service.CatalogServiceImpl;
import io.github.zhubn123.catalog.service.CatalogSortStrategy;
import io.github.zhubn123.catalog.service.CatalogTreeNodeEnricher;
import io.github.zhubn123.catalog.service.sort.ContiguousCatalogSortStrategy;
import io.github.zhubn123.catalog.service.sort.GapCatalogSortStrategy;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 目录模块自动配置。
 */
@Configuration
@EnableConfigurationProperties(CatalogProperties.class)
@MapperScan("io.github.zhubn123.catalog.mapper")
@ConditionalOnProperty(prefix = "catalog", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CatalogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CatalogSortStrategy catalogSortStrategy(CatalogProperties properties) {
        CatalogProperties.SortProperties sort = properties.getSort() == null
                ? new CatalogProperties.SortProperties()
                : properties.getSort();
        String strategy = sort.getStrategy();
        if (!StringUtils.hasText(strategy) || GapCatalogSortStrategy.NAME.equalsIgnoreCase(strategy.trim())) {
            return new GapCatalogSortStrategy(sort.getGapStep());
        }
        if (ContiguousCatalogSortStrategy.NAME.equalsIgnoreCase(strategy.trim())) {
            return new ContiguousCatalogSortStrategy();
        }
        throw new IllegalArgumentException("Unsupported catalog.sort.strategy: " + strategy);
    }

    @Bean
    @ConditionalOnMissingBean
    public CatalogService catalogService(
            CatalogNodeMapper nodeMapper,
            CatalogRelMapper relMapper,
            List<CatalogTreeNodeEnricher> treeNodeEnrichers,
            CatalogSortStrategy sortStrategy
    ) {
        return new CatalogServiceImpl(nodeMapper, relMapper, treeNodeEnrichers, sortStrategy);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "catalog", name = "enable-rest-api", havingValue = "true", matchIfMissing = true)
    public CatalogController catalogController(CatalogService catalogService) {
        return new CatalogController(catalogService);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "catalog", name = "enable-rest-api", havingValue = "true", matchIfMissing = true)
    public CatalogExceptionHandler catalogExceptionHandler() {
        return new CatalogExceptionHandler();
    }
}
