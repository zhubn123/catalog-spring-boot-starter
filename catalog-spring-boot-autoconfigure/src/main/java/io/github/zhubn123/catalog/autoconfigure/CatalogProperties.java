package io.github.zhubn123.catalog.autoconfigure;

import io.github.zhubn123.catalog.service.GapCatalogSortStrategy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 目录模块配置属性。
 */
@Data
@ConfigurationProperties(prefix = "catalog")
public class CatalogProperties {

    /**
     * 是否启用 REST API。
     */
    private boolean enableRestApi = true;

    /**
     * 排序策略配置。
     */
    private SortProperties sort = new SortProperties();

    @Data
    public static class SortProperties {

        /**
         * 当前内置策略名称。
         */
        private String strategy = GapCatalogSortStrategy.NAME;

        /**
         * gap 排序步长。
         */
        private int gapStep = GapCatalogSortStrategy.DEFAULT_STEP;
    }
}